/* Copyright 2010-2020 Norconex Inc.
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

import static com.norconex.commons.lang.collection.CollectionUtil.unmodifiableList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * A <tt>DataUnit</tt> offers representation and conversion of various
 * quantity of bytes/bits. Release 2.0.0 introduces exa, zetta, and yotta
 * units.
 * </p>
 *
 * <h3>Decimal vs binary notations</h3>
 * <p>
 * As of 2.0.0, this class aligns with the international standard
 * system of units by treating
 * <a href="https://en.wikipedia.org/wiki/Kilobyte">decimal and binary
 * notations</a> distinctively. Each are represented as:
 * </p>
 * <table>
 *   <caption class="d-none">Data unit notations</caption>
 *   <tr><th colspan="3">Decimal</th><th colspan="3">Binary</th></tr>
 *   <tr><th>k</th><td>kilo</td><td>10<sup>3</sup></td>
 *       <th>Ki</th><td>kibi</td><td>2<sup>10</sup></td></tr>
 *   <tr><th>M</th><td>mega</td><td>10<sup>6</sup></td>
 *       <th>Mi</th><td>mebi</td><td>2<sup>20</sup></td></tr>
 *   <tr><th>G</th><td>giga</td><td>10<sup>9</sup></td>
 *       <th>Gi</th><td>gibi</td><td>2<sup>30</sup></td></tr>
 *   <tr><th>T</th><td>tera</td><td>10<sup>12</sup></td>
 *       <th>Ti</th><td>tebi</td><td>2<sup>40</sup></td></tr>
 *   <tr><th>P</th><td>peta</td><td>10<sup>15</sup></td>
 *       <th>Pi</th><td>pebi</td><td>2<sup>50</sup></td></tr>
 *   <tr><th>E</th><td>exa</td><td>10<sup>18</sup></td>
 *       <th>Ei</th><td>exbi</td><td>2<sup>60</sup></td></tr>
 *   <tr><th>Z</th><td>zetta</td><td>10<sup>21</sup></td>
 *       <th>Zi</th><td>zebi</td><td>2<sup>70</sup></td></tr>
 *   <tr><th>Y</th><td>yotta</td><td>10<sup>24</sup></td>
 *       <th>Yi</th><td>yobi</td><td>2<sup>80</sup></td></tr>
 * </table>
 *
 * <h3>Bytes vs bits</h3>
 * <p>
 * As of 2.0.0, this class supports bits in addition to bytes. They are
 * represented as:
 * </p>
 * <table>
 *   <caption class="d-none">Bytes and bits notations</caption>
 *   <tr><th>B</th><td>bytes (8-bit)</td></tr>
 *   <tr><th>bit</th><td>bits</td></tr>
 * </table>
 *
 *
 * <h3>Usage examples</h3>
 *
 * <pre>
 *   // how many kibibyte in a gibiabyte amount (binary notation).
 *   long kibibyte = DataUnit.GIB.to(3, KIB).longValue(); // results = 3072
 *
 *   // how many megabyte in a kilobyte amount (decimal notation)
 *   float megabyte = DataUnit.KB.to(2500, MB).floatValue(); // results = 2.5
 * </pre>
 *
 * @since 1.4.0
 * @author Pascal Essiembre
 * @see DataUnitFormatter
 */
public enum DataUnit {

    /** A single bit. */
    BIT(0, "bit", "bit", 1),
    /** A single byte (8 bits). */
    B(0, "B", "byte", 8),

    //--- Decimal bits ---
    KBIT(1, "kbit", "kilo",  10,  3),
    MBIT(2, "Mbit", "mega",  10,  6),
    GBIT(3, "Gbit", "giga",  10,  9),
    TBIT(4, "Tbit", "tera",  10, 12),
    PBIT(5, "Pbit", "peta",  10, 15),
    EBIT(6, "Ebit", "exa",   10, 18),
    ZBIT(7, "Zbit", "zetta", 10, 21),
    YBIT(8, "Ybit", "yotta", 10, 24),

