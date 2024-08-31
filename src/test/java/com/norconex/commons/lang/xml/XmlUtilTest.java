/* Copyright 2019-2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

class XmlUtilTest {

    @Test
    void testXMLUtil(@TempDir Path tempDir) throws SAXException, IOException {
        Schema schema = XmlUtil.createSchemaFactory().newSchema();
        assertThat(schema).isNotNull();
        Validator validator = XmlUtil.createSchemaValidator(schema);
        assertThat(validator).isNotNull();
        XMLReader xmlReader = XmlUtil.createXMLReader();
        assertThat(xmlReader).isNotNull();
        DocumentBuilderFactory dbf = XmlUtil.createDocumentBuilderFactory();
        assertThat(dbf).isNotNull();
        SAXParserFactory spf = XmlUtil.createSaxParserFactory();
        assertThat(spf).isNotNull();
        XMLInputFactory xif = XmlUtil.createXMLInputFactory();
        assertThat(xif).isNotNull();

        Path tmpFile = tempDir.resolve("tempFile.xml");
        Files.createFile(tmpFile);
        assertThat(XmlUtil.createXMLEventReader(tmpFile)).isNotNull();
        assertThat(XmlUtil.createXMLEventReader(tmpFile.toFile())).isNotNull();

        Xml xml = new Xml("<test><value>1</value></test>");
        assertThat(XmlUtil.createXMLEventReader(xml.getNode())).isNotNull();
        assertThat(XmlUtil.createXMLEventReader(xml)).isNotNull();
        assertThat(XmlUtil.createXMLEventReader(xml.toString())).isNotNull();
        assertThat(XmlUtil.createXMLEventReader(new ByteArrayInputStream(
                xml.toString().getBytes()))).isNotNull();
        assertThat(XmlUtil.createXMLEventReader(new StringReader(
                xml.toString()))).isNotNull();
    }

    @Test
    void testToName() {
        assertThat(XmlUtil.toLocalName(new QName("nx:blah"))).isEqualTo("blah");
        assertThat(XmlUtil.toName(new QName("nx:blah"))).isEqualTo("nx:blah");
        assertThat(XmlUtil.toName(new QName("blah"))).isEqualTo("blah");
    }
}
