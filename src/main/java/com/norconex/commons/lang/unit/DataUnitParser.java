/* Copyright 2020-2022 Norconex Inc.
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
package com.norconex.commons.lang.unit;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Parse a textual representation of a data unit and converts it into
 * a {@link BigDecimal} representing the quantity for a given unit
 * (e.g., bytes).
 * </p>
 * <p>
 * If the string is made of digits only, it is assumed to be bytes
 * and the value will remain the same.
 * </p>
 * <p>
 * The data unit can be written in its prefix form, or in full, whether
 * using binary or decimal notations. (e.g., "kB", "kilobyte", "kilobytes").
 * </p>
 * <p>
 * Languages supported are English (default) and French (since 2.0.0).
 * The following are acceptable symbols for each data units, for bytes and bits.
 * The symbols are case-insensitive, and accent-insensitive.
 * </p>
 * <h3>English decimal notation:</h3>
 * <ul>
 *   <li>kB,kilobyte,kilobytes, kbit,kilobit,kilobits</li>
 *   <li>MB,megabyte,megabytes, Mbit,megabit,megabits</li>
 *   <li>GB,gigabyte,gigabytes, Gbit,gigabit,gigabits</li>
 *   <li>TB,terabyte,terabytes, Tbit,terabit,terabits</li>
 *   <li>PB,petabyte,petabytes, Pbit,petabit,petabits</li>
 *   <li>EB,exabyte,exabytes, Ebit,exabit,exabits</li>
 *   <li>ZB,zettabyte,zettabytes, Zbit,zettabit,zettabits</li>
 *   <li>YB,yottabyte,yottabytes, Ybit,yottabit,yottabits</li>
 * </ul>
 * <h3>English binary notation:</h3>
 * <ul>
 *   <li>KiB,kibibyte,kibibytes, kibit,kibibit,kibibits</li>
 *   <li>MiB,mebibyte,mebibytes, Mibit,mebibit,mebibits</li>
 *   <li>GiB,gibibyte,gibibytes, Gibit,gibibit,gibibits</li>
 *   <li>TiB,tebibyte,tebibytes, Tibit,tebibit,tebibits</li>
 *   <li>PiB,pebibyte,pebibytes, Pibit,pebibit,pebibits</li>
 *   <li>EiB,exbibyte,exbiytes, Eibit,exbibit,exbibits</li>
 *   <li>ZiB,zebibyte,zebibytes, Zibit,zebibit,zebibits</li>
 *   <li>YiB,yobibyte,yobibytes, Yibit,yobibit,yobibits</li>
 * </ul>
 *
 * <h3>French notations</h3>
 * <p>
 * French uses the same symbols. Accents aside, the names are also the same,
 * except for replacing "byte" with "octet"
 * (e.g., "gigabyte" becomes "gigaoctet").
 * </p>
 * <p>
 * French typically write the following prefixes with an "&eacute;":
 * m&eacute;ga, m&eacute;bi, t&eacute;ra, t&eacute;bi p&eacute;ta, p&eacute;bi.
 * Both variations are supported (with or without accents).
 * </p>
 *
 * <p>
 *   Refer to {@link DataUnit} for extra information of what they values
 *   represent.
 * </p>
 *
 * <p>
 * No distinction is made between plural and singular.
 * Numeric values can be integers or decimals numbers (e.g., 2.5kB).
 * A numeric
 * value must be followed by a data unit. Other terms or characters
 * are ignored.
 * </p>
 *
 * <h3>Examples:</h3>
 * <p>
 * All of the following will be parsed properly:
 * </p>
 * <ul>
 *   <li>2 gigabytes, 530 megabytes, and 2 kilobytes</li>
 *   <li>6GB10MB23kB</li>
 *   <li>2.5MiB</li>
 *   <li>10PiB9 gibibytes, 8 MB, and 5.5 kibibytes</li>
 *   <li>2 m&eacute;gaoctets et 3 kilooctet</li>
 * </ul>
 * <p>This class is thread-safe and immutable.</p>
 *
 * @since 2.0.0
 */
