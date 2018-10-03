/* Copyright 2018 Norconex Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.ConverterException;
import com.norconex.commons.lang.unit.DataUnit;

/**
 * @author Pascal Essiembre
 */
public class XMLTest {

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
    public void testGetCommonTypes() {
        XML xml = new XML(ResourceLoader.getXmlString(XMLTest.class));

        Assert.assertEquals("a string", xml.getString("testString"));
        Assert.assertEquals((Integer) 123, xml.getInteger("testNumeric/@int"));
        Assert.assertEquals((Long) 12345L, xml.getLong("testNumeric/@long"));
        Assert.assertEquals((Float) 54.01f, xml.getFloat("testNumeric/@float"));
        Assert.assertEquals(
                (Double) 54321.0123, xml.getDouble("testNumeric/@double"));
        Assert.assertEquals(
                Arrays.asList("one", "two", "three"),
                xml.getDelimitedStringList("testDelimStringList"));
        Assert.assertEquals(
                Arrays.asList("four", "five", "six"),
                xml.getStringList("testStringList/item"));
    }

    @Test
    public void testGetNullEmptyBlank() {
        XML xml = new XML(ResourceLoader.getXmlString(XMLTest.class));

        // As strings
        Assert.assertNull(xml.getString("testNull"));
        Assert.assertNull(xml.getString("testNullMissing"));
        Assert.assertEquals("", xml.getString("testEmpty"));
        Assert.assertEquals("  ", xml.getString("testBlank"));

        // As Nodes
        Assert.assertNotNull(xml.getNode("testNull"));
        Assert.assertNull(xml.getNode("testNullMissing"));
        Assert.assertNotNull(xml.getNode("testEmpty"));
        Assert.assertNotNull(xml.getNode("testBlank"));

        // As XMLs
        Assert.assertNotNull(xml.getXML("testNull"));
        Assert.assertNull(xml.getXML("testNullMissing"));
        Assert.assertNotNull(xml.getXML("testEmpty"));
        Assert.assertNotNull(xml.getXML("testBlank"));
    }

    @Test
    public void testAddNullEmptyBlankElements() {
        // Null, empty, and blank strings should all be loaded back as such.
        XML xml1 = new XML("test");

        // As elements with no attribs
        xml1.addElement("elmNull", null);
        xml1.addElement("elmEmpty", "");
        xml1.addElement("elmBlank", " \n ");

        // As elements with attrib
        xml1.addElement("elmNullAttr", null).setAttribute("attr", "exists");
        xml1.addElement("elmEmptyAttr", "").setAttribute("attr", "exists");
        xml1.addElement("elmBlankAttr", " \n ").setAttribute("attr", "exists");

        String xmlStr = xml1.toString();
        LOG.debug("XML is: " + xmlStr);

        XML xml2 = new XML(xmlStr);


        Assert.assertNull(xml2.getString("elmNull"));
        Assert.assertEquals("", xml2.getString("elmEmpty"));
        Assert.assertEquals(" \n ", xml2.getString("elmBlank"));

        Assert.assertNull(xml2.getString("elmNullAttr"));
        Assert.assertEquals("", xml2.getString("elmEmptyAttr"));
        Assert.assertEquals(" \n ", xml2.getString("elmBlankAttr"));

        //TODO test attributes the same way?
    }

    @Test
    public void testUnwrap() {
        String wrapped =
                "<rootTag id=\"banana\" remove=\"me\">"
              + SAMPLE_XML
              + "</rootTag>";
        Assert.assertEquals(SAMPLE_XML, new XML(wrapped).unwrap().toString());

        String list = SAMPLE_XML + "<sampleTag>Another child</sampleTag>";
        String wrappedList =
                "<rootTag id=\"banana\" remove=\"me\">" + list + "</rootTag>";
        try {
            new XML(wrappedList).unwrap();
            Assert.fail("Should have thrown exception.");
        } catch (XMLException e) { }
    }

    @Test
    public void testWrap() {
        String target = "<parentTag>" + SAMPLE_XML + "</parentTag>";
        Assert.assertEquals(
                target, new XML(SAMPLE_XML).wrap("parentTag").toString());
    }

