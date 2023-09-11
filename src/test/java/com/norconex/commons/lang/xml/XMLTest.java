/* Copyright 2018-2023 Norconex Inc.
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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.Converter;
import com.norconex.commons.lang.convert.ConverterException;
import com.norconex.commons.lang.convert.DateConverter;
import com.norconex.commons.lang.convert.DurationConverter;
import com.norconex.commons.lang.convert.EnumConverter;
import com.norconex.commons.lang.map.MapUtil;
import com.norconex.commons.lang.time.DurationUnit;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.url.HttpURL;
import com.norconex.commons.lang.xml.mock.MockCredentials;
import com.norconex.commons.lang.xml.mock.MockEncryptionKey.Source;
import com.norconex.commons.lang.xml.mock.MockHost;
import com.norconex.commons.lang.xml.mock.MockProxySettings;

import lombok.EqualsAndHashCode;
import lombok.ToString;

class XMLTest {

    static final Logger LOG =
            LoggerFactory.getLogger(XMLTest.class);

    public static final String SAMPLE_XML =
            """
    	<sampleTag add="juice" id="orange">\
    	<nestedA id="na" type="ta">\
    	Blah\
    	</nestedA>\
    	<nestedB id="nb"/>\
    	<nestedC/>\
    	</sampleTag>""";

    static final String SAMPLE_PROXYSETTINGS_XML =
            """
    	<proxySettings\s\
    	class="com.norconex.commons.lang.xml.mock.MockProxySettings">\
    	<host>\
    	<name>example.com</name>\
    	<port>123</port>\
    	</host>\
    	<scheme>https</scheme>\
    	<realm>Cinderella</realm>\
    	<credentials>\
    	<username>joe</username>\
    	<password>nottelling</password>\
    	<passwordKey/>\
    	</credentials>\
    	</proxySettings>""";

    private static final MockCredentials SAMPLE_CREDS_OBJECT =
            new MockCredentials("joe", "nottelling");

    private static final MockProxySettings SAMPLE_PROXY_OBJECT =
            new MockProxySettings()
                .setHost(new MockHost("example.com", 123))
                .setScheme("https")
                .setRealm("Cinderella")
                .setCredentials(SAMPLE_CREDS_OBJECT);

    @TempDir
    private Path tempDir;

    @Test
    void testWrite(@TempDir File tempDir) throws IOException {
        var file = new File(tempDir, "file.xml");

        var xml1 = new XML("<test><value>1</value></test>");
        xml1.write(file);

        var xml2 = new XML(file);
        assertThat(xml1).isEqualTo(xml2);

        var w = new StringWriter();
        xml1.write(w);
        assertThat(w).hasToString(xml1.toString());
    }

    @Test
    void testGetXMLWriter() throws IOException {
        var xml = new XML("<test/>");
        var writer = xml.getXMLWriter();
        writer.append("<value>1</value>");
        writer.close();
        assertThat(xml).hasToString("<test><value>1</value></test>");
    }

    @Test
    void testSetRemoveAttributes() {
        var xml = new XML("<test/>");
        xml.setAttribute("a1", "v1");
        xml.setAttributes(MapUtil.toMap(
                "a2", "v2",
                "a3", "v3"
        ));
        xml.setDelimitedAttributeList("a4", Arrays.asList("v4-1", "v4-2"));
        xml.setDelimitedAttributeList("a5", "|", Arrays.asList("v5-1", "v5-2"));
        assertThat(xml).hasToString("""
        	<test\s\
        	a1="v1" \
        	a2="v2" \
        	a3="v3" \
        	a4="v4-1,v4-2" \
        	a5="v5-1|v5-2"\
        	/>"""
        );
        xml.removeAttribute("a1");
        xml.removeAttribute("a4");
        xml.removeAttribute("a5");
        assertThat(xml).hasToString("""
        	<test\s\
        	a2="v2" \
        	a3="v3"\
        	/>"""
        );
    }

    @Test
    void testAddElementList() {
        var xmlNoParent = new XML("<test/>");
        xmlNoParent.addElementList("value", Arrays.asList(1, 2, 3));
        assertThat(xmlNoParent).hasToString(
                """
                	<test>\
                	<value>1</value>\
                	<value>2</value>\
                	<value>3</value>\
                	</test>"""
        );

        var xmlWithParent = new XML("<test/>");
        xmlWithParent.addElementList("parent", "value", Arrays.asList(1, 2, 3));
        assertThat(xmlWithParent).hasToString(
                """
                	<test>\
                	<parent>\
                	<value>1</value>\
                	<value>2</value>\
                	<value>3</value>\
                	</parent>\
                	</test>"""
        );

        assertThat(new XML("<test/>").addElementList("value", null)).isEmpty();

        var xmlDelimComma = new XML("<test/>");
        xmlDelimComma.addDelimitedElementList("value", Arrays.asList(1, 2, 3));
        assertThat(xmlDelimComma).hasToString(
                "<test><value>1,2,3</value></test>");

        var xmlDelimBar = new XML("<test/>");
        xmlDelimBar.addDelimitedElementList("value", "|", Arrays.asList(5,6));
        assertThat(xmlDelimBar).hasToString("<test><value>5|6</value></test>");

        var xmlDelimEmpty = new XML("<test/>");
        xmlDelimEmpty.addDelimitedElementList(
                "value", "|", Collections.emptyList());
        assertThat(xmlDelimEmpty).hasToString("<test><value></value></test>");
    }

    @Test
    void testGetName() {
        var xml = new XML(
                "<test>"
              +   "<blah>abc</blah>"
              + "</test>"
        );
        assertThat(xml.getName()).isEqualTo("test");
        assertThat(xml.getXML("blah").getName()).isEqualTo("blah");
        assertThat(new XML("").getName()).isNull();
    }

    @Test
    void testGetStringMap() {
        var xml = new XML(
                "<test>"
              +   "<map>"
              +     "<entry><key>k1</key><value>v1</value></entry>"
              +     "<entry><key>k2</key><value>v2</value></entry>"
              +     "<entry><key>k3</key><value>v3</value></entry>"
              +   "</map>"
              + "</test>"
        );
        assertThat(xml.getStringMap("map/entry", "key", "value"))
            .containsAllEntriesOf(MapUtil.toMap(
                    "k1", "v1", "k2", "v2", "k3", "v3"));
        assertThat(xml.getStringMap("nil", "key", "value")).isEmpty();
    }

    @SuppressWarnings("deprecation")
    @Test
    void testJoin() {
        assertThat(new XML("<test/>").join("|", Arrays.asList(1, 2, 3)))
            .isEqualTo("1|2|3");
    }

    @Test
    void testForEachAndStream() {
        var total = new MutableInt();
        new XML("<test><value>1</value><value>3</value></test>")
                .forEach("value", x -> total.add(x.getInteger(".")));
        assertThat(total.intValue()).isEqualTo(4);

        total.setValue(0);
        new XML("<test><value>2</value><value>5</value></test>").forEach(c -> {
            if ("value".equals(c.getName())) {
                total.add(c.readAsXML().getInteger("."));
            }
        });
        assertThat(total.intValue()).isEqualTo(7);

        total.setValue(0);
        new XML("<test><value>6</value><value>7</value></test>")
            .stream()
            .filter(c -> "value".equals(c.getName()))
            .forEach(c -> total.add(c.readAsXML().getInteger(".")));
        assertThat(total.intValue()).isEqualTo(13);
    }

    @Test
    void testHashCode() {
        assertThat(new XML("<test/>"))
            .hasSameHashCodeAs(XML.of("<test/>").create());
    }

    @Test
    void testGetNode() {
        assertThat(new XML("<test/>").getNode().getLocalName()).isNull();
        assertThat(new XML("<test/>")
                .getNode().getNodeName()).isEqualTo("test");
        var xml = new XML(
                "<test>"
              +   "<value>abc</value>"
              +   "<parent><value>def</value></parent>"
              + "</test>");
        assertThat(xml.getNode("value").getTextContent()).isEqualTo("abc");
    }

    @Test
    void testValidate() {
        var errors = new ErrorHandlerCapturer();
        var xml = XML.of(SAMPLE_PROXYSETTINGS_XML.replace(
                "class", "invalid=\"invalid\" class"))
            .setErrorHandler(errors)
            .create();

        errors.clear();
        xml.validate();
        assertThat(errors.getErrors()).hasSize(1);
        assertThat(errors.getErrors().get(0).getMessage()).contains(
                "Attribute 'invalid' is not allowed");

        errors.clear();
        xml.validate(new MockProxySettings());
        assertThat(errors.getErrors()).hasSize(1);
        assertThat(errors.getErrors().get(0).getMessage()).contains(
                "Attribute 'invalid' is not allowed");

        errors.clear();
        xml.validate(MockProxySettings.class);
        assertThat(errors.getErrors()).hasSize(1);
        assertThat(errors.getErrors().get(0).getMessage()).contains(
                "Attribute 'invalid' is not allowed");

        errors.clear();
        xml.validate((Object) null);
        assertThat(errors.getErrors()).isEmpty();

    }

    @Test
    void testGetURL() throws MalformedURLException {
        var xml = XML.of(
                "<test>"
              +   "<single>http://example.com</single>"
              +   "<value>http://example.com/1</value>"
              +   "<value>http://example.com/2</value>"
              +   "<value>http://example.com/3</value>"
              + "</test>"
        ).create();

        assertThat(xml.getURL("single"))
            .isEqualTo(new URL("http://example.com"));
        assertThat(xml.getURL("nil", new URL("http://example.com/4")))
            .isEqualTo(new URL("http://example.com/4"));
        assertThat(xml.getURLList("value")).containsExactly(
                new URL("http://example.com/1"),
                new URL("http://example.com/2"),
                new URL("http://example.com/3"));
        assertThat(xml.getURLList("nil", Arrays.asList(
                new URL("http://example.com/5"))))
            .containsExactly(new URL("http://example.com/5"));
    }

    @Test
    void testGetPathAndFiles() {
        var xml = XML.of(
                "<test>"
              +   "<single>c:\\file.txt</single>"
              +   "<value>/path/1</value>"
              +   "<value>/path/2</value>"
              +   "<value>/path/3</value>"
              + "</test>"
        ).create();

        assertThat(xml.getPath("single")).isEqualTo(Path.of("c:\\file.txt"));
        assertThat(xml.getPath("nil", Path.of("blah")))
            .isEqualTo(Path.of("blah"));
        assertThat(xml.getPathList("value")).containsExactly(
                Path.of("/path/1"), Path.of("/path/2"), Path.of("/path/3"));
        assertThat(xml.getPathList("nil", Arrays.asList(Path.of("/path/4"))))
            .containsExactly(Path.of("/path/4"));

        assertThat(xml.getFile("single")).isEqualTo(new File("c:\\file.txt"));
        assertThat(xml.getFile("nil", new File("blah")))
            .isEqualTo(new File("blah"));
        assertThat(xml.getFileList("value")).containsExactly(
                new File("/path/1"), new File("/path/2"), new File("/path/3"));
        assertThat(xml.getFileList("nil", Arrays.asList(new File("/path/4"))))
            .containsExactly(new File("/path/4"));
    }


    @Test
    void testGetEnum() {
        var xml = XML.of(
                "<test>"
              +   "<single>MINUTE</single>"
              +   "<value>SECOND</value>"
              +   "<value>YEAR</value>"
              +   "<value>DAY</value>"
              +   "<noValues/>"
              +   "<values>WEEK, MONTH, MILLISECOND</values>"
              +   "<dashedValues>YEAR-MONTH-DAY</dashedValues>"
              + "</test>"
        ).create();

        assertThat(xml.getEnum("single", DurationUnit.class))
            .isSameAs(DurationUnit.MINUTE);
        assertThat(xml.getEnum(
                "pathToNil", DurationUnit.class, DurationUnit.HOUR))
            .isSameAs(DurationUnit.HOUR);
        assertThat(xml.getEnumList("value", DurationUnit.class,
                Arrays.asList(DurationUnit.WEEK)))
            .containsExactly(
                    DurationUnit.SECOND,
                    DurationUnit.YEAR,
                    DurationUnit.DAY);
        assertThat(xml.getDelimitedEnumList("values", DurationUnit.class,
                Arrays.asList(DurationUnit.WEEK)))
            .containsExactly(
                    DurationUnit.WEEK,
                    DurationUnit.MONTH,
                    DurationUnit.MILLISECOND);
        assertThat(xml.getDelimitedEnumList("dashedValues", DurationUnit.class,
                "-", Arrays.asList(DurationUnit.WEEK)))
            .containsExactly(
                    DurationUnit.YEAR,
                    DurationUnit.MONTH,
                    DurationUnit.DAY);
    }

    @Test
    void testGetString() {
        var xml = XML.of(
                "<test>"
              +   "<value>aa</value>"
              +   "<value>bb</value>"
              +   "<value>cc</value>"
              +   "<noValues/>"
              +   "<values>x, y, z</values>"
              +   "<dashedValues>d-a-s-h</dashedValues>"
              + "</test>"
        ).create();
        assertThat(xml.getStringList("value")).containsExactly(
                "aa", "bb", "cc");
        assertThat(xml.getStringList("pathToNil", Arrays.asList("dd", "ee")))
            .containsExactly("dd", "ee");
        assertThat(xml.getStringList("noValues")).isEmpty();
        assertThat(xml.getDelimitedStringList("values"))
            .containsExactly("x", "y", "z");
        assertThat(xml.getDelimitedStringList("pathToNil")).isEmpty();
        assertThat(xml.getDelimitedStringList("dashedValues", "-"))
            .containsExactly("d", "a", "s", "h");
        assertThat(xml.getDelimitedStringList("pathToNil", "-")).isEmpty();
        assertThat(xml.getDelimitedStringList(
                "dashedValues", "-", Arrays.asList("a", "b", "c")))
            .containsExactly("d", "a", "s", "h");
        assertThat(xml.getDelimitedStringList(
                "pathToNil", "-", Arrays.asList("a", "b", "c")))
            .containsExactly("a", "b", "c");
        assertThat(xml.getDelimitedStringList(
                "noValues", "-", Arrays.asList("a", "b", "c")))
            .isEmpty();
    }

    @Test
    void testGetXMLList() {
        var xml = XML.of(
                "<test>"
              +   "<values>1, 2, 3</values>"
              +   "<values>4, 5, 6</values>"
              +   "<values>7, 8, 9</values>"
              + "</test>"
        ).create();
        assertThat(xml.getXMLList("values")).containsExactly(
            new XML("<values>1, 2, 3</values>"),
            new XML("<values>4, 5, 6</values>"),
            new XML("<values>7, 8, 9</values>"));
        assertThat(xml.getXMLList("pathToNil")).isEmpty();
    }

    @Test
    void testGetObject() {
        assertThat(new XML(SAMPLE_PROXYSETTINGS_XML).<MockProxySettings>getObject(
                ".", null)).isEqualTo(SAMPLE_PROXY_OBJECT);
        assertThat(new XML(SAMPLE_PROXYSETTINGS_XML).<MockProxySettings>getObject(
                null, null)).isEqualTo(SAMPLE_PROXY_OBJECT);
        assertThat(new XML(SAMPLE_PROXYSETTINGS_XML).<MockProxySettings>getObject(
                "pathToNil", new MockProxySettings()))
            .isEqualTo(new MockProxySettings());
        assertThat(new XML(
                "<converter class=\"DurationConverter\"></converter>")
            .<DurationConverter>getObjectImpl(DurationConverter.class, "."))
            .isEqualTo(new DurationConverter());
        assertThat(new XML("<converter/>")
            .getObjectImpl(null, "", new DurationConverter()))
                .isEqualTo(new DurationConverter());
        assertThat(new XML("<converter/>").<DurationConverter>getObjectImpl(
                DurationConverter.class, null, null)).isNull();
        assertThat(new XML("<converter/>").<DurationConverter>getObjectImpl(
                DurationConverter.class, "pathToNil", new DurationConverter()))
                    .isEqualTo(new DurationConverter());


        Object[] expectedList = {
                new DurationConverter(),
                new EnumConverter(),
                new DateConverter()
        };

        // List
        var pkg = "com.norconex.commons.lang.convert.";
        var xml = XML.of(String.format(
                "<test>"
              + "<converters>"
              + "<converter class=\"%1$sDurationConverter\"></converter>"
              + "<converter class=\"%1$sEnumConverter\"></converter>"
              + "<converter class=\"%1$sDateConverter\"></converter>"
              + "</converters>"
              + "<presentButEmpty>"
              +   "<converters/>"
              + "</presentButEmpty>"
              + "<withSingleNull>"
              +   "<converters>"
              +     "<converter/>"
              +   "</converters>"
              + "</withSingleNull>"
              + "</test>", pkg)
        ).create();
        assertThat(xml.getObjectList("converters/converter"))
            .containsExactly(expectedList);
        assertThat(xml.getObjectList("notPresent",
                Arrays.asList(new DurationConverter())))
            .containsExactly(new DurationConverter());
        assertThat(xml.getObjectList("presentButEmpty/converters/converter",
                Arrays.asList(new DurationConverter()))).isEmpty();
        assertThat(xml.getObjectList("withSingleNull/converters/converter",
                Arrays.asList(new DurationConverter())))
            .containsExactly(new DurationConverter());

        // List Impl
        var xmlImpl = XML.of(
                "<test>"
              + "<converters>"
              + "<converter class=\"DurationConverter\"></converter>"
              + "<converter class=\"com.norconex.commons.lang.convert"
              + ".EnumConverter\"></converter>"
              + "<converter class=\"convert.DateConverter\"></converter>"
              + "</converters>"
              + "</test>"
        ).create();
        assertThat(xmlImpl.getObjectListImpl(
                Converter.class, "converters/converter"))
            .containsExactly(expectedList);
        assertThat(xml.getObjectListImpl(DurationConverter.class,
                "presentButEmpty/converters/converter",
                Arrays.asList(new DurationConverter()))).isEmpty();
        assertThat(xml.getObjectListImpl(DurationConverter.class,
                "withSingleNull/converters/converter",
                    Arrays.asList(new DurationConverter())))
            .containsExactly(new DurationConverter());

        assertThat(xmlImpl.toNode()).isNotNull();
    }

    @Test
    void testGetDelimitedListObject() {
        var xml = XML.of(
                "<test>"
              + "<values>1, 2, 3, 4, 5</values>"
              + "<converters>"
              +   "java.time.Duration|java.io.File|java.nio.file.Path"
              + "</converters>"
              + "<noValues/>"
              + "</test>"
        ).create();
        assertThat(xml.getDelimitedList("values", Integer.TYPE))
            .containsExactly(1, 2, 3, 4, 5);
        assertThat(xml.getDelimitedList("converters", Class.class, "\\|"))
            .containsExactly(Duration.class, File.class, Path.class);
        assertThat(xml.getDelimitedList("pathToNil", Integer.TYPE,
                Arrays.asList(6, 7, 8))).containsExactly(6, 7, 8);
        // Deliberately empty
        assertThat(xml.getDelimitedList("noValues", Integer.TYPE,
                Arrays.asList(9, 10))).isEmpty();
    }

    @Test
    void testToObject() {
        var proxy = new XML(SAMPLE_PROXYSETTINGS_XML).toObject();
        assertThat(proxy).isEqualTo(SAMPLE_PROXY_OBJECT);
        assertThatExceptionOfType(XMLException.class).isThrownBy( //NOSONAR
                () -> new XML(SAMPLE_XML).toObject(SAMPLE_PROXY_OBJECT));

        proxy = new XML("<proxySettings/>").toObject(SAMPLE_PROXY_OBJECT);
        assertThat(proxy).isNull();
        proxy = new XML("<proxySettings/>").toObjectImpl(
                MockProxySettings.class, SAMPLE_PROXY_OBJECT);
        assertThat(proxy).isNull();

        proxy = new XML("<proxySettings class="
                + "\"com.norconex.commons.lang.xml.mock.MockProxySettings\"/>")
                .toObject(new MockProxySettings());
        assertThat(proxy).isEqualTo(new MockProxySettings());

        assertThat(new XML(SAMPLE_XML).toObjectImpl(null, new MockProxySettings()))
            .isEqualTo(new MockProxySettings());

        // test when class is not in XML
        var xml = new XML(SAMPLE_PROXYSETTINGS_XML);
        xml.setAttribute("class", "xml.mock.MockProxySettings");
        proxy = xml.toObjectImpl(MockProxySettings.class);
        assertThat(proxy).isEqualTo(SAMPLE_PROXY_OBJECT);

        //test errors
        var badXml = new XML(SAMPLE_PROXYSETTINGS_XML);
        badXml.setAttribute("class", "blah.blah.IdoNotExist");
        assertThatExceptionOfType(XMLException.class).isThrownBy(
                () -> badXml.toObjectImpl(MockProxySettings.class))
            .withStackTraceContaining("No class implementing");

        var badXml2 = new XML(SAMPLE_PROXYSETTINGS_XML);
        badXml.setAttribute("class", "xml.mock.MockProxySettings");
        assertThatExceptionOfType(XMLException.class).isThrownBy(
                () -> badXml2.toObjectImpl(HttpURL.class))
            .withStackTraceContaining("is not an instance of");

        xml = XML.of("<test class=\"DurationConverter\"></test>").create();
        var c = xml.toObjectImpl(Converter.class);
        Assertions.assertNotNull(c);

        var xmlMany = XML.of("<test class=\"erConverter\"></test>").create();
        assertThatExceptionOfType(XMLException.class)
            .isThrownBy(() -> xmlMany.toObjectImpl(Converter.class))
            .withStackTraceContaining(
                    "Consider using fully qualified class name.");
    }

    @Test
    void testPopulate() {
        var expectedProxy = SAMPLE_PROXY_OBJECT;

        var proxy = new MockProxySettings();
        new XML(SAMPLE_PROXYSETTINGS_XML).populate(proxy);
        assertThat(proxy).isEqualTo(expectedProxy);

        //test errors
        var xml = new XML("proxySettings");
        xml.setAttribute("badOne", "IM_BAD");
        var badProxy = new MockProxySettings();
        assertThatExceptionOfType(XMLException.class).isThrownBy(
                () -> xml.populate(badProxy)
        )
        .withStackTraceContaining(
                "Attribute 'badOne' is not allowed to appear");

        // test with xpath
        var creds = new MockCredentials();
        new XML(SAMPLE_PROXYSETTINGS_XML).populate(creds, "credentials");
        assertThat(creds).isEqualTo(expectedProxy.getCredentials());

        // test nulls
        var proxyNull = new MockProxySettings();
        assertThatNoException().isThrownBy(
                () -> new XML((String) null).populate(proxyNull));
        assertThatNoException().isThrownBy(
                () -> new XML(SAMPLE_PROXYSETTINGS_XML).populate(null));
    }

    @Test
    void testEnabledDisabled() {
        var cls =
                "class=\"com.norconex.commons.lang.convert.DateConverter\" ";
        assertThat(new XML("<test " + cls + "/>").isEnabled()).isFalse();
        assertThat(new XML("<test " + cls + "/>").isDisabled()).isFalse();

        assertThat(new XML("<test " + cls + " disabled=\"true\"/>")
                .isEnabled()).isFalse();
        assertThat(new XML("<test " + cls + " disabled=\"true\"/>")
                .isDisabled()).isTrue();

        assertThat(new XML("<test " + cls + " enabled=\"true\"/>")
                .isEnabled()).isTrue();
        assertThat(new XML("<test " + cls + " enabled=\"true\"/>")
                .isDisabled()).isFalse();

        assertThat(new XML((String) null).isEnabled()).isFalse();
        assertThat(new XML((String) null).isDisabled()).isFalse();
    }


    @Test
    void testXMLCreation() throws IOException {
        // all these must be equivalent
        var xmlFile = tempDir.resolve("sample.xml");
        Files.writeString(xmlFile, SAMPLE_XML);

        assertThat(new XML(xmlFile)).hasToString(SAMPLE_XML);
        assertThat(new XML(xmlFile.toFile())).hasToString(SAMPLE_XML);
        assertThat(new XML(Files.newBufferedReader(xmlFile)))
            .hasToString(SAMPLE_XML);
        assertThat(new XML(SAMPLE_XML)).hasToString(SAMPLE_XML);
        var freshXML = new XML("test");
        freshXML.addXML(SAMPLE_XML);
        freshXML.unwrap();
        assertThat(freshXML).hasToString(SAMPLE_XML);
        freshXML = new XML("test", null);
        freshXML.addXML(SAMPLE_XML);
        freshXML.unwrap();
        assertThat(freshXML).hasToString(SAMPLE_XML);

        // with object
        assertThat(new XML("test", new DurationConverter()))
            .hasToString("<test class=\"com.norconex.commons.lang.convert"
                    + ".DurationConverter\"/>");
        assertThat(new XML("test", DurationConverter.class))
            .hasToString("<test class=\"com.norconex.commons.lang.convert"
                    + ".DurationConverter\"/>");
        assertThat(new XML("test", 123)).hasToString("<test>123</test>");

        assertThat(XML.of(Files.newInputStream(xmlFile)).create())
            .hasToString(SAMPLE_XML);
    }

    @Test
    void testInsertBeforeAfter() {
        var xml = new XML(SAMPLE_XML);
        var nestedbXml = xml.getXML("nestedB");
        nestedbXml.insertBefore(new XML("<nestedA2>A2</nestedA2>"));
        nestedbXml.insertAfter(new XML("<nestedB2>B2</nestedB2>"));
        Assertions.assertEquals(
                """
                	<sampleTag add="juice" id="orange">\
                	<nestedA id="na" type="ta">\
                	Blah\
                	</nestedA>\
                	<nestedA2>A2</nestedA2>\
                	<nestedB id="nb"/>\
                	<nestedB2>B2</nestedB2>\
                	<nestedC/>\
                	</sampleTag>""", xml.toString());
    }

    @Test
    void testRemove() {
        XML xml;
        XML removedXML;

        // Remove child with parent
        xml = new XML(SAMPLE_XML);
        var nestedAXML = xml.getXML("nestedA");
        removedXML = nestedAXML.remove();
        Assertions.assertEquals(
                """
                	<sampleTag add="juice" id="orange">\
                	<nestedB id="nb"/>\
                	<nestedC/>\
                	</sampleTag>""", xml.toString());
        Assertions.assertEquals(
                """
                	<nestedA id="na" type="ta">\
                	Blah\
                	</nestedA>""", removedXML.toString());

        // Remove parent
        xml = new XML(SAMPLE_XML);
        removedXML = xml.remove();
        Assertions.assertEquals(SAMPLE_XML, xml.toString());
        Assertions.assertEquals(SAMPLE_XML, removedXML.toString());

        var xmlRemove = new XML(
                "<test>"
              +   "<one>1</one>"
              +   "<two>2</two>"
              +   "<three>3</three>"
              + "</test>"
        );
        xmlRemove.removeElement("two");
        assertThat(xmlRemove).hasToString(
                """
                	<test>\
                	<one>1</one>\
                	<three>3</three>\
                	</test>"""
        );
    }


    @Test
    void testMap() {
        var xmlElements =
                """
        	<myentry mykey="f1">v1</myentry>\
        	<myentry mykey="2.22">v2a</myentry>\
        	<myentry mykey="2.22">v2b</myentry>\
        	<myentry mykey="2.22">v2c</myentry>\
        	<myentry mykey="f3">3.3</myentry>\
        	<myentry mykey="f3">33333</myentry>""";
        var xmlNoParent = "<test>" + xmlElements + "</test>";
        var xmlParent = "<test><mymap>" + xmlElements + "</mymap></test>";

        Map<Object, Object> map = new ListOrderedMap<>();
        // string -> string
        map.put("f1", "v1");
        // object -> string array
        map.put(Double.valueOf(2.22), new String[] {"v2a", "v2b", "v2c"});
        // string -> mixed object collection
        map.put("f3", Arrays.asList(
                Double.valueOf(3.3), Duration.ofMillis(33333)));

        // Test without parent:
        var xml = XML.of("test").create();
        xml.addElementMap("myentry", "mykey", map);

        Assertions.assertEquals(xmlNoParent, xml.toString());

        // Test with parent:
        xml = XML.of("test").create();
        xml.addElementMap("mymap", "myentry", "mykey", map);
        Assertions.assertEquals(xmlParent, xml.toString());

        assertThat(new XML("<test/>").addElementMap(
                "name", "attribute", Collections.emptyMap())).isEmpty();
    }

    @Test
    void testGetBasicTypes() {
        var xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();
        Assertions.assertEquals("a string", xml.getString("testString"));
        Assertions.assertEquals(
                (Integer) 123, xml.getInteger("testNumeric/@int"));
        Assertions.assertEquals((Integer) 456, xml.getInteger("nil", 456));
        Assertions.assertEquals(
                (Long) 12345L, xml.getLong("testNumeric/@long"));
        Assertions.assertEquals((Long) 678L, xml.getLong("nil", 678L));
        Assertions.assertEquals(
                (Float) 54.01f, xml.getFloat("testNumeric/@float"));
        Assertions.assertEquals((Float) 67.8f, xml.getFloat("nil", 67.8f));
        Assertions.assertEquals(
                (Double) 54321.0123, xml.getDouble("testNumeric/@double"));
        Assertions.assertEquals((Double) 111.3, xml.getDouble("nil", 111.3));
        Assertions.assertEquals(
                new Dimension(640, 480), xml.getDimension("dimOK"));
        Assertions.assertEquals(new Dimension(64, 48),
                xml.getDimension("nil", new Dimension(64, 48)));
        assertThat(new XML("<test>true</test>").getBoolean(".")).isTrue();
        assertThat(new XML("<test>false</test>").getBoolean(".")).isFalse();
        assertThat(new XML("<test/>").getBoolean("nil", true)).isTrue();
        Assertions.assertEquals(
                Arrays.asList("one", "two", "three"),
                xml.getDelimitedStringList("testDelimStringList"));
        Assertions.assertEquals(
                Arrays.asList("four", "five", "six"),
                xml.getStringList("testStringList/item"));
    }

    @Test
    void testGetCommonTypes() {
        assertThat(new XML("<test>fr_CA</test>").getLocale("."))
            .isEqualTo(Locale.CANADA_FRENCH);
        assertThat(new XML("<test/>").getLocale("nil", Locale.ITALIAN))
            .isEqualTo(Locale.ITALIAN);
        assertThat(new XML("<test>UTF-8</test>").getCharset("."))
            .isEqualTo(StandardCharsets.UTF_8);
        assertThat(new XML("<test/>").getCharset("nil", ISO_8859_1))
            .isEqualTo(ISO_8859_1);
        assertThat(new XML("<test>2KiB</test>").getDataSize("."))
            .isEqualTo(2048);
        assertThat(new XML("<test/>").getDataSize("nil", 123L)).isEqualTo(123L);
        assertThat(new XML("<test>2MiB</test>").getDataSize(
                ".", DataUnit.KIB, 1234L)).isEqualTo(2048);
        assertThat(new XML("<test>3 minutes</test>").getDurationMillis("."))
            .isEqualTo(3 * 60 * 1000);
        assertThat(new XML("<test/>").getDurationMillis("nil", 123L))
            .isEqualTo(123L);
        assertThat(new XML("<test>3 minutes</test>").getDuration("."))
            .isEqualTo(Duration.ofMinutes(3));
        assertThat(new XML("<test/>").getDuration("nil", Duration.ofDays(2)))
            .isEqualTo(Duration.ofDays(2));
    }

    @Test
    void testGetNullEmptyBlank() {
        var xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();

        // As strings
        Assertions.assertNull(xml.getString("testNull"));
        Assertions.assertNull(xml.getString("testNullMissing"));
        Assertions.assertEquals("", xml.getString("testEmpty"));
        Assertions.assertEquals("", xml.getString("testBlank"));
        Assertions.assertEquals("  ", xml.getString("testBlankPreserve"));

        // As Nodes
        Assertions.assertNotNull(xml.getNode("testNull"));
        Assertions.assertNull(xml.getNode("testNullMissing"));
        Assertions.assertNotNull(xml.getNode("testEmpty"));
        Assertions.assertNotNull(xml.getNode("testBlank"));
        Assertions.assertNotNull(xml.getString("testBlankPreserve"));

        // As XMLs
        Assertions.assertNotNull(xml.getXML("testNull"));
        Assertions.assertNull(xml.getXML("testNullMissing"));
        Assertions.assertNotNull(xml.getXML("testEmpty"));
        Assertions.assertNotNull(xml.getXML("testBlank"));
        Assertions.assertNotNull(xml.getString("testBlankPreserve"));

        // As Enum
        Assertions.assertNull(xml.getEnum("testNull", Source.class));
        Assertions.assertNull(xml.getEnum("testNullMissing", Source.class));
        Assertions.assertThrows(ConverterException.class,
                () -> xml.getEnum("testEmpty", Source.class));
        Assertions.assertThrows(ConverterException.class,
                () -> xml.getEnum("testBlank", Source.class));
        Assertions.assertThrows(ConverterException.class,
                () -> xml.getEnum("testBlankPreserve", Source.class));
    }

    @Test
    void testAddNullEmptyBlankElements() {
        // Null, empty, and blank strings should all be loaded back as such.
        var xml1 = XML.of("test").create();

        // As elements with no attribs
        xml1.addElement("elmNull", null);
        xml1.addElement("elmEmpty", "");
        xml1.addElement("elmBlankPreserve", " \n ");

        // As elements with attrib
        xml1.addElement("elmNullAttr", null).setAttribute("attr", "exists");
        xml1.addElement("elmEmptyAttr", "").setAttribute("attr", "exists");
        xml1.addElement(
                "elmBlankPreserveAttr", " \n ").setAttribute("attr", "exists");

        var xmlStr = xml1.toString();
        LOG.debug("XML is: " + xmlStr);

        var xml2 = XML.of(xmlStr).create();

        Assertions.assertNull(xml2.getString("elmNull"));
        Assertions.assertEquals("", xml2.getString("elmEmpty"));
        Assertions.assertEquals(" \n ", xml2.getString("elmBlankPreserve"));

        Assertions.assertNull(xml2.getString("elmNullAttr"));
        Assertions.assertEquals("", xml2.getString("elmEmptyAttr"));
        Assertions.assertEquals(" \n ", xml2.getString("elmBlankPreserveAttr"));
    }

    @Test
    void testUnwrap() {
        var wrapped =
                "<rootTag id=\"banana\" remove=\"me\">"
              + SAMPLE_XML
              + "</rootTag>";
        Assertions.assertEquals(SAMPLE_XML,
                XML.of(wrapped).create().unwrap().toString());

        var list = SAMPLE_XML + "<sampleTag>Another child</sampleTag>";
        var wrappedList =
                "<rootTag id=\"banana\" remove=\"me\">" + list + "</rootTag>";
        try { //NOSONAR
            XML.of(wrappedList).create().unwrap();
            Assertions.fail("Should have thrown exception.");
        } catch (XMLException e) { }
    }

    @Test
    void testWrap() {
        var target = "<parentTag>" + SAMPLE_XML + "</parentTag>";
        Assertions.assertEquals(target, XML.of(
                SAMPLE_XML).create().wrap("parentTag").toString());
    }

    @Test
    void testClear() {
        Assertions.assertEquals("<sampleTag/>",
                XML.of(SAMPLE_XML).create().clear().toString());
    }

    @Test
    void testReplace() {
        var replacement = "<replacementTag>I replace!</replacementTag>";
        Assertions.assertEquals(replacement, XML.of(
                SAMPLE_XML).create().replace(XML.of(
                        replacement).create()).toString());
    }

    @Test
    void testIsElementPresent() {
        var xml = XML.of(SAMPLE_XML).create();
        assertThat(xml.isElementPresent("banana")).isFalse();
        assertThat(xml.isElementPresent("nestedA")).isTrue();
    }

    @Test
    void testComputeElementIfAbsent() {
        var xml = XML.of(SAMPLE_XML).create();
        xml.computeElementIfAbsent("banana", name -> "split");
        assertThat(xml.getStringList("banana")).hasSize(1);

        xml.computeElementIfAbsent("banana", name -> "split more");
        assertThat(xml.getStringList("banana")).hasSize(1);
    }

    @Test
    void testGetNullMissingDefaultElements() {
        var xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();

        //--- Strings ---

        // self-closing tag is null
        Assertions.assertNull(xml.getString("testNull", "shouldBeNull"));
        // empty tag is empty
        Assertions.assertEquals(
                "", xml.getString("testEmpty", "shouldBeEmpty"));
        // blank tag is empty
        Assertions.assertEquals(
                "", xml.getString("testBlank", "shouldBeEmpty"));
        // blank preserve tag returns actual content
        Assertions.assertEquals("  ", xml.getString(
                "testBlankPreserve", "shouldBeTwoSpaces"));
        // missing tag uses default
        Assertions.assertEquals(
                "pickMe", xml.getString("testMissing", "pickMe"));

        //--- Misc. Objects ---

        // actual value when set
        Assertions.assertEquals(new Dimension(640, 480),
                xml.getDimension("dimOK", new Dimension(10, 20)));
        // self-closing tag is null
        Assertions.assertNull(
                xml.getDimension("dimNull", new Dimension(30, 40)));
        // missing tag uses default
        Assertions.assertEquals(new Dimension(70, 80),
                xml.getDimension("dimMissing", new Dimension(70, 80)));
        // empty tag should fail
        Assertions.assertThrows(ConverterException.class, () -> //NOSONAR
            xml.getDimension("dimEmpty", new Dimension(50, 60)),
            "Dimension wrongfully converted from empty string.");
        // blank tag should fail
        Assertions.assertThrows(ConverterException.class, () -> //NOSONAR
            xml.getDimension("dimBlank", new Dimension(50, 60)),
            "Dimension wrongfully converted from blank string.");
        // blankPreserve tag should fail
        Assertions.assertThrows(ConverterException.class, () -> //NOSONAR
            xml.getDimension("dimBlankPreserve", new Dimension(50, 60)),
            "Dimension wrongfully converted from blankPreserve string.");
    }

    @Test
    void testGetListNullMissingDefaultElements() {
        var xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();
        List<Dimension> defaultList = Arrays.asList(
                new Dimension(1, 2), new Dimension(3, 4));

        // Lists are never null, so what would normally be null is
        // considered empty list when obtained as a list.


        //--- empty lists ---

        // self-closing tag is empty list
        Assertions.assertTrue(xml.getList("listNull", Dimension.class,
                defaultList).isEmpty(), "List not empty.");
        // empty tag should fail
        Assertions.assertThrows(ConverterException.class, () ->
            xml.getList("listEmpty", Dimension.class, defaultList),
            "Dimension wrongfully converted from empty string.");
        // blank tag should fail
        Assertions.assertThrows(ConverterException.class, () ->
            xml.getList("listBlank", Dimension.class, defaultList),
            "Dimension wrongfully converted from blank string.");
        // blankPreserve tag should fail
        Assertions.assertThrows(ConverterException.class, () ->
            xml.getList("listBlankPreserve", Dimension.class, defaultList),
            "Dimension wrongfully converted from blankPreserve string.");

        // missing tag uses default
        Assertions.assertEquals(defaultList,
                xml.getList("missingList", Dimension.class, defaultList));
        // self-closing entries is empty list
        Assertions.assertTrue(xml.getList("listNullEntries/entry",
                Dimension.class, defaultList).isEmpty(), "List not empty.");
        // OK entries returns normally
        Assertions.assertEquals(
                Arrays.asList(new Dimension(10, 20), new Dimension(30, 40)),
                xml.getList("listOKEntries/entry",
                        Dimension.class, defaultList));
        // Mixed entries should return only OK ones
        Assertions.assertEquals(Arrays.asList(new Dimension(50, 60)),
                xml.getList("listMixedEntries/entry",
                        Dimension.class, defaultList));
        // No parent lists returns normally
        Assertions.assertEquals(
                Arrays.asList(new Dimension(70, 80), new Dimension(90, 100)),
                xml.getList("listNoParent", Dimension.class, defaultList));

        // Self-closing no parent lists returns empty list
        Assertions.assertTrue(xml.getList("listNullNoParent",
                Dimension.class, defaultList).isEmpty(), "List not empty.");
    }


    @Test
    void testOverwriteDefaultWithEmptyListReadWrite() {
        var c = new ClassWithDefaultLists();

        // Defaults should be loaded back:
        XML.assertWriteRead(c, "test");

        // Defaults should not be loaded back:
        c.enums.clear();
        c.strings.clear();
        XML.assertWriteRead(c, "test");
    }

    @Test
    void testSetGetRemoveContentText() {
        var xml = new XML("""
            <test attr="val">
              here
              <child childAttr="childVal">a child</child>

              there
            </test>
            """);

        assertThat(xml.getTextContent()).isEqualTo("here there");
        assertThat(xml.getXML("child").getTextContent()).isEqualTo("a child");

        xml.setTextContent("somewhere");
        assertThat(xml.getTextContent()).isEqualTo("somewhere");
        // child elements should not be affected
        assertThat(xml.getXML("child").getTextContent()).isEqualTo("a child");


        xml.removeTextContent();
        assertThat(xml.getTextContent()).isNull();
        // child elements should not be affected
        assertThat(xml.getXML("child").getTextContent()).isEqualTo("a child");

        xml.setTextContent("some value");
        assertThat(xml.getTextContent()).isEqualTo("some value");
    }

    @ToString
    @EqualsAndHashCode
    static class ClassWithDefaultLists implements XMLConfigurable {

        private final List<DataUnit> enums =
                new ArrayList<>(Arrays.asList(DataUnit.KB));
        private final List<String> strings =
                new ArrayList<>(Arrays.asList("blah", "halb"));

        @Override
        public void loadFromXML(XML xml) {
            CollectionUtil.setAll(enums,
                    xml.getDelimitedEnumList("enums", DataUnit.class, enums));
            CollectionUtil.setAll(strings,
                    xml.getDelimitedStringList("strings", strings));
        }
        @Override
        public void saveToXML(XML xml) {
            xml.addDelimitedElementList("enums", enums);
            xml.addDelimitedElementList("strings", strings);
        }
    }
}
