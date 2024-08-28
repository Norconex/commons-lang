/* Copyright 2022-2023 Norconex Inc.
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

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.File;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.mock.MockProxySettings;

import lombok.AllArgsConstructor;
import lombok.Data;

class EnhancedXMLStreamWriterTest {

    @Test
    void testMiscAccessors() {
        var xml = newXMLWriter();

        xml.setPrefix("prefix", "uri");
        assertThat(xml.getPrefix("uri")).isEqualTo("prefix");

        assertThatNoException().isThrownBy(() -> {
            var x = newXMLWriter();
            x.setDefaultNamespace("uri");
            x.setNamespaceContext(null);
            x.getNamespaceContext();
        });
    }

    @Test
    void testWriteStartDocument() {
        var xml = newXMLWriter();
        xml.writeStartDocument();
        xml.writeEmptyElement("test");
        xml.writeEndDocument();
        assertThat(xml.getWriter()).hasToString(
                "<?xml version='1.0' encoding='UTF-8'?><test/>");

        xml = newXMLWriter();
        xml.writeStartDocument("1.1");
        xml.writeEmptyElement("test");
        xml.writeEndDocument();
        assertThat(xml.getWriter()).hasToString("<?xml version='1.1'?><test/>");

        xml = newXMLWriter();
        xml.writeStartDocument(UTF_16.toString(), "1.1");
        xml.writeEmptyElement("test");
        xml.writeEndDocument();
        assertThat(xml.getWriter())
                .hasToString("<?xml version='1.1' encoding='UTF-16'?><test/>");

        EnhancedXMLStreamWriter badWriter =
                new EnhancedXMLStreamWriter(new StringWriter()) {
                    @Override
                    public void writeStartDocument() {
                        throw new XMLException("fake start doc");
                    }

                    @Override
                    public void writeStartDocument(String version) {
                        throw new XMLException("fake start doc");
                    }

                    @Override
                    public void writeStartDocument(String encoding,
                            String version) {
                        throw new XMLException("fake start doc");
                    }
                };
        assertThatExceptionOfType(XMLException.class).isThrownBy(() -> {
            badWriter.writeStartDocument();
        });
        assertThatExceptionOfType(XMLException.class).isThrownBy(() -> {
            badWriter.writeStartDocument("version");
        });
        assertThatExceptionOfType(XMLException.class).isThrownBy(() -> {
            badWriter.writeStartDocument("encoding", "version");
        });
    }

    @Test
    void testWriteMisc() {
        var xml = newXMLWriter();
        xml.writeComment("A comment");
        xml.writeProcessingInstruction("target");
        xml.writeProcessingInstruction("target", "data");
        xml.writeCData("cdata sample");

        assertThat(xml.getWriter()).hasToString(
                """
                    <!--A comment-->\
                    <?target?>\
                    <?target data?>\
                    <![CDATA[cdata sample]]>""");

        assertThatNoException().isThrownBy(() -> { //NOSONAR
            var xml2 = newXMLWriter();
            xml2.writeStartDocument();
            xml2.writeStartElement("test");
            xml2.writeAttribute("localName", "value");
            xml2.writeAttribute("prefix", "nsURI", "localName", "value");
            xml2.writeNamespace("prefix", "nsURI");
            xml2.writeDefaultNamespace("nsURI");
            xml2.writeEndElement();
            xml2.writeEndDocument();
        });

        assertThatExceptionOfType(XMLException.class)
                .isThrownBy(() -> {//NOSONAR
                    var xml3 = newXMLWriter();
                    xml3.writeStartDocument();
                    xml3.writeStartElement("test");
                    xml3.writeAttribute("nsURI", "localName", "value");
                    xml3.writeEndElement();
                    xml3.writeEndDocument();
                });
    }

    @Test
    void testWriteStartElements() {
        var xml = newXMLWriter();
        xml.writeStartElement("test");
        xml.writeEndElement();
        xml.writeStartElement("test", Duration.class);
        xml.writeEndElement();
        xml.writeStartElement("test", Duration.class, true);
        xml.writeEndElement();
        xml.writeStartElement("prefix", "test", "uri");
        xml.writeEndElement();

        assertThat(xml.getWriter()).hasToString(
                """
                    <test/>\
                    <test class="java.time.Duration"/>\
                    <test class="java.time.Duration" disabled="true"/>\
                    <prefix:test/>""");

        assertThatException().isThrownBy(
                () -> xml.writeStartElement("uri", "localName"));
    }

    @Test
    void testWriteEmptyElements() {
        var xml = newXMLWriter();
        xml.writeStartDocument();
        xml.writeEmptyElement("test");
        xml.writeEmptyElement("prefix", "localName", "nsURL");
        xml.writeEndDocument();
        xml.flush();

        assertThat(xml.getWriter()).hasToString(
                """
                <?xml version='1.0' encoding='UTF-8'?>\
                <test/>\
                <prefix:localName/>""");

        assertThatException().isThrownBy(
                () -> xml.writeStartElement("uri", "localName"));
        xml.close();
    }

    @Test
    void testEnhancedXMLStreamWriter() {
        var xml =
                new EnhancedXMLStreamWriter(new StringWriter(), true);

        xml.writeStartDocument();
        xml.writeStartElement("proxySettings", MockProxySettings.class);

        xml.writeStartElement("host");
        xml.writeElementString("name", "example.com");
        xml.writeElementInteger("port", 123);
        xml.writeEndElement();

        xml.writeElementString("scheme", "https");
        xml.writeElementString("realm", "Cinderella");

        xml.writeStartElement("credentials");
        xml.writeElementString("username", "joe");
        xml.writeElementString("password", "nottelling");
        xml.writeElementString("passwordKey", null);
        xml.writeEndElement();

        xml.writeEndElement();
        xml.writeEndDocument();

        xml.flush();

        assertThat(xml.getWriter()).hasToString(
                "<?xml version='1.0' encoding='UTF-8'?>"
                        + XMLTest.SAMPLE_PROXYSETTINGS_XML);
    }

    @Test
    void testElementTypes() {
        var xml = newXMLWriter();

        xml.writeStartDocument();

        xml.writeElementInteger("int", 1);
        xml.writeElementLong("long", 2L);
        xml.writeElementFloat("float", 3F);
        xml.writeElementDouble("double", 4D);
        xml.writeElementBoolean("boolean", true);
        xml.writeElementClass("class", Duration.class);
        xml.writeElementClass("class", null, false);
        xml.writeElementFile("file", new File("blah"));
        xml.writeElementDimension("dimension", null);
        xml.writeElementDisabled("disabled");
        xml.writeElementDisabled("disabled", Duration.class);
        xml.writeElementDelimited("delimited", Arrays.asList(5, 6));
        xml.writeElementObject("object", "7");
        xml.writeElementObjectList(
                "objectList", "object", Arrays.asList(8, 9));
        xml.writeElementObjectList("objectList", "object",
                Collections.emptyList(), true);

        xml.writeEndDocument();

        assertThat(xml.getWriter()).hasToString(
                """
                    <?xml version='1.0' encoding='UTF-8'?>\
                    <int>1</int>\
                    <long>2</long>\
                    <float>3.0</float>\
                    <double>4.0</double>\
                    <boolean>true</boolean>\
                    <class>java.time.Duration</class>\
                    <file>blah</file>\
                    <disabled disabled="true"/>\
                    <disabled class="java.time.Duration" \
                    disabled="true"/>\
                    <delimited>5,6</delimited>\
                    <object>7</object>\
                    <objectList>\
                    <object>8</object>\
                    <object>9</object>\
                    </objectList>\
                    <objectList/>""");
    }

    @Test
    void testAttributeTypes() {
        var xml = newXMLWriter();

        xml.writeStartDocument();
        xml.writeStartElement("test");

        xml.writeAttributeInteger("int", 1);
        xml.writeAttributeLong("long", 2L);
        xml.writeAttributeFloat("float", 3F);
        xml.writeAttributeDouble("double", 4D);
        xml.writeAttributeBoolean("boolean", true);
        xml.writeAttributeClass("class", Duration.class);
        xml.writeAttributeClass("class", null, false);
        xml.writeAttributeString("string", "5");
        xml.writeAttributeObject("object", "6");
        xml.writeAttributeObject("object", null, true);
        xml.writeAttributeDelimited("delimited", Arrays.asList(7, 8));
        xml.writeAttributeDisabled();
        xml.writeAttributeDisabled(true);
        xml.writeAttributeDisabled(true, true);

        xml.writeEndElement();
        xml.writeEndDocument();
        xml.flush();

        assertThat(xml.getWriter()).hasToString(
                """
                    <?xml version='1.0' encoding='UTF-8'?>\
                    <test\s\
                    int="1" \
                    long="2" \
                    float="3.0" \
                    double="4.0" \
                    boolean="true" \
                    class="java.time.Duration" \
                    string="5" \
                    object="6" \
                    object="" \
                    delimited="7,8" \
                    disabled="true" \
                    disabled="true" \
                    disabled="true"\
                    />""");
    }

    @Test
    void testBlanksIndent() {
        var xml =
                new EnhancedXMLStreamWriter(new StringWriter(), true, 2);
        xml.writeStartDocument();
        xml.writeStartElement("test");
        xml.writeElementString("value1", " a ");
        xml.writeElementString("value2", null);
        xml.writeEndElement();
        xml.writeEndDocument();
        xml.flush();

        assertThat(xml.getWriter()).hasToString(
                """
                <?xml version='1.0' encoding='UTF-8'?>
                <test>
                  <value1> a </value1>
                  <value2/>
                </test>""");
    }

    @Data
    @AllArgsConstructor
    private static class Configurable implements XMLConfigurable {
        private String value;

        @Override
        public void loadFromXML(XML xml) {
            value = xml.getString(".");
        }

        @Override
        public void saveToXML(XML xml) {
            xml.setTextContent(value);
        }
    }

    private static EnhancedXMLStreamWriter newXMLWriter() {
        return new EnhancedXMLStreamWriter(new StringWriter());
    }
}
