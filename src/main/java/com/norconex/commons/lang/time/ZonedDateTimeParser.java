/* Copyright 2023 Norconex Inc.
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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * <p>
 * Parses a string representation of a date to a {@link ZonedDateTime}.
 * </p>
 *
 * <h2>Supported date string formats</h2>
 * <ul>
 *   <li>
 *     A string parsable by {@link DateTimeFormatter}, as per the format
 *     specified in {@link #getFormat()}. If the parser format represents
 *     a date without time, it will be set at the beginning
 *     of the day (midnight). If no format is explicitly set on the parser,
 *     parsing using other supported formats will be attempted before
 *     throwing a {@link DateTimeException}.
 *   </li>
 *   <li>
 *     Digits only, representing milliseconds (EPOCH date).
 *   </li>
 *   <li>
 *     <code>TODAY[-+]9[YMDhms]</code> (see "Relative Dates" below)
 *   </li>
 *   <li>
 *     <code>NOW[-+]9[YMDhms]</code> (see "Relative Dates" below)
 *   </li>
 * </ul>
 *
 * <h2>Time zones</h2>
 * <p>
 * You can dictate which time zone the returned date-time should be,
 * with {@link #setZoneId(ZoneId)}.
 * </p>
 * <p>
 * If no zone id is set on the parser but the parser date format has a zone
 * symbol, the returned date-time value will be set to the time zone
 * detected in the date string value (if present). If no time zone is detected,
 * the returned date-time zone will be UTC.
 * </p>
 * <p>
 * If a zone id is set on the parser and no time zone was parsed from the
 * date strings, the returned date-time will be set to the parser zone id.
 * Date strings with detected time zones will be converted to the parser
 * zone id.
 * </p>
 *
 * <h2>Relative dates</h2>
 * <P>
 * Date string values can be moment in time relative to the
 * current date using the <code>TODAY</code> or <code>NOW</code> keyword
 * (case sensitive), optionally followed by a number of time units to
 * add/remove.
 * <code>TODAY</code> is the current day without the hours, minutes, and
 * seconds (midnight), where as <code>NOW</code> is the current day with the
 * hours, minutes, and seconds. The time units that can be added or subtracted
 * are (case sensitive):
 * </p>
 * <ul>
 *   <li><strong>Y</strong>: Year</li>
 *   <li><strong>M</strong>: Month</li>
 *   <li><strong>D</strong>: Day</li>
 *   <li><strong>h</strong>: Hour</li>
 *   <li><strong>m</strong>: Minute</li>
 *   <li><strong>s</strong>: Second</li>
 * </ul>
 * <P>
 * For example, if today's date is July 1st 2023 and you want to represent
 * a year ago (without time), the format value would be:
 * <code>TODAY-1Y</code>.
 * </P>
 */
@Builder
@Data
public class ZonedDateTimeParser {

    public enum TimeUnit {
        YEAR(ChronoUnit.YEARS, "Y"),
        MONTH(ChronoUnit.MONTHS, "M"),
        DAY(ChronoUnit.DAYS, "D"),
        HOUR(ChronoUnit.HOURS, "h"),
        MINUTE(ChronoUnit.MINUTES, "m"),
        SECOND(ChronoUnit.SECONDS, "s");

        private final TemporalUnit temporalUnit;
        private final String abbr;

        TimeUnit(TemporalUnit temporalUnit, String abbr) {
            this.temporalUnit = temporalUnit;
            this.abbr = abbr;
        }

        public TemporalUnit toTemporal() {
            return temporalUnit;
        }

        @Override
        public String toString() {
            return abbr;
        }

        public static TimeUnit getTimeUnit(String unit) {
            if (StringUtils.isBlank(unit)) {
                return null;
            }
            for (TimeUnit tu : TimeUnit.values()) {
                if (tu.abbr.equalsIgnoreCase(unit)) {
                    return tu;
                }
            }
            return null;
        }
    }

    private static final Pattern RELATIVE_DATE_PATTERN = Pattern.compile(
            //1              23            4         5
            "^(NOW|TODAY)\\s*(([-+]{1})\\s*(\\d+)\\s*([YMDhms]{1})\\s*)?$");

    private enum Group {
        ALL, // 0
        NOW_TODAY, // 1
        MODIFIER_PARTS, // 2
        PLUS_MINUS, // 3
        AMOUNT, // 4
        TIME_UNIT // 5
    }

    private String format;
    @Default
    private Locale locale = Locale.getDefault();
    private ZoneId zoneId;