    //--- Decimal bytes ---
    KB(1, "kB", "kilo",  10,  3, 8),
    MB(2, "MB", "mega",  10,  6, 8),
    GB(3, "GB", "giga",  10,  9, 8),
    TB(4, "TB", "tera",  10, 12, 8),
    PB(5, "PB", "peta",  10, 15, 8),
    EB(6, "EB", "exa",   10, 18, 8),
    ZB(7, "ZB", "zetta", 10, 21, 8),
    YB(8, "YB", "yotta", 10, 24, 8),

    //--- Binary bits ---
    KIBIT(1, "Kibit", "kibi", 2, 10),
    MIBIT(2, "Mibit", "mebi", 2, 20),
    GIBIT(3, "Gibit", "gibi", 2, 30),
    TIBIT(4, "Tibit", "tebi", 2, 40),
    PIBIT(5, "Pibit", "pebi", 2, 50),
    EIBIT(6, "Eibit", "exi",  2, 60),
    ZIBIT(7, "Zibit", "zebi", 2, 70),
    YIBIT(8, "Yibit", "yobi", 2, 80),

    //--- Binary bytes ---
    KIB(1, "KiB", "kibi", 2, 10, 8),
    MIB(2, "MiB", "mebi", 2, 20, 8),
    GIB(3, "GiB", "gibi", 2, 30, 8),
    TIB(4, "TiB", "tebi", 2, 40, 8),
    PIB(5, "PiB", "pebi", 2, 50, 8),
    EIB(6, "EiB", "exi",  2, 60, 8),
    ZIB(7, "ZiB", "zebi", 2, 70, 8),
    YIB(8, "YiB", "yobi", 2, 80, 8),

    ;

    public static final List<DataUnit> DECIMAL_BIT_UNITS = unmodifiableList(
            BIT, KBIT, MBIT, GBIT, TBIT, PBIT, EBIT, ZBIT, YBIT);
    public static final List<DataUnit> DECIMAL_BYTE_UNITS = unmodifiableList(
            B, KB, MB, GB, TB, PB, EB, ZB, YB);
    public static final List<DataUnit> BINARY_BIT_UNITS = unmodifiableList(
            BIT, KIBIT, MIBIT, GIBIT, TIBIT, PIBIT, EIBIT, ZIBIT, YIBIT);
    public static final List<DataUnit> BINARY_BYTE_UNITS = unmodifiableList(
            B, KIB, MIB, GIB, TIB, PIB, EIB, ZIB, YIB);

    private final BigDecimal bits;
    private final String symbol;
    private final String prefix;
    private final int index;

    private DataUnit(int index, String symbol, String prefix, long bits) {
        this.bits = BigDecimal.valueOf(bits);
        this.symbol = symbol;
        this.prefix = prefix;
        this.index = index;
    }
    private DataUnit(int index, String symbol, String prefix, int base, int power) {
        this.bits = BigDecimal.valueOf(base).pow(power);
        this.symbol = symbol;
        this.prefix = prefix;
        this.index = index;
    }
    private DataUnit(int index, String symbol,
            String prefix, int base, int power, int multiplier) {
        this.bits = BigDecimal.valueOf(
                base).pow(power).multiply(BigDecimal.valueOf(multiplier));
        this.symbol = symbol;
        this.prefix = prefix;
        this.index = index;
    }

    public String getSymbol() {
        return symbol;
    }
    public String getPrefix() {
        return prefix;
    }
    public String getName() {
        if (this == BIT || this == B) {
            return prefix;
        }
        return prefix + (isBitUnit() ? "bit" : "byte");
    }
    public int getGroupIndex() {
        return index;
    }

    public boolean isBitUnit() {
        return this.name().contains("BIT");
    }
    public boolean isByteUnit() {
        return !this.name().contains("BIT");
    }
    public boolean isBinary() {
        return BINARY_BIT_UNITS.contains(this)
                || BINARY_BYTE_UNITS.contains(this);
    }
    public boolean isDecimal() {
        return DECIMAL_BIT_UNITS.contains(this)
                || DECIMAL_BYTE_UNITS.contains(this);
    }

    public BigInteger bits() {
        return bits.toBigInteger();
    }
    public BigInteger bytes() {
        return bits.toBigInteger().divide(BigInteger.valueOf(8));
    }

