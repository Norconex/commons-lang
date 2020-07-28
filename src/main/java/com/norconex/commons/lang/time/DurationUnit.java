/* Copyright 2018 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.time;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Duration Unit.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
//TODO document that units are always worth the same values, and never estimated
// (unlike Duration)
public enum DurationUnit {

    MILLISECOND(1, ChronoUnit.MILLIS),
    SECOND(1000, ChronoUnit.SECONDS),
    MINUTE(60 * SECOND.ms, ChronoUnit.MINUTES),
    HOUR(60 * MINUTE.ms, ChronoUnit.HOURS),
    DAY(24 * HOUR.ms, ChronoUnit.DAYS),
    WEEK(7 * DAY.ms, ChronoUnit.WEEKS),
    MONTH(2629746000L, ChronoUnit.MONTHS), // 365.2425 days / 12
    YEAR(12 * MONTH.ms, ChronoUnit.YEARS);

    private static final DurationUnit[] REVERSE_VALUES = 
            new DurationUnit[values().length];
    static {
        int idx = 0;
        for (int i = values().length - 1; i >= 0; i--) {
            REVERSE_VALUES[idx++] = values()[i];
        }
    }
    
    private final long ms;
    private final ChronoUnit chronoUnit;

    DurationUnit(long ms, ChronoUnit unit) {
        this.ms = ms;
        this.chronoUnit = unit;
    }
    
    public ChronoUnit toTemporalUnit() {
        return chronoUnit;
    }
    public Duration toDuration() {
        return Duration.ofMillis(ms);
    }
    public long toMilliseconds() {
        return toMilliseconds(1);
    }
    public long toMilliseconds(long amount) {
        return toUnit(DurationUnit.MILLISECOND, amount);
    }
    public long toSeconds() {
        return toSeconds(1);
    }
    public long toSeconds(long amount) {
        return toUnit(DurationUnit.SECOND, amount);
    }
    public long toMinutes() {
        return toMinutes(1);
    }
    public long toMinutes(long amount) {
        return toUnit(DurationUnit.MINUTE, amount);
    }
    public long toHours() {
        return toHours(1);
    }
    public long toHours(long amount) {
        return toUnit(DurationUnit.HOUR, amount);
    }
    public long toDays() {
        return toDays(1);
    }
    public long toDays(long amount) {
        return toUnit(DurationUnit.DAY, amount);
    }
    public long toWeeks() {
        return toWeeks(1);
    }
    public long toWeeks(long amount) {
        return toUnit(DurationUnit.WEEK, amount);
    }
    public long toMonths() {
        return toMonths(1);
    }
    public long toMonths(long amount) {
        return toUnit(DurationUnit.MONTH, amount);
    }
    public long toYears() {
        return toYears(1);
    }
    public long toYears(long amount) {
        return toUnit(DurationUnit.YEAR, amount);
    }

    public long toUnit(DurationUnit targetUnit, long amount) {
        return BigDecimal.valueOf(ms).multiply(
                BigDecimal.valueOf(amount)).divide(BigDecimal.valueOf(
                        targetUnit.ms), RoundingMode.DOWN).longValueExact();
    }
    
    /**
     * Gets the largest unit fitting in the provided duration. If the duration
     * is <code>null</code>, zero, or less, <code>null</code> is returned. 
     * @param duration duration in milliseconds
     * @return duration unit
     */
    public static DurationUnit from(Duration duration) {
        if (duration == null) {
            return null;
        }
        return from(duration.toMillis());
    }
    /**
     * Gets the largest unit fitting in the provided duration. If the duration
     * is zero or less, <code>null</code> is returned. 
     * @param duration duration in milliseconds
     * @return duration unit
     */
    public static DurationUnit from(long duration) {
        DurationUnit match = null;
        for (DurationUnit u : values()) {
            if (u.ms <= duration) {
                match = u;
            } else {
                break;
            }
        }
        return match;
    }
    
    /**
     * Gets the DurationUnit matching the provided string (case insensitive)
     * or <code>null</code> if there are no units matching.
     * @param unit unit name
     * @return duration unit
     */
    public static DurationUnit from(String unit) {
        if (unit == null) {
            return null;
        }
        for (DurationUnit u : values()) {
            if (u.name().equalsIgnoreCase(unit)) {
                return u;
            }
        }
        return null;
    }
    public static DurationUnit from(TemporalUnit temporalUnit) {
        if (temporalUnit == null) {
            return null;
        }
        for (DurationUnit u : values()) {
            if (u.chronoUnit == temporalUnit) {
                return u;
            }
        }
        return null;
    }
    
    /**
     * Returns all units from the highest (year), to the smallest 
     * (milliseconds).
     * @return duration units
     */
    public static DurationUnit[] reverseValues() {
        return REVERSE_VALUES;
    }
    /**
     * Gets ordinal value in reverse order. 
     * @return ordinal value
     */
    public int reverseOrdinal() {
        return ArrayUtils.indexOf(REVERSE_VALUES, this);
    }
}
