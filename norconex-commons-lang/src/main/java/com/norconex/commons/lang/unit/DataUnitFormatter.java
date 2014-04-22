package com.norconex.commons.lang.unit;

import java.io.Serializable;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * Formats a data unit as string.  This class is thread-safe.
 * @author Pascal Essiembre
 * @since 1.4.0
 */
public class DataUnitFormatter implements Serializable {

    private static final long serialVersionUID = -8672773710734223185L;

    private static final DataUnit[] DATA_UNITS = DataUnit.values();
    
    private final Locale locale;
    private final int decimalPrecision;
    private final boolean fixedUnit;
    
    /**
     * Creates a new DataUnit formatter with default system locale, 
     * without decimals.
     */
    public DataUnitFormatter() {
        this(null);
    }
    /**
     * Creates a new DataUnit formatter without decimals.
     * The decimal and thousand separator symbols are set according to 
     * supplied locale.
     * @param locale a locale
     */
    public DataUnitFormatter(Locale locale) {
        this(locale, 0);
    }
    /**
     * Creates a new DataUnit formatter with default system locale
     * with the maximum number of decimals returned when applicable.
     * @param decimalPrecision number of decimals
     */
    public DataUnitFormatter(int decimalPrecision) {
        this(null, decimalPrecision);
    }
    /**
     * Creates a new DataUnit formatter with the maximum number of decimals 
     * returned when applicable. 
     * The decimal and thousand separator symbols are set according to 
     * supplied locale.
     * @param locale a locale
     * @param decimalPrecision number of decimals
     */
    public DataUnitFormatter(Locale locale, int decimalPrecision) {
        this(locale, decimalPrecision, false);
    }
    /**
     * Creates a new DataUnit formatter with the maximum number of decimals 
     * returned when applicable. 
     * The decimal and thousand separator symbols are set according to 
     * supplied locale.
     * Set the <code>fixedUnit</code> to <code>true</code> to ensure
     * the data units supplied are not change in the formatting. 
     * @param locale a locale
     * @param decimalPrecision number of decimals
     * @param fixedUnit <code>true</code> to keep original unit in formatting 
     */
    public DataUnitFormatter(
            Locale locale, int decimalPrecision, boolean fixedUnit) {
        super();
        this.locale = locale;
        this.decimalPrecision = decimalPrecision;
        this.fixedUnit = fixedUnit;
    }

    /**
     * Formats a data amount of the given unit to a human-readable 
     * representation.
     * @param amount the amount to format
     * @param unit the data unit type of the amount
     * @return formatted string
     */
    public String format(long amount, DataUnit unit) {

        // If no unit specified, return as string without a suffix
        if (unit == null) {
            return Long.toString(amount);
        }

        // Use coarser unit if applicable to make value more human-readable
        DataUnit finalUnit = unit;
        long finalAmount = amount;
        int ordinalShift = 0;
        if (!fixedUnit) {
            ordinalShift = (int) (Math.log(amount) / Math.log(1024));
            if (ordinalShift > 0) {
                finalUnit = DATA_UNITS[Math.min(
                        unit.ordinal() + ordinalShift, DATA_UNITS.length -1)];
                finalAmount = finalUnit.convert(amount, unit);
            }
        }

        // Find out decimals
        long decimals = 0;
        if (decimalPrecision > 0 && unit.ordinal() < finalUnit.ordinal()) {
            int previousOrdinal = finalUnit.ordinal() -1;
            if (previousOrdinal >= 0) {
                DataUnit previousUnit = DATA_UNITS[previousOrdinal];
                long originalBytes = unit.toBytes(amount); 
                long finalBytes = finalUnit.toBytes(finalAmount);
                long remainder = DataUnit.B.convert(
                        originalBytes - finalBytes, previousUnit);
                decimals = (remainder * 100) / 1024;
            }
        }

        Locale finalLocale = locale;
        if (finalLocale == null) {
            finalLocale = Locale.getDefault();
        }
        StringBuilder b = new StringBuilder();
        b.append(NumberFormat.getIntegerInstance(
                finalLocale).format(finalAmount));
        if (decimals > 0) {
            b.append(DecimalFormatSymbols.getInstance(
                    finalLocale).getDecimalSeparator());
            b.append(StringUtils.left(
                    Long.toString(decimals), decimalPrecision));
        }
        b.append('\u00A0').append(finalUnit.toString());
        return b.toString();
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + decimalPrecision;
        result = prime * result + (fixedUnit ? 1231 : 1237);
        result = prime * result + ((locale == null) ? 0 : locale.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataUnitFormatter other = (DataUnitFormatter) obj;
        if (decimalPrecision != other.decimalPrecision) {
            return false;
        }
        if (fixedUnit != other.fixedUnit) {
            return false;
        }
        if (locale == null) {
            if (other.locale != null) {
                return false;
            }
        } else if (!locale.equals(other.locale)) {
            return false;
        }
        return true;
    }
    @Override
    public String toString() {
        return "DataUnitFormatter [locale=" + locale + ", decimalPrecision="
                + decimalPrecision + ", fixedUnit=" + fixedUnit + "]";
    }
}