    public BigDecimal toBits(double amount) {
        return toBits(BigDecimal.valueOf(amount));
    }
    public BigDecimal toBits(BigDecimal amount) {
        Objects.requireNonNull(amount, "'amount' must not be null.");
        return bits.multiply(amount);
    }
    public BigDecimal toBytes(double amount) {
        return toBytes(BigDecimal.valueOf(amount));
    }
    public BigDecimal toBytes(BigDecimal amount) {
        return toBits(amount).divide(BigDecimal.valueOf(8));
    }

    public BigDecimal fromBits(double amount) {
        return fromBits(BigDecimal.valueOf(amount));
    }
    public BigDecimal fromBits(BigDecimal amount) {
        Objects.requireNonNull(amount, "'amount' must not be null.");
        return amount.divide(bits);
    }
    public BigDecimal fromBytes(double amount) {
        return fromBytes(BigDecimal.valueOf(amount));
    }
    public BigDecimal fromBytes(BigDecimal amount) {
        return fromBits(amount.multiply(BigDecimal.valueOf(8)));
    }


    public BigDecimal to(double sourceAmount, DataUnit targetUnit) {
        return to(BigDecimal.valueOf(sourceAmount), targetUnit);
    }
    public BigDecimal to(BigDecimal sourceAmount, DataUnit targetUnit) {
        Objects.requireNonNull(sourceAmount,
                "'sourceAmount' must not be null.");
        Objects.requireNonNull(targetUnit,
                "'targetUnit' must not be null.");
        return bits.multiply(sourceAmount).divide(targetUnit.bits);
    }

    /**
     * Converts an amount of a specific unit to this unit.
     * @param sourceAmount source amount to convert
     * @param sourceUnit source unit to convert
     * @return converted value
     */
    public BigDecimal from(double sourceAmount, DataUnit sourceUnit) {
        return from(BigDecimal.valueOf(sourceAmount), sourceUnit);
    }
    /**
     * Converts an amount of a specific unit to this unit.
     * @param sourceAmount source amount to convert
     * @param sourceUnit source unit to convert
     * @return converted value
     */
    public BigDecimal from(BigDecimal sourceAmount, DataUnit sourceUnit) {
        Objects.requireNonNull(sourceAmount,
                "'sourceAmount' must not be null.");
        Objects.requireNonNull(sourceUnit,
                "'sourceUnit' must not be null.");
        return sourceUnit.bits.multiply(sourceAmount).divide(bits);
    }

    /**
     * Gets the unit value representing the given text. Both symbols or
     * full names are supported (English or French,
     * case-insensitive, ignoring accents).
     * @param dataUnit the textual representation of the unit.
     * @return DataUnit instance or <code>null</code> if no match.
     */
    public static DataUnit from(String dataUnit) {
        String txt = StringUtils.trimToNull(dataUnit);
        if (txt == null) {
            return null;
        }

        // normalize (remove accents, plural, and translate)
        txt = StringUtils.stripEnd(txt, "s");
        txt = StringUtils.stripAccents(txt);
        txt = StringUtils.replaceIgnoreCase(txt, "octet", "byte");

        // compare
        for (DataUnit unit : values()) {
            if (unit.getSymbol().equalsIgnoreCase(txt)) {
                return unit;
            }
            if (unit.getName().equalsIgnoreCase(txt)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * Converts a given source data amount and type to this type.
     * @param sourceAmount source data amount
     * @param sourceUnit source data unit
     * @return converted value
     * @deprecated Since 2.0.0, use {@link #from(BigDecimal, DataUnit)}
     */
    @Deprecated
    public long convert(long sourceAmount, DataUnit sourceUnit) {
        return from(BigDecimal.valueOf(sourceAmount), sourceUnit).longValue();
    }
    @Deprecated
    public long toKilobytes(long amount) {
        return to(amount, KB).longValue();
    }
    @Deprecated
    public long toMegabytes(long amount) {
        return to(amount, MB).longValue();
    }
    @Deprecated
    public long toGigabytes(long amount) {
        return to(amount, GB).longValue();
    }
    @Deprecated
    public long toTerabytes(long amount) {
        return to(amount, TB).longValue();
    }
    @Deprecated
    public long toPetabytes(long amount) {
        return to(amount, PB).longValue();
    }

    @Override
    public String toString() {
        return symbol;
    }
}
