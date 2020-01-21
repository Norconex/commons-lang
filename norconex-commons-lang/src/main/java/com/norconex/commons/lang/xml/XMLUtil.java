/* Copyright 2019 Norconex Inc.
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

import static org.apache.commons.lang3.StringUtils.repeat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * <p>
 * Utility XML-related methods. When applicable:
 * </p>
 * <ul>
 *   <li>Uses XML Schema version 1.1</li>
 *   <li>Addresses XML security vulnerabilities (XXE)</li>
 * </ul>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class XMLUtil {

    private static final Logger LOG = LoggerFactory.getLogger(XMLUtil.class);

    public static final String W3C_XML_SCHEMA_NS_URI_1_1 =
            "http://www.w3.org/XML/XMLSchema/v1.1";
    private static final String WRAP_START = "<__wrapper__>";
    private static final String WRAP_END = "</__wrapper__>";
    private static final Pattern ATTR_PATTERN = Pattern.compile(
            "\\s+([A-Za-z_][A-Za-z_\\.:]*?\\s*=\\s*([\"']).*?\\2)");

    private XMLUtil() {
        super();
    }

    public static Validator createSchemaValidator(Schema schema) {
        Validator validator = schema.newValidator();
        try {
            validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException e) {
            LOG.debug(e.getMessage());
        }
        return validator;
    }

    public static SchemaFactory createSchemaFactory() {
        return SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI_1_1);
    }

    public static XMLReader createXMLReader() throws SAXException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        try {
            xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xmlReader.setFeature("http://apache.org/xml/features/"
                    + "nonvalidating/load-external-dtd", false);
            xmlReader.setFeature("http://xml.org/sax/features/"
                    + "external-general-entities", false);
            xmlReader.setFeature("http://xml.org/sax/features/"
                    + "external-parameter-entities", false);
            xmlReader.setEntityResolver((publicId, systemId) -> null);
        } catch (SAXException e) {
            LOG.debug(e.getMessage());
        }
        return xmlReader;
    }

    public static DocumentBuilderFactory createDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            LOG.debug(e.getMessage());
        }
        return factory;
    }

    /**
     * Formats an XML string using 4 spaces indents. XML can have multiple
     * roots (e.g., to format XML fragments).
     * @param xml XML string
     * @return formatted XML
     */
    public static String format(String xml) {
        return format(xml, 4);
    }
    /**
     * Formats an XML string using 4 spaces indents. XML can have multiple
     * roots (e.g., to format XML fragments).
     * @param xml XML string
     * @param indent number of spaces for each indent
     * @return formatted XML
     */
    public static String format(String xml, int indent) {
        return format(xml, indent, 0);
    }
    /**
     * Formats an XML string using the specified number of spaces to indent
     * and optionally wrap long lines of attributes after the number
     * of specified characters. XML can have multiple
     * roots (e.g., to format XML fragments).
     * @param xml XML string
     * @param indent number of spaces for each indent
     * @param wrapAttrAt number of characters after which to wrap long
     *        lines of attributes
     * @return formatted XML
     */
    public static String format(String xml, int indent, int wrapAttrAt) {
        String wrapper = WRAP_START + xml + WRAP_END;
        String x = new XML(wrapper).toString(indent).trim();
        x = StringUtils.removeStart(x, WRAP_START);
        x = StringUtils.removeEnd(x, WRAP_END);
        x = x.replaceAll("(\n\r|\r\n)", "\n");
        x = x.replaceAll("\r", "\n");
        // Remove just one new line char from begining and end
        x = x.replaceAll("(^ *\n| *\n$)", "");

        if (indent > 0) {
            //Remove indent created by adding wrapper tag
            x = x.replaceAll("(?m)^ {" + indent + "}", "");
        }

        // attribute line wrap
        if (wrapAttrAt > 0) {
            String[] lines = x.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() < wrapAttrAt) {
                    continue;
                }
                // check if a tag only and not cdata
                Matcher m = Pattern.compile(
                        "^(\\s*)<[^!/>][^!>]*?>$").matcher(line);
                if (m.matches()) {
                    String wrapIndent = m.group(1) + repeat(' ', indent * 2);
                    line = ATTR_PATTERN.matcher(line).replaceAll(
                            "\n" + wrapIndent + "$1");
                    lines[i] = line;
                }
            }
            x = StringUtils.join(lines, "\n");
        }

        return x;
    }

}
