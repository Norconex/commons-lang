/* Copyright 2018-2022 Norconex Inc.
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

import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Formats a duration to a string.
 * This class is thread-safe and immutable.
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
public final class DurationFormatter {

    /** Example: 5D18h1m23s */
    public static final DurationFormatter COMPACT = new DurationFormatter()
            .withUnitFormatter(RBDurationUnitFormatter.COMPACT)
            .withInnerSeparator(null)
            .withOuterSeparator(null);

    /** Example: 5 days 18 hours 1 minute 23 seconds */
    public static final DurationFormatter FULL = new DurationFormatter();

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final DurationUnitFormatter DEFAULT_UNIT_FORMATTER =
            RBDurationUnitFormatter.FULL;

    private Locale locale;
    private int unitPrecision;
    private DurationUnit highestUnit;
    private DurationUnit lowestUnit;
    private DurationUnitFormatter unitFormatter;
    private NumberFormat numberFormat;
    private String innerSeparator = " ";
    private String outerSeparator = " ";
    private String outerLastSeparator;

    /**
     * Creates a duration with the default locale and full words for
     * duration units.
     */
    public DurationFormatter() {  //NOSONAR Exists for javadoc
    }

    /**
     * Creates a copy if this formatter with the given locale.  Default
     * locale is English.
     * @param locale locale
     * @return duration formatter copy
     */
    public DurationFormatter withLocale(Locale locale) {
        DurationFormatter df = copy();
        df.locale = locale;
        return df;
    }

    public Locale getLocale() {
        return locale;
    }

    /**
     * Creates a copy if this formatter with the given unit precision.
     * @param unitPrecision unit precision
     * @return duration formatter copy
     */
    public DurationFormatter withUnitPrecision(int unitPrecision) {
        DurationFormatter df = copy();
        df.unitPrecision = unitPrecision;
        return df;
    }

    public int getUnitPrecision() {
        return unitPrecision;
    }

    /**
     * Creates a copy if this formatter with the given number format.
     * Default number format is {@link RBDurationUnitFormatter#FULL}.
     * @param numberFormat number format
     * @return duration formatter copy
     */
    public DurationFormatter withNumberFormat(NumberFormat numberFormat) {
        DurationFormatter df = copy();
        df.numberFormat = numberFormat;
        return df;
    }

    public NumberFormat getNumberFormat() {
        return numberFormat;
    }

    /**
     * Creates a copy if this formatter with the given highest unit.
     * @param highestUnit highest unit
     * @return duration formatter copy
     */
    public DurationFormatter withHighestUnit(DurationUnit highestUnit) {
        DurationFormatter df = copy();
        df.highestUnit = highestUnit;
        return df;
    }

    public DurationUnit getHighestUnit() {
        return highestUnit;
    }

    /**
     * Creates a copy if this formatter with the given lowest unit.
     * @param lowestUnit lowest unit
     * @return duration formatter copy
     */
    public DurationFormatter withLowestUnit(DurationUnit lowestUnit) {
        DurationFormatter df = copy();
        df.lowestUnit = lowestUnit;
        return df;
    }

    public DurationUnit getLowestUnit() {
        return lowestUnit;
    }

    /**
     * Creates a copy if this formatter with the given unit formatter.
     * @param unitFormatter unit formatter
     * @return duration formatter copy
     */
    public DurationFormatter withUnitFormatter(
            DurationUnitFormatter unitFormatter) {
        DurationFormatter df = copy();
        df.unitFormatter = unitFormatter;
        return df;
    }

    public DurationUnitFormatter getUnitFormatter() {
        return unitFormatter;
    }

    /**
     * Creates a copy if this formatter with the specified separator text
     * to be inserted between the numeric and textual values. For instance,
     * specifying an hyphen ('-') for a ten seconds duration would result
     * in: <code>10-seconds</code>.  Default is a single space character.
     * @param innerSeparator inner separator
     * @return duration formatter copy
     */
    public DurationFormatter withInnerSeparator(
            String innerSeparator) {
        DurationFormatter df = copy();
        df.innerSeparator = innerSeparator;
        return df;
    }

    public String getInnerSeparator() {
        return innerSeparator;
    }

    /**
     * Creates a copy if this formatter with the specified separator text
     * to be inserted between each formatter units (number and text pairs).
     * For instance, specifying ", " for two minutes and ten seconds duration
     * would result in: <code>2 minutes, 10 seconds</code>.
     * Default is a single space character.
     * @param outerSeparator outer separator
     * @return duration formatter copy
     */
    public DurationFormatter withOuterSeparator(
            String outerSeparator) {
        DurationFormatter df = copy();
        df.outerSeparator = outerSeparator;
        return df;
    }

    public String getOuterSeparator() {
        return outerSeparator;
    }

    /**
     * Creates a copy if this formatter with the specified separator text
     * to be inserted between the second to last and last formatter units
     * (number and text pairs). For instance, specifying " and " for
     * 1 hour, two minutes and ten seconds duration
     * would result in: <code>1 hour 2 minutes and 10 seconds</code>.
     * Default is <code>null</code> (fallsback to
     * using {@link #getOuterSeparator()}).
     * @param outerLastSeparator last outer separator
     * @return duration formatter copy
     */
    public DurationFormatter withOuterLastSeparator(
            String outerLastSeparator) {
        DurationFormatter df = copy();
        df.outerLastSeparator = outerLastSeparator;
        return df;
    }

    public String getOuterLastSeparator() {
        return outerLastSeparator;
    }

    /**
     * Formats the given duration to a string.
     * @param duration the duration to format
     * @return formatted duration
     */
    public String format(Duration duration) {
        if (duration == null) {
            return null;
        }
        return format(duration.toMillis());
    }

    /**
     * Formats the given duration (in milliseconds) to a string.
     * @param duration the duration to format
     * @return formatted duration
     */
    public String format(long duration) {
        DurationUnit highest =
                highestUnit == null ? DurationUnit.YEAR : highestUnit;
        DurationUnit lowest =
                lowestUnit == null ? DurationUnit.MILLISECOND : lowestUnit;
        if (lowest.ordinal() > highest.ordinal()) {
            DurationUnit h = highest;
            highest = lowest;
            lowest = h;
        }

        // If duration is zero, don't bother computing, return lowest unit
        if (duration == 0) {
            return format(0, lowest).trim();
        }

        // If precision is greater than highest minus lowest, it
        // is reduced to match that difference.
        int precision = unitPrecision;
        if (precision < 1) {
            precision = DurationUnit.values().length;
        }
        precision = Math.min(
                highest.ordinal() - lowest.ordinal() + 1, precision);

        // Proceed
        long remainder = duration;
        int unitCount = 0;
        List<String> formattedParts = new ArrayList<>();

        for (DurationUnit u : ArrayUtils.subarray(DurationUnit.reverseValues(),
                highest.reverseOrdinal(), lowest.reverseOrdinal() + 1)) {
            // break if we have reached max desired number of units (precision)
            if (unitCount == precision) {
                break;
            }

            long factor = remainder / u.toMilliseconds();
            if (factor > 0) {
                formattedParts.add(format(factor, u));
                unitCount++;
                remainder = remainder % u.toMilliseconds();
            } else if (unitCount > 0) {
                unitCount++;
            }
        }

        if (unitCount == 0) {
            return format(0, lowest).trim();
        }

        // merge acording to outer separators
        StringBuilder b = new StringBuilder();
        int index = 0;
        int lastIndex = formattedParts.size() - 1;
        for (String part : formattedParts) {
            if (b.length() > 0) {
                b.append(resolveOuterSeparator(index, lastIndex));
            }
            b.append(part);
            index++;
        }
        return b.toString().trim();
    }

    private String resolveOuterSeparator(int index, int lastIndex) {
        if (index == lastIndex && outerLastSeparator != null) {
            return outerLastSeparator;
        }
        return StringUtils.defaultString(outerSeparator);
    }

    private String format(long factor, DurationUnit unit) {
        StringBuilder b = new StringBuilder();

        // Numeric value
        if (numberFormat != null) {
            b.append(numberFormat.format(factor));
        } else {
            b.append(factor);
        }

        // Inner separator
        b.append(StringUtils.defaultString(innerSeparator));

        // Textual value
        DurationUnitFormatter duf =
                unitFormatter == null ? DEFAULT_UNIT_FORMATTER : unitFormatter;
        Locale safeLocale = locale == null ? DEFAULT_LOCALE : locale;
        b.append(duf.format(unit, safeLocale, factor > 1));
        return b.toString();
    }

    private DurationFormatter copy() {
        DurationFormatter df = new DurationFormatter();
        df.locale = locale;
        df.unitPrecision = unitPrecision;
        df.highestUnit = highestUnit;
        df.lowestUnit = lowestUnit;
        df.unitFormatter = unitFormatter;
        df.numberFormat = numberFormat;
        df.innerSeparator = innerSeparator;
        df.outerSeparator = outerSeparator;
        df.outerLastSeparator = outerLastSeparator;
        return df;
    }
}
