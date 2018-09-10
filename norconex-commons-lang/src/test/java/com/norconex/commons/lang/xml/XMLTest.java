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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.ResourceLoader;

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

//    setRequestHeaders(xml.parseXMLMap("headers/header", x ->
//    new DefaultMapEntry<>(x.getString("@name"), x.getString(".")),
//    requestHeaders));

}
