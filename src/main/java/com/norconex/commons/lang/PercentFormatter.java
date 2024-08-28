/* Copyright 2014-2021 Norconex Inc.
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
package com.norconex.commons.lang;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats percentage as string.  This class is thread-safe.
 * Methods expecting a denominator will always return zero percent if said
 * denominator is zero.
 * @since 1.4.0
 */
public class PercentFormatter implements Serializable {

    //MAYBE: consider having a builder instead of multi-args.

    private static final long serialVersionUID = 3403860660255503075L;

    private final Locale locale;
    private final int decimalPrecision;

    /**
     * Creates a new percent formatter with no decimal precision and using
     * the default system locale.
     */
    public PercentFormatter() {
        decimalPrecision = 0;
        locale = null;
    }

    /**
     * Creates a new percent formatter for the given local and decimal
     * precision.
     * @param decimalPrecision maximum number of decimals to display
     * @param locale a locale
     */
    public PercentFormatter(int decimalPrecision, Locale locale) {
        this.decimalPrecision = decimalPrecision;
        this.locale = locale;
    }

    /**
     * Formats fraction values as percentage string.
     * @param numerator the value to divide
     * @param denominator the divider
     * @return formatted percent string
     */
    public String format(double numerator, double denominator) {
        return format(numerator, denominator, decimalPrecision, locale);
    }

    /**
     * Formats a fraction as percentage string.
     * @param fraction the value to format as percentage
     * @return formatted percent string
     */
    public String format(double fraction) {
        return format(fraction, decimalPrecision, locale);
    }

    /**
     * Formats fraction values as percentage with the given local and
     * decimal precision.
     * @param numerator the value to divide
     * @param denominator the divider
     * @param decimalPrecision maximum number of decimals to display
     * @param locale a locale
     * @return formatted percent string
     */
    public static String format(double numerator, double denominator,
            int decimalPrecision, Locale locale) {
        if (denominator == 0d) {
            return format(0d, decimalPrecision, locale);
        }
        return format(BigDecimal.valueOf(numerator).divide(
                BigDecimal.valueOf(denominator), decimalPrecision + 2,
                RoundingMode.HALF_UP).doubleValue(),
                decimalPrecision + 2, locale);
    }

    /**
     * Formats a fraction as percentage with the given local and
     * decimal precision.
     * @param fraction the value to format as percentage
     * @param decimalPrecision maximum number of decimals to display
     * @param locale a locale
     * @return formatted percent string
     */
    public static String format(
            double fraction, int decimalPrecision, Locale locale) {
        Locale safeLocale = locale;
        if (safeLocale == null) {
            safeLocale = Locale.getDefault();
        }
        //MAYBE cache message format with locale?
        NumberFormat percentFormat =
                NumberFormat.getPercentInstance(safeLocale);
        percentFormat.setMaximumFractionDigits(decimalPrecision);
        //MAYBE consider returning <0.1% when not zero but too small to
        //represent
        return percentFormat.format(fraction);
    }
}
