/* Copyright 2017 Norconex Inc.
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

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.norconex.commons.lang.map.Properties;

/**
 * <p>
 * Parse a textual (English) representation of a duration and converts it into 
 * a <code>long</code> millisecond value.
 * </p>
 * <p>
 * If the string is made of digits only, it is assumed to be a millisecond
 * and the value will remain the same.
 * </p>
 * <p>
 * The duration unit can be written in single character or full words.
 * For instance, "months" can be represented as uppercase "M", "month",
 * or "months".  Some abbreviations are also accepted (e.g., "mo", "mos", 
 * "mth", "mths").  Here is a list you can rely on:
 * </p>
 * <ul>
 *   <li>y,yr,yrs,year,years</li>
 *   <li>M,mo,mos,mth,mths,month,months</li>
 *   <li>w,wk,wks,week,weeks</li>
 *   <li>d,day,days</li>
 *   <li>h,hs,hrs,hour,hours</li>
 *   <li>m,min,minute,minutes</li>
 *   <li>s,sec,second,seconds</li>
 *   <li>S,ms,msec,millis,millisecond,milliseconds</li>
 * </ul>
 * <p>
 * Single-character representation are case sensitive. Other terms are not.
 * No distinction is made between plural and singular.
 * Numeric values can be integers or decimals numbers (e.g., 2.5 months).
 * One month duration uses the average of 30.44 days per month. A numeric
 * value must be followed by a time unit. Other terms or characters
 * are ignored. 
 * </p>
 * <h3>Examples:</h3>
 * <p>
 * All of the following will be parsed properly:
 * </p>
 * <ul>
 *   <li>3 hours, 30 minute, and 30 seconds</li>
 *   <li>6h10m23s</li>
 *   <li>2.5hrs</li>
 *   <li>10y9 months, 8 d, 7hrs, 6 minute, and 5.5 Seconds</li>
 * </ul>
 * 
 * @author Pascal Essiembre
 * @since 1.13.0
 */
public final class DurationParser {

    private static final Logger LOG = 
            LogManager.getLogger(DurationParser.class);

    private static final Properties UNIT_LABELS = new Properties();
    static {
        UNIT_LABELS.setMultiValueDelimiter(",");
        try {
            UNIT_LABELS.load(DurationParser.class.getResourceAsStream(
                    ClassUtils.getShortCanonicalName(DurationParser.class)
                    + ".properties"), ",");
        } catch (IOException e) {
            throw new DurationParserException(
                    "Coult not initialize DurationParser.", e);
        }
    }

    private enum Unit{
        millisecond(1),
        second(millisecond.ms() * 1000),
        minute(second.ms() * 60),
        hour(minute.ms() * 60),
        day(hour.ms() * 24),
        week(day.ms() * 7),
        month((double) (((float) day.ms()) * 30.44f)),
        year(month.ms() * 12);
        private double ms;
        Unit(double ms) {
            this.ms = ms;
        }
        private double ms() {
            return ms;
        }
    };
    
    private static final Pattern PATTERN = 
            Pattern.compile("(\\d+([\\.,]\\d+){0,1})(\\D+)");

    public DurationParser() {
        super();
    }

    /**
     * Parses an English representation of a duration and converts it 
     * to milliseconds.
     * If the value cannot be parsed, a {@link DurationParserException}
     * is thrown.
     * @param duration the duration to parse
     * @return milliseconds
     */
    public static long parse(String duration) {
        return parse(duration, -1, true);
    }
    /**
     * Parses an English representation of a duration and converts it 
     * to milliseconds.
     * If the value cannot be parsed, the default value is returned
     * (no exception is thrown).
     * @param duration the duration to parse
     * @param defaultValue default value
     * @return milliseconds
     */
    public static long parse(String duration, long defaultValue) {
        return parse(duration, defaultValue, false);
    }
    
    private static long parse(
            String duration, long defaultValue, boolean throwException) {
        
        if (StringUtils.isBlank(duration)) {
            parseError(throwException, Level.DEBUG, "Blank duration value.");
            return defaultValue;
        }
        
        // If only digits, consider milliseconds and convert to long
        if (NumberUtils.isDigits(duration.trim())) {
            return NumberUtils.toLong(duration);
        }

        // There must be at least one digit
        if (!duration.matches(".*\\d+.*")) {
            parseError(throwException, Level.ERROR, 
                    "Could not parse duration: \"" 
                  + duration+ "\". No number.");
            return defaultValue;
        }
        
        // Else parse the string
        Matcher m = PATTERN.matcher(duration);
        long ms = 0; 
        boolean matchesPattern = false;
        while (m.find()) {
            matchesPattern = true;
            String numGroup = m.group(1);
            String unitGroup = m.group(3).trim();

            String num = numGroup.replace(',', '.');
            if (!NumberUtils.isParsable(num)) {
                parseError(throwException, Level.ERROR, 
                        "Could not parse duration: \"" + duration
                      + "\". Invalid duration value: " + numGroup);
                return defaultValue;
            }
            float val = NumberUtils.toFloat(num, -1);
            if (val == -1) {
                parseError(throwException, Level.ERROR, 
                        "Could not parse duration: \"" + duration
                      + "\". Invalid duration value: " + numGroup);
                return defaultValue;
            }

            String unitStr = unitGroup.replaceFirst("^(\\w+)(.*)", "$1");
            Unit unit = getUnit(unitStr);
            if (unit == null) {
                parseError(throwException, Level.ERROR, 
                        "Could not parse duration: \"" + duration
                      + "\". Unknown unit: \"" + unitStr + "\".");
                return defaultValue;
            }
            ms += unit.ms * val;
        }
        if (matchesPattern) {
            return ms;
        }
        parseError(throwException, Level.ERROR, 
                "Could not parse duration: \"" + duration
              + "\". Invalid duration value.");
        return defaultValue;
    }
    
    private static void parseError(
            boolean throwException, Priority logLevel, String message) {
        if (throwException) {
            throw new DurationParserException(message);
        }
        LOG.log(logLevel, message);
    }
    
    private static synchronized Unit getUnit(String label) {
        if (StringUtils.isBlank(label)) {
            return null;
        }
        
        // if unit label is 1 char, compare to first item, case sensitive
        if (label.length() == 1) {
            for (String key : UNIT_LABELS.keySet()) {
                if (UNIT_LABELS.getString(key).equals(label)) {
                    return Unit.valueOf(key);
                }
            }
        // if more than 1 char, compare ignoring case
        } else {
            for (Entry<String, List<String>> e : UNIT_LABELS.entrySet()) {
                for (String value : e.getValue()) {
                    if (value.equalsIgnoreCase(label)) {
                        return Unit.valueOf(e.getKey());
                    }
                }
            }
        }
        return null;
    }
}
