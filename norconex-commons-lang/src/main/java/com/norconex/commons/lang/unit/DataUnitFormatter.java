/* Copyright 2010-2014 Norconex Inc.
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

import java.io.Serializable;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * Formats a data unit as string.  This class is thread-safe.
 * @author Pascal Essiembre
 * @since 1.4.0
 */
public class DataUnitFormatter implements Serializable {

    private static final long serialVersionUID = -8672773710734223185L;

    private static final DataUnit[] DATA_UNITS = DataUnit.values();
    private static final int K = 1024;
    private static final int D = 10;
    
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
            ordinalShift = (int) (Math.log(amount) / Math.log(K));
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
                long originalBytes = unit.toBytes(amount); 
                long finalBytes = finalUnit.toBytes(finalAmount);
                long diff = originalBytes - finalBytes;
                DataUnit previousUnit = DATA_UNITS[previousOrdinal];
                long remainder = previousUnit.convert(diff, DataUnit.B);
                long base = remainder * (long) Math.pow(D, decimalPrecision);
                decimals = base / K;
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
    public String toString() {
        return "DataUnitFormatter [locale=" + locale + ", decimalPrecision="
                + decimalPrecision + ", fixedUnit=" + fixedUnit + "]";
    }
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DataUnitFormatter)) {
            return false;
        }
        DataUnitFormatter castOther = (DataUnitFormatter) other;
        return new EqualsBuilder().append(locale, castOther.locale)
                .append(decimalPrecision, castOther.decimalPrecision)
                .append(fixedUnit, castOther.fixedUnit).isEquals();
    }
    private transient int hashCode;

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(locale)
                    .append(decimalPrecision).append(fixedUnit).toHashCode();
        }
        return hashCode;
    }
}
