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
package com.norconex.commons.lang.xml;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.awt.Dimension;
import java.io.File;
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

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.convert.CharsetConverter;
import com.norconex.commons.lang.convert.ContentTypeConverter;
import com.norconex.commons.lang.convert.DimensionConverter;
import com.norconex.commons.lang.convert.DurationConverter;
import com.norconex.commons.lang.convert.FileConverter;
import com.norconex.commons.lang.convert.InstantConverter;
import com.norconex.commons.lang.convert.LocalDateTimeConverter;
import com.norconex.commons.lang.convert.LocaleConverter;
import com.norconex.commons.lang.convert.PathConverter;
import com.norconex.commons.lang.convert.PatternConverter;
import com.norconex.commons.lang.convert.ZonedDateTimeConverter;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.img.MutableImage;
import com.norconex.commons.lang.img.MutableImage.Quality;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Data;
import lombok.EqualsAndHashCode;

class XmlAdaptersTest {

    @Test
    void testXMLAdapters() throws MalformedURLException {

        var obj = new JaxbComplexTypes();
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

        obj.setFromXMLConfig("am I here?");

        assertThatNoException().isThrownBy(() -> {
            Xml.assertWriteRead(obj, "jaxbComplexTypes");
        });
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class JaxbComplexTypes implements XmlConfigurable {
        @XmlJavaTypeAdapter(CharsetConverter.XmlAdapter.class)
        private Charset charset;
        @XmlJavaTypeAdapter(ContentTypeConverter.XmlAdapter.class)
        private ContentType contentType;
        @XmlJavaTypeAdapter(DimensionConverter.XmlAdapter.class)
        private Dimension dimension;
        @XmlJavaTypeAdapter(DurationConverter.XmlAdapter.class)
        private Duration duration;
        @XmlJavaTypeAdapter(FileConverter.XmlAdapter.class)
        private File file;
        @XmlJavaTypeAdapter(InstantConverter.XmlAdapter.class)
        private Instant instant;
        @XmlJavaTypeAdapter(LocalDateTimeConverter.XmlAdapter.class)
        private LocalDateTime localDateTime;
        @XmlJavaTypeAdapter(LocaleConverter.XmlAdapter.class)
        private Locale locale;
        @XmlJavaTypeAdapter(PathConverter.XmlAdapter.class)
        private Path path;
        @XmlJavaTypeAdapter(PatternConverter.XmlAdapter.class)
        @EqualsAndHashCode.Exclude
        private Pattern pattern;
        @XmlJavaTypeAdapter(ZonedDateTimeConverter.XmlAdapter.class)
        private ZonedDateTime zonedDateTime;

        // don't need adapters
        private boolean booleanValue;
        private Character character;
        private Class<?> classObj;
        private Date date;
        private MutableImage.Quality enumObj;
        private int intValue;
        private float floatValue;
        private byte byteValue;
        private BigDecimal bigDecimal;
        private String string;
        private URL url;

        @XmlTransient
        private String fromXMLConfig;

        @EqualsAndHashCode.Include(replaces = "pattern")
        public String getPatternStr() {
            return pattern.toString();
        }

        @Override
        public void loadFromXML(Xml xml) {
            setFromXMLConfig(xml.getString("fromXMLConfig", "Not there."));
        }

        @Override
        public void saveToXML(Xml xml) {
            xml.addElement("fromXMLConfig", fromXMLConfig);
        }
    }
}