    /**
     * Parses a string representation of a date.
     * @param dateString representation of a date
     * @return a zoned date-time or null if the date string is blank.
     * @throws DateTimeException if the date string cannot be parsed
     */
    public ZonedDateTime parse(String dateString) {
        var dt = StringUtils.trimToNull(dateString);
        if (dt == null) {
            return null;
        }

        // at this point, neither the parser format or date string can be null

        // Format
        var fmt = StringUtils.trimToNull(format);
        if (fmt != null) {
            return ofFormat(dt);
        }

        // Relative (TODAY/NOW)
        if (StringUtils.startsWithAny(dt.toUpperCase(), "TODAY", "NOW")) {
            return ofRelative(dateString);
        }

        // EPOCH
        if (NumberUtils.isDigits(dt)) {
            return ofEpoch(dt);
        }

        throw new DateTimeException("Cannot parse date %s with: %s".formatted(
                dateString, this));
    }

    private ZonedDateTime ofFormat(String dateStr) {
        var zdt = ofFormat(
                dateStr,
                DateTimeFormatter.ofPattern(format,
                        locale == null ? Locale.ENGLISH : locale),
                zoneId);
        if (zdt == null) {
            throw new DateTimeException("Could not parse date '" + dateStr
                    + "' with format: " + format);
        }
        return zdt;
    }

    static ZonedDateTime ofFormat(
            String dateStr, DateTimeFormatter formatter, ZoneId targetZone) {
        // direct parsing
        try {
            return formatter.parse(dateStr, ZonedDateTime::from);
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            // Swallow, we try something else on failure
        }
        try {
            return formatter.parse(dateStr, LocalDateTime::from).atZone(
                    zoneIdOrUTC(targetZone));
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            // Swallow, we try something else on failure
        }
        try {
            return formatter.parse(dateStr, LocalDate::from).atStartOfDay(
                    zoneIdOrUTC(targetZone));
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            // Swallow, we try something else on failure
        }

        // with optional format elements
        var dtf = new DateTimeFormatterBuilder().append(formatter)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter();

        var parsed = dtf.parseBest(dateStr,
                ZonedDateTime::from,
                LocalDateTime::from,
                LocalDate::from);

        // if it's a ZonedDateTime, return it
        if (parsed instanceof ZonedDateTime zdt) {
            if (targetZone != null) {
                zdt = ZonedDateTime.ofInstant(zdt.toInstant(), targetZone);
            }
            return zdt;
        }
        if (parsed instanceof LocalDateTime dt) {
            return dt.atZone(zoneIdOrUTC(targetZone));
        }
        if (parsed instanceof LocalDate ld) {
            return ld.atStartOfDay(zoneIdOrUTC(targetZone));
        }
        return null;
    }

    private ZonedDateTime ofEpoch(String dateStr) {
        try {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(
                    Long.parseLong(dateStr)),
                    Optional.ofNullable(zoneId).orElse(ZoneOffset.UTC));
        } catch (NumberFormatException e) {
            throw new DateTimeException(
                    "Could not parse EPOCH date string: " + dateStr, e);
        }
    }

    private ZonedDateTime ofRelative(String dateStr) {
        // [TODAY|NOW][-+]9[YMDhms]
        var m = RELATIVE_DATE_PATTERN.matcher(dateStr);
        if (!m.matches()) {
            throw new DateTimeException(
                    "Could not parse TODAY date string: " + dateStr);
        }

        var now = ZonedDateTime.now(zoneIdOrUTC(zoneId));

        // If TODAY, we truncate before calculations to allow adding hours
        // finer to the begining of day.
        if ("TODAY".equals(m.group(Group.NOW_TODAY.ordinal()))) {
            now = now.truncatedTo(ChronoUnit.DAYS);
        }

        var amount = NumberUtils.toInt(m.group(Group.AMOUNT.ordinal()), -1);
        if (amount > -1) {
            if ("-".equals(m.group(Group.PLUS_MINUS.ordinal()))) {
                amount = -amount;
            }
            var unit = TimeUnit.getTimeUnit(m.group(Group.TIME_UNIT.ordinal()));
            if (unit == null) {
                throw new DateTimeException("Invalid time unit: " + dateStr);
            }
            now = now.plus(amount, unit.toTemporal());
        }
        return now;
    }

    private static ZoneId zoneIdOrUTC(ZoneId zoneId) {
        return Optional.ofNullable(zoneId).orElse(ZoneOffset.UTC);
    }
}
