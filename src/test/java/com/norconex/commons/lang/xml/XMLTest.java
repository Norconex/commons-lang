/* Copyright 2018-2021 Norconex Inc.
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

import java.awt.Dimension;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.ConverterException;
import com.norconex.commons.lang.convert.DateConverter;
import com.norconex.commons.lang.convert.DurationConverter;
import com.norconex.commons.lang.convert.EnumConverter;
import com.norconex.commons.lang.convert.IConverter;
import com.norconex.commons.lang.encrypt.EncryptionKey.Source;
import com.norconex.commons.lang.unit.DataUnit;

/**
 * @author Pascal Essiembre
 */
class XMLTest {

    private static final Logger LOG =
            LoggerFactory.getLogger(XMLTest.class);

    public static final String SAMPLE_XML =
            "<sampleTag add=\"juice\" id=\"orange\">"
              + "<nestedA id=\"na\" type=\"ta\">"
              + "Blah"
              + "</nestedA>"
              + "<nestedB id=\"nb\"/>"
              + "<nestedC/>"
          + "</sampleTag>";

    @Test
    void testInsertBeforeAfter() {
        XML xml = new XML(SAMPLE_XML);
        XML nestedbXml = xml.getXML("nestedB");
        nestedbXml.insertBefore(new XML("<nestedA2>A2</nestedA2>"));
        nestedbXml.insertAfter(new XML("<nestedB2>B2</nestedB2>"));
        Assertions.assertEquals(
                "<sampleTag add=\"juice\" id=\"orange\">"
                        + "<nestedA id=\"na\" type=\"ta\">"
                        + "Blah"
                        + "</nestedA>"
                        + "<nestedA2>A2</nestedA2>"
                        + "<nestedB id=\"nb\"/>"
                        + "<nestedB2>B2</nestedB2>"
                        + "<nestedC/>"
                    + "</sampleTag>", xml.toString());
    }

    @Test
    void testRemove() {
        XML xml = null;
        XML removedXML = null;

        // Remove child with parent
        xml = new XML(SAMPLE_XML);
        XML nestedAXML = xml.getXML("nestedA");
        removedXML = nestedAXML.remove();
        Assertions.assertEquals(
                "<sampleTag add=\"juice\" id=\"orange\">"
                + "<nestedB id=\"nb\"/>"
                + "<nestedC/>"
                + "</sampleTag>", xml.toString());
        Assertions.assertEquals(
                "<nestedA id=\"na\" type=\"ta\">"
                + "Blah"
                + "</nestedA>", removedXML.toString());

        // Remove parent
        xml = new XML(SAMPLE_XML);
        removedXML = xml.remove();
        Assertions.assertEquals(SAMPLE_XML, xml.toString());
        Assertions.assertEquals(SAMPLE_XML, removedXML.toString());
    }


    @Test
    void testMap() {
        String xmlElements =
                "<myentry mykey=\"f1\">v1</myentry>"
              + "<myentry mykey=\"2.22\">v2a</myentry>"
              + "<myentry mykey=\"2.22\">v2b</myentry>"
              + "<myentry mykey=\"2.22\">v2c</myentry>"
              + "<myentry mykey=\"f3\">3.3</myentry>"
              + "<myentry mykey=\"f3\">33333</myentry>";
        String xmlNoParent = "<test>" + xmlElements + "</test>";
        String xmlParent = "<test><mymap>" + xmlElements + "</mymap></test>";

        Map<Object, Object> map = new ListOrderedMap<>();
        // string -> string
        map.put("f1", "v1");
        // object -> string array
        map.put(Double.valueOf(2.22), new String[] {"v2a", "v2b", "v2c"});
        // string -> mixed object collection
        map.put("f3", Arrays.asList(
                Double.valueOf(3.3), Duration.ofMillis(33333)));

        // Test without parent:
        XML xml = XML.of("test").create();
        xml.addElementMap("myentry", "mykey", map);

        Assertions.assertEquals(xmlNoParent, xml.toString());

        // Test with parent:
        xml = XML.of("test").create();
        xml.addElementMap("mymap", "myentry", "mykey", map);
        Assertions.assertEquals(xmlParent, xml.toString());
    }

    @Test
    void testJaxb() {
        JaxbPojo pojo = new JaxbPojo();

        pojo.setFirstName("John");
        pojo.setLastName("Smith");
        pojo.setLuckyNumber(7);

        XML xml = XML.of("test").create();

        xml.addElement("pojo", pojo);

        //System.out.println(xml.toString(4));
        Assertions.assertEquals(7, xml.getInteger("pojo/@luckyNumber"));
        Assertions.assertEquals("John", xml.getString("pojo/firstName"));
        Assertions.assertEquals("Smith", xml.getString("pojo/lastName"));

        JaxbPojo newPojo = xml.getObject("pojo");
        Assertions.assertEquals(7, newPojo.getLuckyNumber());
        Assertions.assertEquals("John", newPojo.getFirstName());
        Assertions.assertEquals("Smith", newPojo.getLastName());
    }