    @Test
    public void testClear() {
        Assert.assertEquals("<sampleTag/>",
                new XML(SAMPLE_XML).clear().toString());
    }

    @Test
    public void testReplace() {
        String replacement = "<replacementTag>I replace!</replacementTag>";
        Assert.assertEquals(replacement,
                new XML(SAMPLE_XML).replace(new XML(replacement)).toString());
    }

    @Test
    public void testGetNullMissingDefaultElements() {
        XML xml = new XML(ResourceLoader.getXmlString(XMLTest.class));

        //--- Strings ---

        // self-closing tag is null
        Assert.assertNull(xml.getString("testNull", "shouldBeNull"));
        // empty tag is empty
        Assert.assertEquals("", xml.getString("testEmpty", "shouldBeEmpty"));
        // missing tag uses default
        Assert.assertEquals("pickMe", xml.getString("testMissing", "pickMe"));

        //--- Misc. Objects ---

        // actual value when set
        Assert.assertEquals(new Dimension(640, 480),
                xml.getDimension("dimOK", new Dimension(10, 20)));
        // self-closing tag is null
        Assert.assertNull(xml.getDimension("dimNull", new Dimension(30, 40)));
        // missing tag uses default
        Assert.assertEquals(new Dimension(70, 80),
                xml.getDimension("dimMissing", new Dimension(70, 80)));
        // empty tag should fail
        try {
            xml.getDimension("dimEmpty", new Dimension(50, 60));
            Assert.fail("Dimension wrongfully converted from empty string.");
        } catch (ConverterException e) {
            // swallow
        }
    }

    @Test
    public void testGetListNullMissingDefaultElements() {
        XML xml = new XML(ResourceLoader.getXmlString(XMLTest.class));
        List<Dimension> defaultList = Arrays.asList(
                new Dimension(1, 2), new Dimension(3, 4));

        // Lists are never null, so what would normally be null is
        // considered empty list when obtained as a list.


        //--- empty lists ---

        // self-closing tag is empty list
        Assert.assertTrue("List not empty.", xml.getList(
                "listNull", Dimension.class, defaultList).isEmpty());
        // empty tag should fail
        try {
            xml.getList("listEmpty", Dimension.class, defaultList);
            Assert.fail(
                    "Dimension list wrongfully converted from empty string.");
        } catch (ConverterException e) {
            // swallow
        }
        // blank tag should fail
        try {
            xml.getList("listBlank", Dimension.class, defaultList);
            Assert.fail(
                    "Dimension list wrongfully converted from blank string.");
        } catch (ConverterException e) {
            // swallow
        }
        // missing tag uses default
        Assert.assertEquals(defaultList,
                xml.getList("missingList", Dimension.class, defaultList));
        // self-closing entries is empty list
        Assert.assertTrue("List not empty.", xml.getList(
                "listNullEntries/entry", Dimension.class,
                defaultList).isEmpty());
        // OK entries returns normally
        Assert.assertEquals(
                Arrays.asList(new Dimension(10, 20), new Dimension(30, 40)),
                xml.getList("listOKEntries/entry",
                        Dimension.class, defaultList));
        // Mixed entries should return only OK ones
        Assert.assertEquals(Arrays.asList(new Dimension(50, 60)),
                xml.getList("listMixedEntries/entry",
                        Dimension.class, defaultList));
        // No parent lists returns normally
        Assert.assertEquals(
                Arrays.asList(new Dimension(70, 80), new Dimension(90, 100)),
                xml.getList("listNoParent", Dimension.class, defaultList));

        // Self-closing no parent lists returns empty list
        Assert.assertTrue("List not empty.", xml.getList("listNullNoParent",
                Dimension.class, defaultList).isEmpty());
    }


    @Test
    public void testOverwriteDefaultWithEmptyListReadWrite() {
        ClassWithDefaultLists c = new ClassWithDefaultLists();

        // Defaults should be loaded back:
       // XML.assertWriteRead(c, "test");

        // Defaults should not be loaded back:
        c.enums.clear();
        c.strings.clear();
        XML.assertWriteRead(c, "test");
    }
    public static class ClassWithDefaultLists implements IXMLConfigurable {

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
