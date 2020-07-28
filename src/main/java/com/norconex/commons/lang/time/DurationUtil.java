/* Copyright 2010-2018 Norconex Inc.
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

import java.util.Locale;

/**
 * Utility class for duration-related features.  English and French are the 
 * two supported locale languages.  An unsupported locale will fallback to 
 * English.
 * @author Pascal Essiembre
 * @deprecated Since 2.0.0, use {@link DurationFormatter}.
 */
@Deprecated 
public final class DurationUtil {

    private DurationUtil() {
        super();
    }
    /**
     * Formats a duration using time units abbreviations according to supplied 
     * locale. All matching units are returned.
     * @param locale locale of time unit names
     * @param duration the duration to format
     * @return formatted duration
     */
    public static String formatShort(Locale locale, long duration) {
        return format(locale, duration, false, -1);
    }
    /**
     * Formats a duration using time units full names according to supplied 
     * locale. All matching units are returned.
     * @param locale locale of time unit names
     * @param duration the duration to format
     * @return formatted duration
     */
    public static String formatLong(Locale locale, long duration) {
        return format(locale, duration, true, -1);
    }
    /**
     * Formats a duration using time units abbreviations according to supplied 
     * locale.
     * @param locale locale of time unit names
     * @param duration the duration to format
     * @param maxUnits the number of different time units to return (zero or 
     *                 less returns all matching units)
     * @return formatted duration
     */
    public static String formatShort(
            Locale locale, long duration, int maxUnits) {
        return format(locale, duration, false, maxUnits);
    }
    /**
     * Formats a duration using time units full names according to supplied 
     * locale.
     * @param locale locale of time unit names
     * @param duration the duration to format
     * @param maxUnits the number of different time units to return (zero or 
     *                 less returns all matching units)
     * @return formatted duration
     */
    public static String formatLong(
            Locale locale, long duration, int maxUnits) {
        return format(locale, duration, true, maxUnits);
    }

    private static String format(
            Locale locale, long duration, boolean islong, int maxUnits) {
        DurationFormatter formatter = new DurationFormatter()
                .withLocale(locale)
                .withUnitPrecision(maxUnits)
                .withHighestUnit(DurationUnit.DAY)
                .withLowestUnit(DurationUnit.SECOND);
        if (islong) {
            return formatter.format(duration);
        }
        return formatter
                .withUnitFormatter(RBDurationUnitFormatter.COMPACT)
                .withOuterSeparator(null)
                .withInnerSeparator(null)
                .format(duration).toLowerCase();
    }
}