    @Test
    void testGetCommonTypes() {
        XML xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();

        Assertions.assertEquals("a string", xml.getString("testString"));
        Assertions.assertEquals(
                (Integer) 123, xml.getInteger("testNumeric/@int"));
        Assertions.assertEquals(
                (Long) 12345L, xml.getLong("testNumeric/@long"));
        Assertions.assertEquals(
                (Float) 54.01f, xml.getFloat("testNumeric/@float"));
        Assertions.assertEquals(
                (Double) 54321.0123, xml.getDouble("testNumeric/@double"));
        Assertions.assertEquals(
                Arrays.asList("one", "two", "three"),
                xml.getDelimitedStringList("testDelimStringList"));
        Assertions.assertEquals(
                Arrays.asList("four", "five", "six"),
                xml.getStringList("testStringList/item"));
    }

    @Test
    void testGetNullEmptyBlank() {
        XML xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();

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
        XML xml1 = XML.of("test").create();

        // As elements with no attribs
        xml1.addElement("elmNull", null);
        xml1.addElement("elmEmpty", "");
        xml1.addElement("elmBlankPreserve", " \n ");

        // As elements with attrib
        xml1.addElement("elmNullAttr", null).setAttribute("attr", "exists");
        xml1.addElement("elmEmptyAttr", "").setAttribute("attr", "exists");
        xml1.addElement(
                "elmBlankPreserveAttr", " \n ").setAttribute("attr", "exists");

        String xmlStr = xml1.toString();
        LOG.debug("XML is: " + xmlStr);

        XML xml2 = XML.of(xmlStr).create();

        Assertions.assertNull(xml2.getString("elmNull"));
        Assertions.assertEquals("", xml2.getString("elmEmpty"));
        Assertions.assertEquals(" \n ", xml2.getString("elmBlankPreserve"));

        Assertions.assertNull(xml2.getString("elmNullAttr"));
        Assertions.assertEquals("", xml2.getString("elmEmptyAttr"));
        Assertions.assertEquals(" \n ", xml2.getString("elmBlankPreserveAttr"));

        //TODO test attributes the same way?
    }

    @Test
    void testUnwrap() {
        String wrapped =
                "<rootTag id=\"banana\" remove=\"me\">"
              + SAMPLE_XML
              + "</rootTag>";
        Assertions.assertEquals(SAMPLE_XML,
                XML.of(wrapped).create().unwrap().toString());

        String list = SAMPLE_XML + "<sampleTag>Another child</sampleTag>";
        String wrappedList =
                "<rootTag id=\"banana\" remove=\"me\">" + list + "</rootTag>";
        try {
            XML.of(wrappedList).create().unwrap();
            Assertions.fail("Should have thrown exception.");
        } catch (XMLException e) { }
    }

    @Test
    void testWrap() {
        String target = "<parentTag>" + SAMPLE_XML + "</parentTag>";
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
        String replacement = "<replacementTag>I replace!</replacementTag>";
        Assertions.assertEquals(replacement, XML.of(
                SAMPLE_XML).create().replace(XML.of(
                        replacement).create()).toString());
    }

    @Test
    void testToObjectImpl() {
        XML xml = XML.of("<test class=\"DurationConverter\"></test>").create();
        DurationConverter c = xml.toObjectImpl(IConverter.class);
        Assertions.assertNotNull(c);
    }

    @Test
    void testGetObjectListImpl() {
        XML xml = XML.of(
                "<test>"
              + "<converters>"
              + "<converter class=\"DurationConverter\"></converter>"
              + "<converter class=\"com.norconex.commons.lang.convert"
              + ".EnumConverter\"></converter>"
              + "<converter class=\"convert.DateConverter\"></converter>"
              + "</converters>"
              + "</test>"

        ).create();
        List<IConverter> converters = xml.getObjectListImpl(
                IConverter.class, "converters/converter");
        Assertions.assertEquals(3, converters.size());
        Assertions.assertTrue(converters.contains(new DurationConverter()));
        Assertions.assertTrue(converters.contains(new EnumConverter()));
        Assertions.assertTrue(converters.contains(new DateConverter()));
    }

    @Test
    void testGetNullMissingDefaultElements() {
        XML xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();

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
        Assertions.assertThrows(ConverterException.class, () ->
            xml.getDimension("dimEmpty", new Dimension(50, 60)),
            "Dimension wrongfully converted from empty string.");
        // blank tag should fail
        Assertions.assertThrows(ConverterException.class, () ->
            xml.getDimension("dimBlank", new Dimension(50, 60)),
            "Dimension wrongfully converted from blank string.");
        // blankPreserve tag should fail
        Assertions.assertThrows(ConverterException.class, () ->
            xml.getDimension("dimBlankPreserve", new Dimension(50, 60)),
            "Dimension wrongfully converted from blankPreserve string.");
    }

    @Test
    void testGetListNullMissingDefaultElements() {
        XML xml = XML.of(ResourceLoader.getXmlString(XMLTest.class)).create();
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
        ClassWithDefaultLists c = new ClassWithDefaultLists();

        // Defaults should be loaded back:
       // XML.assertWriteRead(c, "test");

        // Defaults should not be loaded back:
        c.enums.clear();
        c.strings.clear();
        XML.assertWriteRead(c, "test");
    }
    static class ClassWithDefaultLists implements IXMLConfigurable {

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
        @Override
        public boolean equals(final Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
        @Override
        public String toString() {
            return new ReflectionToStringBuilder(
                    this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
        }
    }
}
