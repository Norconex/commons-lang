package com.norconex.commons.lang;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats percentage as string.  This class is thread-safe.
 * @author Pascal Essiembre
 * @since 1.4.0
 */
public class PercentFormatter implements Serializable {

    private static final long serialVersionUID = 3403860660255503075L;
    
    private final Locale locale;
    private final int decimalPrecision;

    /**
     * Creates a new percent formatter with no decimal precision and using
     * the default system locale.
     */
    public PercentFormatter() {
        super();
        this.decimalPrecision = 0;
        this.locale = null;
    }
    
    /**
     * Creates a new percent formatter for the given local and decimal 
     * precision.
     * @param decimalPrecision maximum number of decimals to display
     * @param locale a locale
     */
    public PercentFormatter(int decimalPrecision, Locale locale) {
        super();
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
        //TODO cache message format with locale?
        NumberFormat percentFormat = 
                NumberFormat.getPercentInstance(safeLocale);
        percentFormat.setMaximumFractionDigits(decimalPrecision);
        //TODO consider returning <0.1% when not zero but too small to represent
        return percentFormat.format(fraction);
    }
}
