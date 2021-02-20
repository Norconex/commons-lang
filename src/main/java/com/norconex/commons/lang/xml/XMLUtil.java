/* Copyright 2019-2021 Norconex Inc.
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
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

    private static boolean featureErrorsLogged;
    private static boolean propertyErrorsLogged;

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
            if (propertyErrorsLogged) {
                LOG.debug(e.getMessage());
            }
            propertyErrorsLogged = false;
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
            if (featureErrorsLogged) {
                LOG.debug(e.getMessage());
            }
            featureErrorsLogged = false;
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

    public static SAXParserFactory createSaxParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (SAXNotRecognizedException | SAXNotSupportedException
                | ParserConfigurationException e) {
            LOG.debug(e.getMessage());
        }
        return factory;
    }

    public static XMLInputFactory createXMLInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        return factory;
    }
}
