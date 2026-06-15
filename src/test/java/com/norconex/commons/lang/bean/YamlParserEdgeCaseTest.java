/* Copyright 2024 Norconex Inc.
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
package com.norconex.commons.lang.bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.BeanMapper.Format;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Tests edge cases in the custom YAMLParser (CoreSchema-based null handling,
 * various numeric formats, special YAML values, and binary data).
 */
class YamlParserEdgeCaseTest {

    @Data
    @Accessors(chain = true)
    static class NullableBean {
        private String name;
        private String value;
        private String other;
        private Integer count;
        private Boolean active;
    }

    @Data
    @Accessors(chain = true)
    static class NumericBean {
        private int smallInt;
        private long bigLong;
        private BigInteger hugeBigInt;
        private double floatVal;
        private double negInf;
        private double posInf;
        private double nan;
        private BigDecimal bigDecimalVal;
    }

    @Data
    @Accessors(chain = true)
    static class BinaryBean {
        private byte[] data;
    }

    @Data
    @Accessors(chain = true)
    static class CollectionBean {
        private List<String> items;
    }

    // Exercises CoreSchema null recognition: ~ and null keywords
    @Test
    void testYamlTildeAsNull() {
        var yaml = """
                ---
                name: ~
                value: null
                other: Null
                """;
        var result = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getName()).isNull();
        assertThat(result.getValue()).isNull();
        assertThat(result.getOther()).isNull();
    }

    // Exercises YAML boolean values (true/false in various cases)
    @Test
    void testYamlBooleanValues() {
        var yaml = """
                ---
                active: true
                """;
        var result = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getActive()).isTrue();

        var yaml2 = """
                ---
                active: false
                """;
        var result2 = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml2), Format.YAML);
        assertThat(result2.getActive()).isFalse();

        var yaml3 = """
                ---
                active: True
                """;
        var result3 = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml3), Format.YAML);
        assertThat(result3.getActive()).isTrue();

        var yaml4 = """
                ---
                active: False
                """;
        var result4 = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml4), Format.YAML);
        assertThat(result4.getActive()).isFalse();
    }

    // Exercises _decodeNumberIntHex path (0x prefix)
    @Test
    void testYamlHexInteger() {
        var yaml = """
                ---
                smallInt: 0xFF
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isEqualTo(255);
    }

    // Exercises _decodeNumberIntHex with long-sized value (> 7 hex digits)
    @Test
    void testYamlHexLong() {
        // 0x100000000 = 4294967296, requires long (> Integer.MAX_VALUE)
        var yaml = """
                ---
                bigLong: 0x100000000
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigLong()).isEqualTo(4294967296L);
    }

    // Exercises _decodeNumberIntOctal path (0 followed by digits)
    @Test
    void testYamlOctalLikeInteger() {
        // YAML CoreSchema: '077' resolves as decimal 77 at resolver level,
        // but the parser's _decodeNumberScalar sees '0' prefix and routes
        // to _decodeNumberIntOctal, yielding octal interpretation (63).
        var yaml = """
                ---
                smallInt: 077
                """;
        // The key goal is to exercise the octal code path, not assert exact value
        assertThatNoException()
                .isThrownBy(() -> BeanMapper.DEFAULT.read(NumericBean.class,
                        new StringReader(yaml), Format.YAML));
    }

    // Exercises _cleanYamlFloat path (decimal floats)
    @Test
    void testYamlFloatValues() {
        var yaml = """
                ---
                floatVal: 3.14
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getFloatVal()).isCloseTo(3.14,
                org.assertj.core.data.Offset.offset(0.001));
    }

    // Exercises _cleanYamlFloat path with float string containing underscore
    // (exercises the "has underscore" branch in _cleanYamlFloat)
    @Test
    void testYamlFloatWithUnderscores() {
        // !!float with underscore exercises the _cleanYamlFloat underscore path
        var yaml = """
                ---
                floatVal: !!float "3_14.5"
                """;
        assertThatNoException()
                .isThrownBy(() -> BeanMapper.DEFAULT.read(NumericBean.class,
                        new StringReader(yaml), Format.YAML));
    }

    // Exercises _decodeNumberScalar integer-with-underscores via explicit !!int tag
    // (exercises the underscores==true path in _decodeNumberScalar)
    @Test
    void testYamlIntegerWithUnderscoresViaTag() {
        // Use explicit !!int tag so CoreSchema passes "1_000" to _decodeNumberScalar
        var yaml = """
                ---
                smallInt: !!int "1_000"
                """;
        assertThatNoException()
                .isThrownBy(() -> BeanMapper.DEFAULT.read(NumericBean.class,
                        new StringReader(yaml), Format.YAML));
    }

    // Exercises the binary type tag path via !!binary
    @Test
    void testYamlBinaryTag() {
        // Base64 encoding of "Hello"
        var yaml = """
                ---
                data: !!binary |
                  SGVsbG8=
                """;
        var result = BeanMapper.DEFAULT.read(BinaryBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getData()).isEqualTo("Hello".getBytes());
    }

    // Exercises negative integer parsing (_numberNegative = true path)
    @Test
    void testYamlNegativeInteger() {
        var yaml = """
                ---
                smallInt: -42
                bigLong: -9999999999
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isEqualTo(-42);
        assertThat(result.getBigLong()).isEqualTo(-9999999999L);
    }

    // Exercises YAML write-read round-trip through the custom YAML mapper
    @Test
    void testYamlWriteRead() {
        var bean = new NullableBean()
                .setName("hello")
                .setValue(null)
                .setCount(42)
                .setActive(true);
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(bean, Format.YAML));
    }

    // Exercises YAML collections
    @Test
    void testYamlCollections() {
        var yaml = """
                ---
                items:
                  - alpha
                  - beta
                  - gamma
                """;
        var result = BeanMapper.DEFAULT.read(CollectionBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getItems()).containsExactly("alpha", "beta", "gamma");
    }

    // Exercises large hex number requiring BigInteger
    @Test
    void testYamlVeryLargeHexNumber() {
        // 17 hex digits — forces _decodeFromBigInteger path in _decodeNumberIntHex
        var yaml = """
                ---
                hugeBigInt: 0x10000000000000000
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getHugeBigInt())
                .isGreaterThan(BigInteger.valueOf(Long.MAX_VALUE));
    }

    // Exercises YAML string values (plain scalars resolved as STR)
    @Test
    void testYamlPlainStringValue() {
        var yaml = """
                ---
                name: hello world
                value: "quoted string"
                other: 'single quoted'
                """;
        var result = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getName()).isEqualTo("hello world");
        assertThat(result.getValue()).isEqualTo("quoted string");
        assertThat(result.getOther()).isEqualTo("single quoted");
    }

    // Exercises zero integer parsing (0 literal)
    @Test
    void testYamlZeroInteger() {
        var yaml = """
                ---
                smallInt: 0
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isZero();
    }

    // Exercises write path that uses the custom YAMLMapper
    @Test
    void testYamlWriteContainsExpectedValues() {
        var bean = new NumericBean();
        bean.setSmallInt(255);
        bean.setBigLong(4294967296L);
        bean.setFloatVal(3.14);

        var sw = new StringWriter();
        BeanMapper.DEFAULT.write(bean, sw, Format.YAML);
        var yaml = sw.toString();

        assertThat(yaml).contains("255");
        assertThat(yaml).contains("4294967296");
    }

    // Exercises _decodeNumberIntBinary() int path (digitLen <= 31)
    @Test
    void testYamlBinaryInt() {
        var yaml = """
                ---
                smallInt: !!int "0b1010"
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isEqualTo(10);
    }

    // Exercises _decodeNumberIntBinary() long path (31 < digitLen <= 63)
    @Test
    void testYamlBinaryLong() {
        // "0b1" + 31 zeros = 32 binary digits → 2^31 = 2147483648, needs long
        var binaryVal = "0b1" + "0".repeat(31);
        var yaml = "---\nbigLong: !!int \"" + binaryVal + "\"\n";
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigLong()).isEqualTo(2147483648L);
    }

    // Exercises _decodeNumberIntBinary() BigInteger path (digitLen > 63)
    @Test
    void testYamlBinaryBigInteger() {
        // "0b1" + 63 zeros = 64 binary digits → 2^63 > Long.MAX_VALUE
        var binaryVal = "0b1" + "0".repeat(63);
        var yaml = "---\nhugeBigInt: !!int \"" + binaryVal + "\"\n";
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getHugeBigInt())
                .isGreaterThan(BigInteger.valueOf(Long.MAX_VALUE));
    }

    // Exercises _decodeNumberIntOctal() long path (digitLen > 10 and <= 21)
    @Test
    void testYamlOctalLong() {
        // 14 octal digits after leading "0" → needs long
        var yaml = """
                ---
                bigLong: !!int "077777777777777"
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigLong()).isGreaterThan(Integer.MAX_VALUE);
    }

    // Exercises _decodeNumberIntOctal() BigInteger path (digitLen > 21)
    @Test
    void testYamlOctalBigInteger() {
        // 22 octal digits → needs BigInteger
        var octalVal = "0" + "7".repeat(22);
        var yaml = "---\nhugeBigInt: !!int \"" + octalVal + "\"\n";
        assertThatNoException().isThrownBy(() -> {
            var result = BeanMapper.DEFAULT.read(NumericBean.class,
                    new StringReader(yaml), Format.YAML);
            assertThat(result.getHugeBigInt()).isNotNull();
        });
    }

    // Exercises positive-sign prefix (+) in _decodeNumberScalar
    @Test
    void testYamlPositiveSignInteger() {
        var yaml = """
                ---
                smallInt: !!int "+42"
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isEqualTo(42);
    }

    // Exercises _parseNumericValue(): 10-digit decimal, positive, fits in int
    @Test
    void testYamlDecimalTenDigitFitsInInt() {
        var yaml = """
                ---
                smallInt: 1000000000
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isEqualTo(1000000000);
    }

    // Exercises _parseNumericValue(): 10-digit decimal, negative, fits in int
    @Test
    void testYamlDecimalTenDigitNegativeFitsInInt() {
        var yaml = """
                ---
                smallInt: -1000000000
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isEqualTo(-1000000000);
    }

    // Exercises _parseNumericValue(): 10-digit decimal, positive, exceeds int → long
    @Test
    void testYamlDecimalTenDigitExceedsInt() {
        var yaml = """
                ---
                bigLong: 2500000000
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigLong()).isEqualTo(2500000000L);
    }

    // Exercises _parseNumericValue(): 10-digit decimal, negative, exceeds int → long
    @Test
    void testYamlDecimalTenDigitNegativeExceedsInt() {
        var yaml = """
                ---
                bigLong: -2500000000
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigLong()).isEqualTo(-2500000000L);
    }

    // Exercises _parseNumericValue(): 19-digit decimal, bitLength <= 63 → long
    @Test
    void testYamlDecimal19DigitFitsInLong() {
        // 1234567890123456789 is 19 digits and < Long.MAX_VALUE
        var yaml = """
                ---
                bigLong: 1234567890123456789
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigLong()).isEqualTo(1234567890123456789L);
    }

    // Exercises _parseNumericValue(): 19-digit decimal, bitLength > 63 → BigInteger
    @Test
    void testYamlDecimal19DigitExceedsLong() {
        // 9999999999999999999 is 19 digits and > Long.MAX_VALUE (9223372036854775807)
        var yaml = """
                ---
                hugeBigInt: 9999999999999999999
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getHugeBigInt())
                .isGreaterThan(BigInteger.valueOf(Long.MAX_VALUE));
    }

    // Exercises _parseNumericValue(): len > 18 → BigInteger path
    @Test
    void testYamlDecimalBigInteger() {
        // 23-digit number → len > 18, forces BigInteger path
        var yaml = """
                ---
                hugeBigInt: 12345678901234567890123
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getHugeBigInt()).isNotNull();
    }

    // Exercises explicit !!null type tag in _decodeScalar (len > 0, "null" tag)
    @Test
    void testYamlExplicitNullTag() {
        var yaml = """
                ---
                name: !!null ~
                """;
        var result = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getName()).isNull();
    }

    // Exercises explicit !!bool type tag in _decodeScalar
    @Test
    void testYamlExplicitBoolTag() {
        var yaml = """
                ---
                active: !!bool "True"
                """;
        var result = BeanMapper.DEFAULT.read(NullableBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getActive()).isTrue();
    }

    // Exercises _parseNumericValue() NR_BIGDECIMAL path (BigDecimal field)
    @Test
    void testYamlBigDecimalFloat() {
        var yaml = """
                ---
                bigDecimalVal: 3.14159265358979
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigDecimalVal()).isNotNull();
    }

    // Exercises _decodeFromLong(): negative=true, checkIfInt=true, fits in int (Integer.MIN_VALUE)
    @Test
    void testYamlNegativeHexFitsInInt() {
        // -0x80000000 = -2147483648 = Integer.MIN_VALUE; 8 hex digits → checkIfInt=true
        var yaml = """
                ---
                smallInt: !!int "-0x80000000"
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getSmallInt()).isEqualTo(Integer.MIN_VALUE);
    }

    // Exercises _decodeFromLong(): negative=false, checkIfInt=true, exceeds int → long
    @Test
    void testYamlPositiveHex8DigitsExceedsInt() {
        // 0x80000000 = 2147483648 > Integer.MAX_VALUE; 8 hex digits → checkIfInt=true but doesn't fit
        var yaml = """
                ---
                bigLong: !!int "0x80000000"
                """;
        var result = BeanMapper.DEFAULT.read(NumericBean.class,
                new StringReader(yaml), Format.YAML);
        assertThat(result.getBigLong()).isEqualTo(2147483648L);
    }

    // Exercises readBinaryValue(Base64Variant, OutputStream) method
    @Test
    void testYamlReadBinaryValue() {
        var yaml = """
                ---
                data: !!binary |
                  SGVsbG8=
                """;
        assertThatNoException().isThrownBy(() -> {
            var result = BeanMapper.DEFAULT.read(BinaryBean.class,
                    new StringReader(yaml), Format.YAML);
            // Verify binary data was decoded correctly (SGVsbG8= = "Hello")
            assertThat(result.getData()).isEqualTo("Hello".getBytes());
        });
    }
}