@Slf4j
public final class DataUnitParser {

    private static final Pattern PATTERN =
            Pattern.compile("(\\d+([\\.,]\\d+)?)(\\D+)");

    private DataUnitParser() {
    }

    /**
     * Parses a text representation of a data measurement and returns
     * the number of bytes it represents.
     * If the value cannot be parsed, a {@link DataUnitParserException}
     * is thrown. Default value is zero byte.
     * @param text the data measurement text to parse
     * @return data measurement
     */
    public static BigDecimal parse(String text) {
        return parseTo(text, null, null, true);
    }

    /**
     * Parses a text representation of a data measurement.
     * If the value cannot be parsed, the default value is returned
     * (no exception is thrown).
     * @param text the data measurement text to parse
     * @param defaultValue default value
     * @return data measurement
     */
    public static BigDecimal parse(String text, BigDecimal defaultValue) {
        return parseTo(text, null, defaultValue, false);
    }

    /**
     * Parses a text representation of a data measurement.
     * If the value cannot be parsed, a {@link DataUnitParserException}
     * is thrown. Default value is zero byte.
     * @param text the data measurement text to parse
     * @param targetUnit desired target unit for the returned amount
     * @return amount for unit
     */
    public static BigDecimal parse(String text, DataUnit targetUnit) {
        return parseTo(text, targetUnit, null, true);
    }

    /**
     * Parses a text representation of a data measurement.
     * If the value cannot be parsed, the default value is returned
     * (no exception is thrown).
     * @param text the data measurement text to parse
     * @param targetUnit desired target unit for the returned amount
     * @param defaultValue default value
     * @return amount for unit
     */
    public static BigDecimal parse(
            String text, DataUnit targetUnit, BigDecimal defaultValue) {
        return parseTo(text, targetUnit, defaultValue, false);
    }

    private static BigDecimal parseTo(String text, DataUnit targetUnit,
            BigDecimal defaultValue, boolean throwException) {

        BigDecimal safeDefault =
                defaultValue == null ? BigDecimal.ZERO : defaultValue;

        if (StringUtils.isBlank(text)) {
            LOG.debug("Blank measurement value. "
                    + "Using default: {}.", safeDefault);
            return safeDefault;
        }

        String safeText = StringUtils.stripAccents(text).trim();
        DataUnit safeTargetUnit = targetUnit == null ? DataUnit.B : targetUnit;

        // If only digits, assuming bytes
        if (NumberUtils.isDigits(safeText)) {
            return BigDecimal.valueOf(NumberUtils.toLong(safeText));
        }

        // There must be at least one digit
        if (!safeText.matches(".*\\d+.*")) {
            parseError(throwException, text);
            return safeDefault;
        }

        // Else parse the string
        Matcher m = PATTERN.matcher(safeText);
        BigDecimal byteAmount = BigDecimal.ZERO;
        boolean matchesPattern = false;
        while (m.find()) {
            matchesPattern = true;
            String numGroup = m.group(1);
            String unitGroup = m.group(3).trim();

            String num = numGroup.replace(',', '.');
            if (!NumberUtils.isParsable(num)) {
                parseError(throwException, text);
                return safeDefault;
            }
            double val = NumberUtils.toDouble(num, -1);
            if (val == -1) {
                parseError(throwException, text);
                return safeDefault;
            }

            String unitStr = unitGroup.replaceFirst("^(\\w+)(.*)", "$1");
            DataUnit unitFromText = DataUnit.from(unitStr);
            if (unitFromText == null) {
                parseError(throwException, text);
                return safeDefault;
            }
            byteAmount = byteAmount.add(unitFromText.toBytes(val));
        }
        if (matchesPattern) {
            return DataUnit.B.to(byteAmount, safeTargetUnit);
        }
        parseError(throwException, text);
        return safeDefault;
    }

    private static void parseError(boolean throwException, String text) {
        String msg = "Invalid or unsupported data measure: \"" + text + "\".";
        if (throwException) {
            throw new DataUnitParserException(msg);
        }
        LOG.error(msg);
    }
}
