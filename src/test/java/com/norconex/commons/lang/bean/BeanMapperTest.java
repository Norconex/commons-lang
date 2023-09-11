/* Copyright 2023 Norconex Inc.
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

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.io.input.BrokenReader;
import org.apache.commons.io.output.BrokenWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.norconex.commons.lang.Sleeper;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.bean.stubs.Automobile;
import com.norconex.commons.lang.bean.stubs.Plane;
import com.norconex.commons.lang.bean.stubs.PlaneConfig.Type;
import com.norconex.commons.lang.bean.stubs.TestConfig;
import com.norconex.commons.lang.bean.stubs.Transportation;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.img.MutableImage;
import com.norconex.commons.lang.img.MutableImage.Quality;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

class BeanMapperTest {

    @Test
    void testAssertWriteRead() throws MalformedURLException {
        var obj = new MultiTypes();
        obj.setPath(Path.of("/tmp/somepath.txt"));

        obj.setBigDecimal(BigDecimal.valueOf(123.456));
        obj.setBooleanValue(true);
        obj.setByteValue((byte) 123);
        obj.setCharacter('z');
        obj.setCharset(UTF_16BE);
        obj.setClassObj(MutableImage.Quality.class);
        obj.setContentType(ContentType.ZIP);
        obj.setDate(new Date());
        obj.setDimension(new java.awt.Dimension(640, 480));
        obj.setDuration(Duration.ofDays(3));
        obj.setEnumObj(Quality.HIGH);
        obj.setFile(new File("/tmp/somefile.txt"));
        obj.setFloatValue(234.567f);
        obj.setInstant(Instant.now());
        obj.setIntValue(567);
        obj.setLocalDateTime(LocalDateTime.now());
        obj.setLocale(Locale.CANADA_FRENCH);
        obj.setPath(Path.of("/tmp/somepath.txt"));
        obj.setPattern(Pattern.compile(".*potato.*"));
        obj.setString("some string");
        obj.setUrl(new URL("http://example.com"));
        obj.setZonedDateTime(ZonedDateTime.now());

        assertThatNoException().isThrownBy(() -> {
            BeanMapper.DEFAULT.assertWriteRead(obj);
        });
    }

    @Data
    public static class MultiTypes {
        private boolean booleanValue;
        private Character character;
        private Class<?> classObj;
        private Date date;
        private MutableImage.Quality enumObj;
        @Min(0)
        private int intValue;
        private float floatValue;
        private byte byteValue;
        private BigDecimal bigDecimal;
        private String string;
        private URL url;

        private Charset charset;
        private ContentType contentType;
        private Dimension dimension;
        private Duration duration;
        private File file;
        private Instant instant;
        private LocalDateTime localDateTime;
        private Locale locale;
        private Path path;
        @EqualsAndHashCode.Exclude
        private Pattern pattern;
        private ZonedDateTime zonedDateTime;

        @EqualsAndHashCode.Include(replaces = "pattern")
        @JsonIgnore
        public String getPatternStr() {
            return pattern.toString();
        }
    }

    @Test
    void testExceptions() {
        assertThatException().isThrownBy(() ->//NOSONAR
            BeanMapper.DEFAULT.write(
                    new MultiTypes(), BrokenWriter.INSTANCE, Format.XML)
        );
        assertThatException().isThrownBy(() ->
            BeanMapper.DEFAULT.read(
                    MultiTypes.class, BrokenReader.INSTANCE, Format.XML)
        );
        assertThatException().isThrownBy(() ->//NOSONAR
            BeanMapper.DEFAULT.read(
                    new MultiTypes(), BrokenReader.INSTANCE, Format.XML)
        );

        var obj = new MultiTypes();
        obj.setIntValue(-200);
        assertThatExceptionOfType(
                ConstraintViolationException.class).isThrownBy(() ->//NOSONAR
            BeanMapper.DEFAULT.assertWriteRead(obj)
        );

        assertThatException().isThrownBy(() ->//NOSONAR
            BeanMapper.DEFAULT.assertWriteRead(new NotEqual())
        );

    }

    @Data
    public static class NotEqual{
        long timestamp;
        public long getTimestamp() {
            Sleeper.sleepMillis(1);
            return System.currentTimeMillis();
        }
    }

    //--- Polymorphic tests ----------------------------------------------------

    @RequiredArgsConstructor
    enum Source {
        XML("""
            <TestConfig id="my config">
              <transportations>
                <transportation class="Automobile">
                  <configuration>
                    <make>Toyota</make>
                    <model>Camry</model>
                    <year>1800</year>
                  </configuration>
                </transportation>
                <transportation class="Plane">
                  <configuration>
                    <name>Boeing 737</name>
                    <type>COMMERCIAL</type>
                  </configuration>
                </transportation>
              </transportations>
            </TestConfig>
            """,
            Format.XML
        ),
        JSON("""
            {
              "id" : "my config",
              "transportations" : [ {
                "class" : "Automobile",
                "configuration" : {
                  "make" : "Toyota",
                  "model" : "Camry",
                  "year" : 1800
                }
              }, {
                "class" : "Plane",
                "configuration" : {
                  "name" : "Boeing 737",
                  "type" : "COMMERCIAL"
                }
              } ]
            }""",
            Format.JSON
        ),
        YAML("""
            ---
            id: "my config"
            transportations:
            - class: "Automobile"
              configuration:
                make: "Toyota"
                model: "Camry"
                year: 1800
            - class: "Plane"
              configuration:
                name: "Boeing 737"
                type: "COMMERCIAL"
            """,
            Format.YAML
        )
        ;
        final String source;
        final Format format;
    }

    @ParameterizedTest
    @EnumSource(value = Source.class)
    void testPolymorphicTypes(Source source)
            throws IOException {

        // Expected
        var expected = new TestConfig();
        expected.setId("my config");

        var automobile = new Automobile();
        automobile.getConfiguration()
            .setModel("Camry")
            .setYear(1800)
            .setBrand("Toyota");
        expected.getTransportations().add(automobile);

        var plane = new Plane();
        plane.getConfiguration()
            .setName("Boeing 737")
            .setType(Type.COMMERCIAL);
        expected.getTransportations().add(plane);


        // Mapper
        var builder = BeanMapper.builder()
            .skipValidation(true)
            .polymorphicType(Transportation.class,
                    name -> name.startsWith("com.norconex."))
            ;
        var cfg = builder.build().read(
                TestConfig.class,
                new StringReader(source.source),
                source.format);

        // Test write/read
        assertThat(cfg).isEqualTo(expected);
        builder.build().assertWriteRead(cfg, source.format);

        // Test write properly
        var out = new StringWriter();
        builder.indent(true).build().write(cfg, out, source.format);
        assertThat(out.toString()).isEqualToNormalizingNewlines(source.source);

        // Test validates properly
        builder.skipValidation(false);
        assertThatExceptionOfType(
                ConstraintViolationException.class).isThrownBy(() -> {//NOSONAR
            builder.build().read(
                    TestConfig.class,
                    new StringReader(source.source),
                    source.format);
        });
    }
}
