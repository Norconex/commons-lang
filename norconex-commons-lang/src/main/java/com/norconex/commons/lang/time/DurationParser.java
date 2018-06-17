/* Copyright 2017-2018 Norconex Inc.
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

import java.time.Duration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Parse a textual representation of a duration and converts it into 
 * a <code>long</code> millisecond value.  
 * </p>
 * <p>
 * If the string is made of digits only, it is assumed to be a millisecond
 * and the value will remain the same.
 * </p>
 * <p>
 * The duration unit can be written in single character or full words.
 * Some abbreviations are also accepted (e.g., "mo", "mos", 
 * "mth", "mths").
 * </p>
 * <p>
 * Languages supported are English (default) and French (since 2.0.0).
 * Here is are acceptable values for each duration units.
 * </p>
 * <h3>English:</h3>
 * <ul>
 *   <li>Y,yr,yrs,year,years</li>
 *   <li>M,mo,mos,mth,mths,month,months</li>
 *   <li>W,wk,wks,week,weeks</li>
 *   <li>D,day,days</li>
 *   <li>h,hs,hrs,hour,hours</li>
 *   <li>m,min,mins,minute,minutes</li>
 *   <li>s,sec,secs,second,seconds</li>
 *   <li>ms,msec,msecs,millis,millisecond,milliseconds</li>
 * </ul>
 * <h3>French:</h3>
 * <ul>
 *   <li>A,an,ans,ann&eacute;e,ann&eacute;es</li>
 *   <li>M,mo,mos,mois</li>
 *   <li>S,sem,sems,semaine,semaines</li>
 *   <li>J,jour,jours,journ&eacute;e,journ&eacute;es</li>
 *   <li>h,hr,hrs,heure,heures</li>
 *   <li>m,min,mins,minute,minutes</li>
 *   <li>s,sec,secs,seconde,secondes</li>
 *   <li>ms,msec,msecs,millis,milliseconde,millisecondes</li>
 * </ul>
 * <p>
 * Single-character representation are case sensitive. Other terms are not.
 * No distinction is made between plural and singular.
 * Numeric values can be integers or decimals numbers (e.g., 2.5 months).
 * One year uses the average of 365.2425 days and a month is 1/12th of that.
 * A numeric
 * value must be followed by a time unit. Other terms or characters
 * are ignored. 
 * </p>
 * 
 * <h3>Examples:</h3>
 * <p>
 * All of the following will be parsed properly:
 * </p>
 * <ul>
 *   <li>3 hours, 30 minute, and 30 seconds</li>
 *   <li>6h10m23s</li>
 *   <li>2.5hrs</li>
 *   <li>10y9 months, 8 d, 7hrs, 6 minute, and 5.5 Seconds</li>
 *   <li>2 ans et 3 mois</li>
 * </ul>
 * <p>This class is thread-safe and immutable.</p>
 * 
 * @author Pascal Essiembre
 * @since 1.13.0
 */
public class DurationParser {

    private static final Logger LOG = 
            LoggerFactory.getLogger(DurationParser.class);
    
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;    

    
    private static final Pattern PATTERN = 
            Pattern.compile("(\\d+([\\.,]\\d+){0,1})(\\D+)");

    private Locale locale;
    
    /**
     * Creates a copy if this parser with the given locale.  Default 
     * locale is English. 
     * @param locale locale
     * @return duration parser copy
     */
    public DurationParser withLocale(Locale locale) {
        DurationParser dp = copy();
        dp.locale = locale;
        return dp;
    }
    public Locale getLocale() {
        return locale;
    }    

    /**
     * Parses a text representation of a duration.
     * If the value cannot be parsed, a {@link DurationParserException}
     * is thrown. Default value is zero (no duration).
     * @param duration the duration text to parse
     * @return duration
     */
    public Duration parse(String duration) {
        return Duration.ofMillis(parseToMillis(duration, 0, true));
    }
    /**
     * Parses a text representation of a duration.
     * If the value cannot be parsed, the default value is returned
     * (no exception is thrown).
     * @param duration the duration text to parse
     * @param defaultValue default value
     * @return duration
     */
    public Duration parse(String duration, Duration defaultValue) {
        long defVal = defaultValue == null ? 0 : defaultValue.toMillis();
        return Duration.ofMillis(parseToMillis(duration, defVal, false));
    }
    
    /**
     * Parses a text representation of a duration.
     * If the value cannot be parsed, a {@link DurationParserException}
     * is thrown. Default value is zero (no duration)
     * @param duration the duration text to parse
     * @return milliseconds
     */
    public long parseToMillis(String duration) {
        return parseToMillis(duration, 0, true);
    }
    /**
     * Parses a text representation of a duration.
     * If the value cannot be parsed, the default value is returned
     * (no exception is thrown).
     * @param duration the duration text to parse
     * @param defaultValue default value
     * @return milliseconds
     */
    public long parseToMillis(String duration, long defaultValue) {
        return parseToMillis(duration, defaultValue, false);
    }
    
    private Long parseToMillis(
            String duration, long defaultValue, boolean throwException) {
        
        if (StringUtils.isBlank(duration)) {
            LOG.debug("Blank duration value. Using default: {}.", defaultValue);
            return defaultValue;
        }
        
        // If only digits, consider milliseconds and convert to long
        if (NumberUtils.isDigits(duration.trim())) {
            return NumberUtils.toLong(duration);
        }

        // There must be at least one digit
        if (!duration.matches(".*\\d+.*")) {
            parseError(throwException, "No number.", duration);
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
                parseError(throwException, 
                        "Invalid duration value: \"%s\".", duration, numGroup);
                return defaultValue;
            }
            float val = NumberUtils.toFloat(num, -1);
            if (val == -1) {
                parseError(throwException, 
                        "Invalid duration value: \"%s\".", duration, numGroup);
                return defaultValue;
            }

            String unitStr = unitGroup.replaceFirst("^(\\w+)(.*)", "$1");
            DurationUnit unit = getUnit(unitStr);
            if (unit == null) {
                parseError(throwException, 
                        "Unknown unit: \"%s\".", duration, unitStr);
                return defaultValue;
            }
            ms += (long) (unit.toMilliseconds() * val);
        }
        if (matchesPattern) {
            return ms;
        }
        parseError(throwException, 
                "Could not parse duration: \"%s\". Invalid duration value.", 
                duration);
        return defaultValue;
    }
    
    private void parseError(boolean throwException, String message, 
            String duration, Object... args) {
        String msg = "Could not parse duration: \"%s\". "
            + String.format(message, duration, args);
        
        if (throwException) {
            throw new DurationParserException(msg);
        }
        LOG.error(msg);
    }
    
    //TODO if performance is a concern, consider weak-caching most 
    //frequently used.
    private DurationUnit getUnit(String label) {
        if (StringUtils.isBlank(label)) {
            return null;
        }
        Locale safeLocale = locale == null ? DEFAULT_LOCALE : locale;
        
        ResourceBundle rb = ResourceBundle.getBundle(
                DurationParser.class.getCanonicalName(), safeLocale);

        for (String key : rb.keySet()) {
            String[] variants = rb.getString(key).split(",");
            if ((label.length() == 1 && StringUtils.equalsAny(label, variants))
                    || (label.length() > 1 
                        && StringUtils.equalsAnyIgnoreCase(label, variants))) {
                return DurationUnit.from(key);
            }
        }
        return null;
    }
    
    private DurationParser copy() {
        DurationParser dp = new DurationParser();
        dp.locale = locale;
        return dp;
    }
    
    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, 
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
