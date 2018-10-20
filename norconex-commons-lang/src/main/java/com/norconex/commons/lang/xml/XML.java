/* Copyright 2010-2018 Norconex Inc.
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xni.NamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.Converter;
import com.norconex.commons.lang.time.DurationParser;
import com.norconex.commons.lang.time.DurationParserException;
import com.norconex.commons.lang.xml.XMLValidationError.Severity;

//TODO consider checking for a "disable=false|true" and setting it on
//a method if this method exists, and/or do not load if set to true.

/**
 * <p>
 * XML DOM wrapper facilitating node querying and automatically creating,
 * validating, and populating classes from/to XML, with support
 * for {@link IXMLConfigurable}.
 * </p>
 * <h3>XML syntax and white spaces</h3>
 * <p>
 * White spaces in elements should always be preserved.  Empty tags
 * are interpreted as having an empty strings while
 * self-closing tags have their value interpreted as <code>null</code>.
 * Non-existing tags have no effect (when loading over an object, that
 * object current value should remain unchanged).
 * </p>
 * <p>
 * Checked exceptions are wrapped into an {@link XMLException}.
 * </p>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class XML {

    private static final Logger LOG = LoggerFactory.getLogger(XML.class);

    public static final String W3C_XML_SCHEMA_NS_URI_1_1 =
            "http://www.w3.org/XML/XMLSchema/v1.1";

    private static final String DEFAULT_DELIM_REGEX = "(\\s*,\\s*)+";
    private static final String NULL_XML_VALUE = "\u0000";
    private static final List<String> NULL_XML_LIST = new ArrayList<>(0);



    private final Node node;

    /**
     * <p>Parse an XML file into an XML document, without consideration
     * for namespaces.</p>
     * @param file the XML file to parse
     */
    public XML(Path file) {
        this(fileToString(file.toFile()), (DocumentBuilderFactory) null);
    }
    /**
     * <p>Parse an XML file into an XML document, without consideration
     * for namespaces.</p>
     * @param file the XML file to parse
     */
    //TODO really keep this one or have path only?
    public XML(File file) {
        this(fileToString(file), (DocumentBuilderFactory) null);
    }
    /**
     * <p>Parse an XML stream into an XML document, without consideration
     * for namespaces.</p>
     * @param reader the XML stream to parse
     */
    public XML(Reader reader) {
        this(reader, (DocumentBuilderFactory) null);
    }
    /**
     * <p>Parse an XML stream into an XML document, using the provided
     * document builder factory.</p>
     * @param reader the XML stream to parse
     * @param factory the document builder factory
     */
    public XML(Reader reader, DocumentBuilderFactory factory) {
        this(readerToString(reader), factory);
    }

    /**
     * <p>Creates an XML with the given node.</p>
     * @param node the node representing the XML
     */
    public XML(Node node) {
        this.node = node;
    }

    /**
     * <p>
     * Parse an XML string into an XML document, without consideration
     * for namespaces.
     * </p>
     * <p>
     * The supplied "xml" string can either be a well-formed XML or
     * a string without angle brackets. When the later is supplied,
     * it is assumed to be the XML root element name (for a fresh XML).
     * </p>
     * @param xml the XML string to parse
     */
    public XML(String xml) {
        this(xml, (DocumentBuilderFactory) null);
    }

    public XML(String xml, DocumentBuilderFactory factory) {
        String xmlStr = resolveXML(xml);
        if (StringUtils.isBlank(xmlStr)) {
            this.node = null;
            return;
        }

        xmlStr = xmlStr.replaceAll(
                "(<\\s*)([^\\s>]+)([^>]*)(\\s*><\\s*\\/\\s*\\2\\s*>)",
                "$1$2 xml:space=\"empty\"$3$4");
        try {
            DocumentBuilderFactory safeFactory =
                    factory != null ? factory : createDefaultFactory();
            DocumentBuilder builder = safeFactory.newDocumentBuilder();
            this.node = builder.parse(new InputSource(
                    new StringReader(xmlStr))).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new XMLException("Could not parse XML.", e);
        }
    }


    public XML(String rootElement, Object obj) {
        this.node = new XML("<" + rootElement + "/>").node;
        if (obj == null) {
            return;
        }

        if (obj instanceof Class) {
            setAttribute("class", ((Class<?>) obj).getCanonicalName());
        } else if (Converter.defaultInstance().isConvertible(obj.getClass())) {
            setTextContent(Converter.convert(obj));
        } else {
            setAttribute("class", obj.getClass().getCanonicalName());
            if (obj instanceof IXMLConfigurable) {
                ((IXMLConfigurable) obj).saveToXML(this);
            }
        }
    }

    private static String resolveXML(String xml) {
        if (xml == null) {
            return null;
        }
        if (xml.contains("<")) {
            return xml;
        }
        return "<" + xml + "/>";
    }

    public Node toNode() {
        return node;
    }

    //TODO  make it a toObject(Reader, Object... args) method for
    // creating IXMLConfigurable objects with only non-empty constructors?

    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on this XML root node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLException if instance cannot be created/populated
     */
    public <T extends Object> T toObject() {
        return toObject(null);
    }
    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLException if instance cannot be created/populated
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> T toObject(T defaultObject) {
        T obj;
        String clazz;
        if (node == null) {
            return defaultObject;
        }
        clazz = getString("@class");
        if (clazz != null) {
            try {
                obj = (T) ClassUtils.getClass(clazz).newInstance();
            } catch (Exception e) {
                throw new XMLException(
                        "This class could not be instantiated: \""
                        + clazz + "\".", e);
            }
        } else {
            LOG.debug("A configuration entry was found without class "
                   + "reference where one could have been provided; "
                   + "using default value: {}", defaultObject);
            obj = defaultObject;
        }
        if (obj == null) {
            return defaultObject;
        }
        if (obj instanceof IXMLConfigurable) {
            configure((IXMLConfigurable) obj);
        }
        return obj;
    }


    /**
     * <p>Creates a new instance of the class represented by the "class"
     * attribute on the node matching the expression.
     * The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should throw a
     * {@link XMLException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     *
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     */
    public <T extends Object> T getObject(String xpathExpression) {
        return getObject(xpathExpression, (T) null, true);
    }
    /**
     * <p>Creates a new instance of the class represented by the "class"
     * attribute on the node matching the expression.
     * The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should not throw exception upon errors, but will return
     * the default value instead (even if null). Use a method without
     * a default value argument to get exceptions on errors.</p>
     *
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     */
    public <T> T getObject(
            String xpathExpression, T defaultObject) {
        return getObject(xpathExpression, defaultObject, false);
    }
    private <T> T getObject(String xpathExpression,
            T defaultObject, boolean canThrowException) {
        if (node == null) {
            return defaultObject;
        }

        try {
            if (xpathExpression == null && defaultObject == null) {
                return toObject((T) null);
            }
            XML xml = getXML(xpathExpression);
            if (xml == null) {
                return defaultObject;
            }
            return xml.toObject(defaultObject);
        } catch (Exception e) {
            handleException(
                    node.getNodeName(), xpathExpression, e, canThrowException);
            return defaultObject;
        }
    }
    /**
     * <p>Creates an instance list from classes represented by the "class"
     * attribute on the nodes matching the expression.
     * The classes must have an empty constructor.
     * If a class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should not throw exception upon errors, but will return
     * the default value instead (even if null). Use a method without
     * a default value argument to get exceptions on errors.</p>
     *
     * @param xpathExpression xpath expression
     * @param defaultObjects if returned list is empty,
     *        returns this default list.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLException if instance cannot be created/populated
     */
    public <T> List<T> getObjectList(
            String xpathExpression, List<T> defaultObjects) {
        List<T> list = new ArrayList<>();
        List<XML> xmls = getXMLList(xpathExpression);
        for (XML xml : xmls) {
            if (xml != null) {
                list.add(xml.toObject());
            }
        }
        if (list.isEmpty()) {
            return defaultObjects;
        }
        return list;
    }
    /**
     * <p>Creates an instance list from classes represented by the "class"
     * attribute on the nodes matching the expression.
     * The classes must have an empty constructor.
     * If a class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should throw a
     * {@link XMLException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     *
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLException if instance cannot be created/populated
     */
    public <T> List<T> getObjectList(String xpathExpression) {
        return getObjectList(xpathExpression, Collections.emptyList());
    }

    //TODO getElement(s) and getAttribute(s) which are direct references to
    // elements attributes (not xpath)

    /**
     * Gets the xml subset matching the xpath expression.
     * @param xpathExpression expression to match
     * @return XML or <code>null</code> is xpath has no match
     */
    public XML getXML(String xpathExpression) {
        Node xmlNode = getNode(xpathExpression);
        if (xmlNode == null) {
            return null;
        }
        return new XML(xmlNode);
    }
    /**
     * Gets the XML subsets matching the xpath expression.
     * @param xpathExpression expression to match
     * @return XML list, never <code>null</code>
     */
    public List<XML> getXMLList(String xpathExpression) {
        List<XML> list = new ArrayList<>();
        for (Node n : getNodeList(xpathExpression)) {
            list.add(new XML(n));
        }
        return list;
    }

    private static void handleException(
            String rootNode, String key,
            Exception e, boolean canThrowException) {

        // Throw exception
        if (canThrowException) {
            if (e instanceof XMLException) {
                throw (XMLException) e;
            } else {
                throw new XMLException(
                        "Could not instantiate object from configuration "
                      + "for \"" + rootNode + " -> " + key + "\".", e);
            }
        }

        // Log exception
        if (e instanceof XMLException
                && e.getCause() != null) {
            if (e.getCause() instanceof ClassNotFoundException) {
                LOG.error("You declared a class that does not exists for "
                        + "\"{} -> {}\". Check for typos in your "
                        + "XML and make sure that "
                        + "class is part of your Java classpath.",
                        rootNode, key, e);
            } else if (e.getCause() instanceof SAXParseException) {
                String systemId =
                        ((SAXParseException) e.getCause()).getSystemId();
                if (StringUtils.endsWith(systemId, ".xsd")) {
                    LOG.error("XML Schema parsing error for "
                            + "\"{} -> {}\". Schema: {}",
                            rootNode, key, systemId, e);
                } else {
                    LOG.error("XML parsing error for \"{} -> "
                            + "{}\".", rootNode, key, e);
                }
            }
        } else{
            LOG.debug("Could not instantiate object from configuration "
                    + "for \"{} -> \".", rootNode, key, e);
        }
    }

    /**
     * Creates a new {@link Reader} from a {@link Node}.
     * Do not forget to close the reader instance when you are done with it.
     * @return reader
     * @throws XMLException cannot read configuration
     */
    public Reader toReader() {
        return new StringReader(toString());
    }

    /**
     * Gets a string representation of this XML.
     * @return XML string
     * @throws XMLException cannot read configuration
     */
    @Override
    public String toString() {
        return toString(0);
    }
    /**
     * Gets a string representation of this XML.
     * @param indent whether to indent the XML
     * @return XML string
     * @throws XMLException cannot read configuration
     */
    public String toString(int indent) {
        try {
            node.normalize();
            StringWriter w = new StringWriter();
            Result outputTarget = new StreamResult(w);

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer t = factory.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, indent > 0 ? "yes" : "no");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            if (indent > 0) {
                t.setOutputProperty(
                        "{http://xml.apache.org/xslt}indent-amount",
                        Integer.toString(indent));
            }
            t.transform(new DOMSource(node), outputTarget);

            String xmlStr = w.toString();
            // convert self closing tags for empty ones
            return xmlStr.replaceAll(
                    "<\\s*([^\\s>]+)([^>]*) xml:space=\"empty\"([^>]*)/\\s*>",
                    "<$1$2></$1>");
        } catch (TransformerFactoryConfigurationError
                | TransformerException e) {
            throw new XMLException(
                    "Could not convert node to reader "
                  + "for node \"" + node.getNodeName() + "\".", e);
        }
    }

    /**
     * <p>
     * Validates this XML against its attached XSD schema and logs any
     * error/warnings. The root tag has to have a "class" attribute representing
     * an {@link IXMLConfigurable} implementation.
     * The schema expected to be found at the same classpath location and have
     * the same name as the object class, but with the ".xsd" extension.
     * </p>
     * <p>
     * This method is the same as invoking
     * <code>validate(getClass("@class"))</code>
     * </p>
     * @return list of errors/warnings or empty (never <code>null</code>)
     */
    public List<XMLValidationError> validate() {
        return validate(getClass("@class"));
    }

    /**
     * <p>
     * Validates this XML for objects implementing {@link IXMLConfigurable}
     * and having an XSD schema attached, and logs any error/warnings.
     * The schema expected to be found at the same classpath location and have
     * the same name as the object class, but with the ".xsd" extension.
     * </p>
     * <p>
     * This method is the same as invoking <code>validate(obj.getClass())</code>
     * </p>
     * @param obj the object to validate the XML for
     * @return list of errors/warnings or empty (never <code>null</code>)
     */
    public List<XMLValidationError> validate(Object obj) {
        return validate(obj == null ? null : obj.getClass());
    }

    /**
     * Validates this XML for classes implementing {@link IXMLConfigurable}
     * and having an XSD schema attached, and logs any error/warnings.
     * The schema expected to be found at the same classpath location and have
     * the same name as the object class, but with the ".xsd" extension.
     * @param clazz the class to validate the XML for
     * @return list of errors/warnings or empty (never <code>null</code>)
     */
    public List<XMLValidationError> validate(Class<?> clazz) {

        List<XMLValidationError> errors = new ArrayList<>();

        // Only validate if IXMLConfigurable
        if (clazz == null || !IXMLConfigurable.class.isAssignableFrom(clazz)) {
            return errors;
        }

        // Only validate if .xsd file exist in classpath for class
        String xsdResource = ClassUtils.getSimpleName(clazz) + ".xsd";
        LOG.debug("Class to validate: {}", ClassUtils.getSimpleName(clazz));
        if (clazz.getResource(xsdResource) == null) {
            LOG.debug("Resource not found for validation: {}", xsdResource);
            return errors;
        }

        // Go ahead: validate
        SchemaFactory schemaFactory =
                SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI_1_1);
        schemaFactory.setResourceResolver(new ClasspathResourceResolver(clazz));

        try (InputStream xsdStream = clazz.getResourceAsStream(xsdResource);
                Reader reader = toReader()) {
            Schema schema = schemaFactory.newSchema(
                    new StreamSource(xsdStream, getXSDResourcePath(clazz)));
            Validator validator = schema.newValidator();
            LogErrorHandler seh = new LogErrorHandler(clazz, errors);
            validator.setErrorHandler(seh);
            SAXSource saxSource = new SAXSource(new W3XMLNamespaceFilter(
                    XMLReaderFactory.createXMLReader()),
                            new InputSource(reader));
            validator.validate(saxSource);
            return errors;
        } catch (SAXException | IOException e) {
            throw new XMLException("Could not validate class: " + clazz, e);
        }
    }

    /**
     * Configures the given object by invoking its
     * {@link IXMLConfigurable#loadFromXML(XML)} method.
     * @param obj object to have loaded
     * @return list of errors/warnings or empty (never <code>null</code>)
     */
    public List<XMLValidationError> configure(IXMLConfigurable obj) {
        if (obj == null || node == null) {
            return Collections.emptyList();
        }
        List<XMLValidationError> errors = validate(obj.getClass());
        obj.loadFromXML(this);
        return errors;
    }
    //TODO have a static version of configure that also takes a file?

    /**
     * Convenience class for testing that a {@link IXMLConfigurable} instance
     * can be written, and read into an new instance that is equal as per
     * {@link #equals(Object)}.
     * @param xmlConfigurable the instance to test if it writes/read properly
     * @param elementName the tag name of the root element being written
     * @throws XMLException Cannot save/load configuration
     */
    public static void assertWriteRead(
            IXMLConfigurable xmlConfigurable, String elementName) {

        LOG.debug("Writing/Reading this: {}", xmlConfigurable);

        // Write
        String xmlStr;
        try (StringWriter out = new StringWriter()) {
            XML xml = new XML(elementName, xmlConfigurable);
            xml.write(out);
            xmlStr = out.toString();
        } catch (IOException e) {
            throw new XMLException("Could not save XML.", e);
        }
        LOG.trace(xmlStr);

        // Read
        XML xml = new XML(xmlStr);
        IXMLConfigurable readConfigurable = xml.toObject();
        if (!xmlConfigurable.equals(readConfigurable)) {
            LOG.error("BEFORE: {}", xmlConfigurable);
            LOG.error(" AFTER: {}", readConfigurable);
            throw new XMLException("Saved and loaded XML are not the same.");
        }
    }

    public boolean contains(String xpathExpression) {
        try {
            return newXPathExpression(xpathExpression).evaluate(
                    node, XPathConstants.NODE) != null;
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate expression: " + xpathExpression, e) ;
        }
    }

    /**
     * Gets a list of strings after splitting the matching node value(s)
     * on commas (CSV).
     * Values are trimmed and blank entries removed.
     * Commas can have any spaces before or after.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @return list of strings, never <code>null</code>
     */
    public List<String> getDelimitedStringList(String xpathExpression) {
        List<String> values =
                getDelimitedStringList(xpathExpression, (List<String>) null);
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }
    /**
     * Gets a list of strings after splitting the matching node value(s)
     * on commas (CSV).
     * Values are trimmed and blank entries removed.
     * Commas can have any spaces before or after.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param defaultValues default values if the split returns
     *        <code>null</code> or an empty list
     * @return list of strings
     */
    public List<String> getDelimitedStringList(
            String xpathExpression, List<String> defaultValues) {
        return getDelimitedStringList(
                xpathExpression, DEFAULT_DELIM_REGEX, defaultValues);
    }
    /**
     * Gets a list of strings after splitting the matching node value(s) with
     * the given delimiter regular expression. Values are trimmed
     * before being split and blank entries removed.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param delimRegex regular expression matching split delimiter
     * @return list of strings, never <code>null</code>
     */
    public List<String> getDelimitedStringList(
            String xpathExpression, String delimRegex) {
        List<String> values =
                getDelimitedStringList(xpathExpression, delimRegex, null);
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }
    /**
     * Gets a list of strings after splitting the matching node value(s) with
     * the given delimiter regular expression. Values are trimmed
     * and blank entries removed.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param delimRegex regular expression matching split delimiter
     * @param defaultValues default values if the split returns
     *        <code>null</code> or an empty list
     * @return list of strings
     */
    public List<String> getDelimitedStringList(String xpathExpression,
            String delimRegex, List<String> defaultValues) {

        if (!contains(xpathExpression)) {
            return defaultValues;
        }

        List<String> delimList = getStringList(xpathExpression, NULL_XML_LIST);
        if (delimList == null) {
            return defaultValues;
        }
        if (delimList.isEmpty() || delimList == NULL_XML_LIST) {
            return Collections.emptyList();
        }

        List<String> splitList = new ArrayList<>();
        for (String str : delimList) {
            List<String> values = split(str, delimRegex);
            if (CollectionUtils.isEmpty(values)) {
                continue;
            }
            for (String val : values) {
                String trimmed = StringUtils.trimToNull(val);
                if (trimmed != null) {
                    splitList.add(trimmed);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(splitList)) {
            return splitList;
        }
        return Collections.emptyList();
    }
    private List<String> split(String str, String delimRegex) {
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        return Arrays.asList(str.trim().split(delimRegex));
    }

    public String join(String delim, List<?> values) {
        String sep = Objects.toString(delim, ",");
        StringBuilder b = new StringBuilder();
        for (Object obj : values) {
            String str = Objects.toString(obj, "").trim();
            if (StringUtils.isNotEmpty(str)) {
                if (b.length() > 0) {
                    b.append(sep);
                }
                b.append(str);
            }
        }
        return b.toString();
    }

    /**
     * Gets a list of the given type after splitting the matching node value(s)
     * on commas (CSV).
     * Values are trimmed and blank entries removed before attempting
     * to convert them to given type.
     * Commas can have any spaces before or after.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param type target list type
     * @param <T> returned list type
     * @return list of given type, never <code>null</code>
     */
    public <T> List<T> getDelimitedList(
            String xpathExpression, Class<T> type) {
        return getDelimitedList(xpathExpression, type, Collections.emptyList());
    }
    /**
     * Gets a list of given type after splitting the matching node value(s)
     * on commas (CSV).
     * Values are trimmed and blank entries removed before attempting
     * to convert them to given type.
     * Commas can have any spaces before or after.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param type target list type
     * @param defaultValues default values if the split returns
     *        <code>null</code> or an empty list
     * @param <T> returned list type
     * @return list of strings
     */
    public <T> List<T> getDelimitedList(
            String xpathExpression, Class<T> type, List<T> defaultValues) {
        return getDelimitedList(
                xpathExpression, type, DEFAULT_DELIM_REGEX, defaultValues);
    }
    /**
     * Gets a list of given type after splitting the matching node value(s) with
     * the given delimiter regular expression.
     * Values are trimmed and blank entries removed before attempting
     * to convert them to given type.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param type target list type
     * @param delimRegex regular expression matching split delimiter
     * @param <T> returned list type
     * @return list of strings, never <code>null</code>
     */
    public <T> List<T> getDelimitedList(
            String xpathExpression, Class<T> type, String delimRegex) {
        return getDelimitedList(
                xpathExpression, type, delimRegex, Collections.emptyList());
    }
    /**
     * Gets a list of given type after splitting the matching node value(s) with
     * the given delimiter regular expression.
     * Values are trimmed and blank entries removed before attempting
     * to convert them to given type.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param type target list type
     * @param delimRegex regular expression matching split delimiter
     * @param defaultValues default values if the split returns
     *        <code>null</code> or an empty list
     * @param <T> returned list type
     * @return list of strings
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getDelimitedList(String xpathExpression, Class<T> type,
            String delimRegex, List<? extends T> defaultValues) {
        if (!contains(xpathExpression)) {
            return (List<T>) defaultValues;
        }

        List<String> values = getDelimitedStringList(
                xpathExpression, delimRegex, NULL_XML_LIST);
        if (values == null) {
            return (List<T>) defaultValues;
        }
        if (values.isEmpty() || values == NULL_XML_LIST) {
            return Collections.emptyList();
        }
        return CollectionUtil.toTypeList(values, type);

    }

    private static String getXSDResourcePath(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return "/" + clazz.getCanonicalName().replace('.', '/') + ".xsd";
    }

    private static class LogErrorHandler implements ErrorHandler {
        private final Class<?> clazz;
        private final List<XMLValidationError> errors;
        public LogErrorHandler(
                Class<?> clazz, List<XMLValidationError> errors) {
            super();
            this.clazz = clazz;
            this.errors = errors;
        }
        @Override
        public void warning(SAXParseException e) throws SAXException {
            String msg = msg(e);
            errors.add(new XMLValidationError(Severity.WARNING, msg));
            LOG.warn(msg);
        }
        @Override
        public void error(SAXParseException e) throws SAXException {
            String msg = msg(e);
            errors.add(new XMLValidationError(Severity.ERROR, msg));
            LOG.error(msg);
        }
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            String msg = msg(e);
            errors.add(new XMLValidationError(Severity.FATAL, msg));
            LOG.error(msg);
        }
        private String msg(SAXParseException e) {
            return "(XML Validation) "
                    + clazz.getSimpleName() + ": " + e.getMessage();
        }
    }

    // Filter out "xml:" name space so attributes like xml:space="preserve"
    // are validated OK.
    private static class W3XMLNamespaceFilter extends XMLFilterImpl {
        public W3XMLNamespaceFilter(XMLReader parent) {
            super(parent);
        }
        @Override
        public void startElement(
                String uri, String localName, String qName, Attributes atts)
                        throws SAXException {
            for (int i = 0; i < atts.getLength(); i++) {
                if (NamespaceContext.XML_URI.equals(atts.getURI(i))) {
                    AttributesImpl modifiedAtts = new AttributesImpl(atts);
                    modifiedAtts.removeAttribute(i);
                    super.startElement(uri, localName, qName, modifiedAtts);
                    return;
                }
            }
            super.startElement(uri, localName, qName, atts);
        }
    }

    private static DocumentBuilderFactory createDefaultFactory() {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setIgnoringElementContentWhitespace(false);
        return factory;
    }

    public static XPath newXPath() {
        //TODO consider caching w/ ThreadLocal if performance becomes a concern
        XPathFactory xpathFactory = XPathFactory.newInstance();
        return xpathFactory.newXPath();
    }
    public static XPathExpression newXPathExpression(String expression) {
        try {
            return newXPath().compile(expression);
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not create XPath expression.", e);
        }
    }

    public NodeArrayList getNodeList(String xpathExpression) {
        try {
            return new NodeArrayList((NodeList) newXPathExpression(
                    xpathExpression).evaluate(node, XPathConstants.NODESET));
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate XPath expression.", e);
        }
    }
    public Node getNode(String xpathExpression) {
        return getNode(xpathExpression, node);
    }
    public Node getNode() {
        return node;
    }
    private Node getNode(String xpathExpression, Node parentNode) {
        try {
            return (Node) newXPathExpression(xpathExpression).evaluate(
                    parentNode, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate XPath expression.", e);
        }
    }

    public String getString(String xpathExpression) {
        return getString(xpathExpression, null);
    }
    public String getString(String xpathExpression, String defaultValue) {
        Node n = getNode(xpathExpression);
        if (n == null) {
            return defaultValue;
        }
        return getNodeString(n);
    }
    /**
     * Gets the matching list of elements/attributes as strings.
     * @param xpathExpression XPath expression to the node values
     * @return list of strings, never <code>null</code>
     */
    public List<String> getStringList(String xpathExpression) {
        List<String> list = getStringList(xpathExpression, null);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }
    /**
     * Gets the matching list of elements/attributes as strings.
     * @param xpathExpression XPath expression to the node values
     * @param defaultValues default values if the expression does not match
     *        anything.
     * @return list of strings
     */
    public List<String> getStringList(
            String xpathExpression, List<String> defaultValues) {
        NodeArrayList nodeList = getNodeList(xpathExpression);
        if (nodeList.isEmpty()) {
            return defaultValues;
        }
        List<String> list = new ArrayList<>();
        for (Node n : nodeList) {
            String str = getNodeString(n);
            if (str != null) {
                list.add(str);
            }
        }
        return list;
    }

    /**
     * Gets the matching element/attribute, converted from
     * string to the given type.
     * @param xpathExpression XPath expression to the node value
     * @param type target class type of returned value
     * @param <T> target type
     * @return object of given type
     */
    public <T> T get(String xpathExpression, Class<T> type) {
        return get(xpathExpression, type, null);
    }
    /**
     * Gets the matching element/attribute, converted from
     * string to the given type.
     * @param xpathExpression XPath expression to the node value
     * @param type target class type of returned value
     * @param defaultValue default value if the expression returns
     *        <code>null</code>
     * @param <T> target type
     * @return object of given type
     */
    public <T> T get(String xpathExpression, Class<T> type, T defaultValue) {
        String value = getString(xpathExpression, NULL_XML_VALUE);
        if (value == null) {
            return null;
        }
        if (value.equals(NULL_XML_VALUE)) {
            return defaultValue;
        }
        return Converter.convert(value, type, defaultValue);
    }

    /**
     * Gets the matching list of elements/attributes, converted from
     * string to the given type.
     * @param xpathExpression XPath expression to the node values
     * @param type target class type of returned list
     * @param <T> returned list type
     * @return list of given type, never <code>null</code>
     */
    public <T> List<? extends T> getList(
            String xpathExpression, Class<T> type) {
        return getList(xpathExpression, type, Collections.emptyList());
    }
    /**
     * Gets the matching list of elements/attributes, converted from
     * string to the given type.
     * @param xpathExpression XPath expression to the node values
     * @param type target class type of returned list
     * @param defaultValues default values if the expression returns
     *        <code>null</code> or an empty list
     * @param <T> returned list type
     * @return list of given type
     */
    public <T> List<? extends T> getList(String xpathExpression,
            Class<T> type, List<? extends T> defaultValues) {
        List<String> list = getStringList(xpathExpression, null);
        if (list == null) {
            return defaultValues;
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return CollectionUtil.toTypeList(list, type);
    }

    /**
     * Gets the matching map of elements/attributes as strings.
     * @param xpathList XPath expression to the node list representing the map
     * @param xpathKey XPath expression to a node key
     * @param xpathValue XPath expression to a node value
     * @return map of strings, never <code>null</code>
     */
    public Map<String, String> getStringMap(
            String xpathList, String xpathKey, String xpathValue) {
        Map<String, String> map = getStringMap(
                xpathList, xpathKey, xpathValue, null);
        if (MapUtils.isEmpty(map)) {
            return Collections.emptyMap();
        }
        return map;
    }
    /**
     * Gets the matching map of elements/attributes as strings.
     * @param xpathList XPath expression to the node list representing the map
     * @param xpathKey XPath expression to a node key
     * @param xpathValue XPath expression to a node value
     * @param defaultValues default values if the expressions return
     *        <code>null</code> or an empty map
     * @return map of strings, never <code>null</code>
     */
    public Map<String, String> getStringMap(String xpathList, String xpathKey,
            String xpathValue, Map<String, String> defaultValues) {
        Map<String, String> map = new HashMap<>();
        List<XML> xmls = getXMLList(xpathList);
        for (XML xml : xmls) {
            if (xml != null) {
                map.put(xml.getString(xpathKey), xml.getString(xpathValue));
            }
        }
        if (map.isEmpty()) {
            return defaultValues;
        }
        return map;
    }

    public Integer getInteger(String xpathExpression) {
        return get(xpathExpression, Integer.class);
    }
    public Integer getInteger(String xpathExpression, Integer defaultValue) {
        return get(xpathExpression, Integer.class, defaultValue);
    }

    public Long getLong(String xpathExpression) {
        return get(xpathExpression, Long.class);
    }
    public Long getLong(String xpathExpression, Long defaultValue) {
        return get(xpathExpression, Long.class, defaultValue);
    }

    public Float getFloat(String xpathExpression) {
        return get(xpathExpression, Float.class);
    }
    public Float getFloat(String xpathExpression, Float defaultValue) {
        return get(xpathExpression, Float.class, defaultValue);
    }

    public Dimension getDimension(String xpathExpression) {
        return get(xpathExpression, Dimension.class);
    }
    public Dimension getDimension(
            String xpathExpression, Dimension defaultValue) {
        return get(xpathExpression, Dimension.class, defaultValue);
    }

    public Double getDouble(String xpathExpression) {
        return get(xpathExpression, Double.class);
    }
    public Double getDouble(String xpathExpression, Double defaultValue) {
        return get(xpathExpression, Double.class, defaultValue);
    }

    public Boolean getBoolean(String xpathExpression) {
        return get(xpathExpression, Boolean.class);
    }
    public Boolean getBoolean(String xpathExpression, Boolean defaultValue) {
        return get(xpathExpression, Boolean.class, defaultValue);
    }

    public Locale getLocale(String xpathExpression) {
        return get(xpathExpression, Locale.class);
    }
    public Locale getLocale(String xpathExpression, Locale defaultValue) {
        return get(xpathExpression, Locale.class, defaultValue);
    }

    public Charset getCharset(String xpathExpression) {
        return get(xpathExpression, Charset.class);
    }
    public Charset getCharset(String xpathExpression, Charset defaultValue) {
        return get(xpathExpression, Charset.class, defaultValue);
    }

    /**
     * Gets a duration in milliseconds which can exists as a numerical
     * value or a textual
     * representation of a duration as per {@link DurationParser}.
     * If the key value is found but there are parsing errors, a
     * {@link DurationParserException} will be thrown.
     * @param xpathExpression xpath to the element/attribute containing the
     *        duration
     * @return duration in milliseconds
     */
    public Long getDurationMillis(String xpathExpression) {
        return getDurationMillis(xpathExpression, null);
    }
    /**
     * Gets a duration in milliseconds which can exists as a numerical
     * value or a textual
     * representation of a duration as per {@link DurationParser}.
     * If the key value is found but there are parsing errors, a
     * {@link DurationParserException} will be thrown.
     * @param xpathExpression xpath to the element/attribute containing the
     *        duration
     * @param defaultValue default duration
     * @return duration in milliseconds
     */
    public Long getDurationMillis(String xpathExpression, Long defaultValue) {
        Duration d = getDuration(xpathExpression);
        return d == null ? defaultValue : d.toMillis();
    }
    /**
     * Gets a duration which can exists as a numerical
     * value or a textual
     * representation of a duration as per {@link DurationParser}.
     * If the duration does not exists for the given key or is blank,
     * <code>null</code> is returned.
     * If the key value is found but there are parsing errors, a
     * {@link DurationParserException} will be thrown.
     * @param xpathExpression xpath to the element/attribute containing the
     *        duration
     * @return duration
     */
    public Duration getDuration(String xpathExpression) {
        return get(xpathExpression, Duration.class);
    }
    /**
     * Gets a duration which can exists as a numerical
     * value or a textual
     * representation of a duration as per {@link DurationParser}.
     * If the duration does not exists for the given key or is blank,
     * the default value is returned.
     * If the key value is found but there are parsing errors, a
     * {@link DurationParserException} will be thrown.
     * @param xpathExpression xpath to the element/attribute containing the
     *        duration
     * @param defaultValue default duration
     * @return duration
     */
    public Duration getDuration(
            String xpathExpression, Duration defaultValue) {
        return get(xpathExpression, Duration.class, defaultValue);
    }

    public String getName() {
        return node.getNodeName();
    }

    //TODO addElementFirst
    //TODO addElementLast

    /**
     * Adds an empty child element to this XML root element.
     * @param tagName element name
     * @return XML of the added element
     */
    public XML addElement(String tagName) {
        return addElement(tagName, null);
    }

    /**
     * <p>
     * Adds a child element to this XML root element.
     * If the element value is blank, and empty element is created.
     * Otherwise, the value is handled as
     * {@link #XML(String, Object)}
     * @param tagName element name
     * @param value element value
     * @return XML of the added element or <code>null</code> if value is
     *         <code>null</code>
     */
    public XML addElement(String tagName, Object value) {
        XML xml = new XML(tagName, value);
        Node newNode = node.getOwnerDocument().importNode(xml.node, true);
        return new XML(node.appendChild(newNode));
    }

    public List<XML> addElementList(String tagName, List<?> values) {
        return addElementList(null, tagName, values);
    }
    public List<XML> addElementList(
            String parentTagName, String tagName, List<?> values) {

        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        XML parentXML = this;
        boolean hasParent = StringUtils.isNotBlank(parentTagName);
        if (hasParent) {
            parentXML = addElement(parentTagName);
        }
        List<XML> xmlList = new ArrayList<>();
        for (Object value : values) {
            xmlList.add(parentXML.addElement(tagName, value));
        }
        return Collections.unmodifiableList(xmlList);
    }

    /**
     * Sets a list of values as a new element after joining them with
     * a comma (CSV). Values are trimmed and blank entries removed.
     * Values can be of any types, as they converted to String by
     * invoking their "toString()" method.
     * @param name attribute name
     * @param values attribute values
     * @return the newly added element
     */
    public XML addDelimitedElementList(String name, List<?> values) {
        return addDelimitedElementList(name, ",", values);
    }
    /**
     * Sets a list of values as a new element after joining them with
     * the given delimiter. Values are trimmed and blank entries removed.
     * Values can be of any types, as they converted to String by
     * invoking their "toString()" method.
     * @param name attribute name
     * @param delim delimiter
     * @param values attribute values
     * @return the newly added element
     */
    public XML addDelimitedElementList(
            String name, String delim, List<?> values) {
        if (values.isEmpty()) {
            return addElement(name, "");
        }
        return addElement(name, join(delim, values));
    }

    /**
     * Sets an attribute on this XML element, converting the supplied object
     * to a string (enums are also converted to lowercase).
     * A <code>null</code> value is equivalent to not
     * adding or removing that attribute.
     * @param name attribute name
     * @param value attribute value
     * @return this element
     */
    public XML setAttribute(String name, Object value) {
        //TODO check if not a node, throw exception
        Element el = (Element) node;
        if (value == null) {
            el.removeAttribute(name);
        } else if (Converter.defaultInstance().isConvertible(
                value.getClass())) {
            el.setAttribute(name, Converter.convert(value));
        } else {
            el.setAttribute(name, value.toString());
        }
        return this;
    }
    /**
     * Sets attributes on this XML element.
     * @param attribs attributes
     * @return this element
     */
    public XML setAttributes(Map<String, ?> attribs) {
        //TODO check if not a node, throw exception
        if (MapUtils.isNotEmpty(attribs)) {
            for (Entry<String, ?> en : attribs.entrySet()) {
                setAttribute(en.getKey(), en.getValue());
            }
        }
        return this;
    }
    /**
     * Sets a list of values as an attribute after joining them with
     * a comma (CSV). Values are trimmed and blank entries removed.
     * Values can be of any types, as they converted to String by
     * invoking their "toString()" method.
     * @param name attribute name
     * @param values attribute values
     * @return this element
     */
    public XML setDelimitedAttributeList(String name, List<?> values) {
        return setDelimitedAttributeList(name, ",", values);
    }
    /**
     * Sets a list of values as an attribute after joining them with
     * the given delimiter. Values are trimmed and blank entries removed.
     * Values can be of any types, as they converted to String by
     * invoking their "toString()" method.
     * @param name attribute name
     * @param delim delimiter
     * @param values attribute values
     * @return this element
     */
    public XML setDelimitedAttributeList(
            String name, String delim, List<?> values) {
        if (values.isEmpty()) {
            return this;
        }
        setAttribute(name, join(delim, values));
        return this;
    }

    /**
     * Sets the text content of an XML element.
     * @param textContent text content
     * @return this element
     */
    public XML setTextContent(Object textContent) {
        String content = Objects.toString(textContent, null);

        Element el = (Element) node;
        el.removeAttribute("xml:space");
        if ("".equals(content)) {
            if (node instanceof Element) {
                el.setAttribute("xml:space", "empty");
            }
        } else if (content != null) {
            node.setTextContent(content);
        }
        return this;
    }

    // returns the newly added XML
    public XML addXML(Reader xml) {
        return addXML(new XML(xml));
    }
    // returns the newly added XML
    public XML addXML(String xml) {
        return addXML(new XML(xml));
    }
    // returns the newly added XML
    public XML addXML(XML xml) {
        Node childNode = node.getOwnerDocument().importNode(xml.node, true);
        node.appendChild(childNode);
        return new XML(childNode);
    }

    public Writer getXMLWriter() {
        return new StringWriter() {
            @Override
            public void close() throws IOException {
                String s = this.toString();
                if (StringUtils.isNotBlank(s)) {
                    addXML(s);
                }
            }
        };
    }
    public EnhancedXMLStreamWriter getXMLStreamWriter() {
        return new EnhancedXMLStreamWriter(getXMLWriter());
    }

    public void write(Writer writer) {
        write(writer, 0);
    }
    public void write(Writer writer, int indent) {
        try {
            writer.write(toString(indent));
        } catch (IOException e) {
            throw new XMLException("Could not write XML to Writer.", e);
        }
    }

    public void write(File file) {
        write(file, 0);
    }
    public void write(File file, int indent) {
        try {
            FileUtils.writeStringToFile(
                    file, toString(indent), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new XMLException(
                    "Could not write XML to file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Unwraps this XML by removing the root tag and keeping its child element
     * (and its nested element).
     * If there are no child (i.e., nothing to unwrap), invoking
     * this method has no effect.
     * If there are more than one child element, this method throws an
     * {@link XMLException}.
     * @return this XML, unwrapped
     */
    public XML unwrap() {
        NodeList children = node.getChildNodes();

        // If no child, end here
        if (children == null || children.getLength() == 0) {
            return this;
        }

        // If multiple children, throw exception
        if (children.getLength() > 1) {
            //TODO maybe support XML made of lists?
            throw new XMLException("Cannot unwrap " + getName()
                    + " element as it contains multiple child elements.");
        }

        // Proceed with the unwrapping
        replace(new XML(children.item(0)));
        return this;
    }

    /**
     * Wraps this XML by adding a parent element around it.
     * @param parentName name of wrapping element
     * @return this XML, wrapped
     */
    public XML wrap(String parentName) {
        Document doc = node.getOwnerDocument();
        Node childNode = node.cloneNode(true);
        clear();
        doc.renameNode(node, null, parentName);
        node.appendChild(childNode);
        return this;
    }

    /**
     * Clears this XML by removing all its attributes and elements
     * (i.e., making it an empty tag).
     * @return this cleared XML
     */
    public XML clear() {
        // clear child nodes
        while (node.hasChildNodes()) {
            node.removeChild(node.getFirstChild());
        }
        // clear attributes
        while (node.getAttributes().getLength() > 0) {
            Node att = node.getAttributes().item(0);
            node.getAttributes().removeNamedItem(att.getNodeName());
        }
        return this;
    }

    /**
     * Replaces the current XML with the provided one.
     * @param replacement replacing XML
     * @return this XML, replaced
     */
    public XML replace(XML replacement) {
        clear();
        Document doc = node.getOwnerDocument();

        // overwrite parent node with child one
        NamedNodeMap attrs = replacement.node.getAttributes();
        for (int i=0; i < attrs.getLength(); i++) {
            node.getAttributes().setNamedItem(
                    doc.importNode(attrs.item(i), true));
        }
        while (replacement.node.hasChildNodes()) {
            Node childNode = replacement.node.removeChild(
                    replacement.node.getFirstChild());
            node.appendChild(doc.importNode(childNode, true));
        }
        doc.renameNode(node, null, replacement.node.getNodeName());
        return this;
    }

    //--- Enum -----------------------------------------------------------------
    /**
     * Gets an Enum constant matching one of the constants in the provided
     * Enum class, ignoring case.
     * @param xpathExpression XPath expression to the enum value.
     * @param enumClass target enum class
     * @param <E> enum type
     * @return an enum value or <code>null</code> if no values are matching.
     */
    public final <E extends Enum<E>> E getEnum(
            String xpathExpression, Class<E> enumClass) {
        return get(xpathExpression, enumClass);
//        return getEnum(xpathExpression, enumClass, null);
    }
    /**
     * Gets an Enum constant matching one of the constants in the provided
     * Enum class, ignoring case.
     * @param xpathExpression XPath expression to the enum value.
     * @param enumClass target enum class
     * @param defaultValue defaultValue
     * @param <E> enum type
     * @return an enum value or default value if no values are matching.
     */
    public final <E extends Enum<E>> E getEnum(
            String xpathExpression, Class<E> enumClass, E defaultValue) {
        return get(xpathExpression, enumClass, defaultValue);
//        Objects.requireNonNull(enumClass, "enumClass must not be null");
//        return toEnum(getString(xpathExpression), enumClass, defaultValue);
    }

    /**
     * Gets a list of enum constants.
     * Values are trimmed and blank entries removed before attempting
     * to convert them to the given enum type.
     * @param xpathExpression XPath expression
     * @param enumClass target enum class
     * @param defaultValues default values
     * @param <E> enum type
     * @return list of enums
     */
    public <E extends Enum<E>> List<E> getEnumList(
            String xpathExpression, Class<E> enumClass, List<E> defaultValues) {
        return getDelimitedList(xpathExpression, enumClass);
//        List<String> values =
//                getStringList(xpathExpression, (List<String>) null);
//        if (values == null) {
//            return defaultValues;
//        }
//        return values.stream().map(str ->
//            toEnum(str, enumClass, null)).collect(Collectors.toList());
    }

    /**
     * Gets a list of enum constants after splitting the matching node value(s)
     * on commas (CSV).
     * Values are trimmed and blank entries removed before attempting
     * to convert them to the given enum type.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param enumClass target enum class
     * @param defaultValues default values if the split returns
     *        <code>null</code> or an empty list
     * @param <E> enum type
     * @return list of enums
     */
    public <E extends Enum<E>> List<E> getDelimitedEnumList(
            String xpathExpression, Class<E> enumClass, List<E> defaultValues) {
        return getDelimitedList(
                xpathExpression, enumClass, defaultValues);
//        List<String> values =
//                getDelimitedStringList(xpathExpression, (List<String>) null);
//        if (values == null) {
//            return defaultValues;
//        }
//        return values.stream().map(str ->
//            toEnum(str, enumClass, null)).collect(Collectors.toList());
    }

    /**
     * Gets a list of enum constants after splitting the matching node
     * value(s) with the given delimiter regular expression.
     * Values are trimmed and blank entries removed before attempting
     * to convert them to given enum type.
     * @param xpathExpression XPath expression to the node value(s) to split
     * @param enumClass target enum class
     * @param delimRegex regular expression matching split delimiter
     * @param defaultValues default values if the split returns
     *        <code>null</code> or an empty list
     * @param <E> enum type
     * @return list of enums
     */
    public <E extends Enum<E>> List<E> getDelimitedEnumList(
            String xpathExpression, Class<E> enumClass,
            String delimRegex, List<E> defaultValues) {
        return getDelimitedList(
                xpathExpression, enumClass, delimRegex, defaultValues);
    }

    //--- Path -----------------------------------------------------------------
    /**
     * Gets a path, assuming the node value is a file system path.
     * @param xpathExpression XPath expression to the node containing the path
     * @return a path
     */
    public final Path getPath(String xpathExpression) {
        return get(xpathExpression, Path.class);
    }
    /**
     * Gets a path, assuming the node value is a file system path.
     * @param xpathExpression XPath expression to the node containing the path
     * @param defaultValue default path being returned if no path has been
     *        defined for the given expression.
     * @return a path
     */
    public final Path getPath(String xpathExpression, Path defaultValue) {
        return get(xpathExpression, Path.class, defaultValue);
    }
    /**
     * Gets values as a list of paths.
     * @param xpathExpression XPath expression
     * @return the values
     */
    @SuppressWarnings("unchecked")
    public final List<Path> getPathList(String xpathExpression) {
        return (List<Path>) getList(xpathExpression, Path.class);
    }
    /**
     * Gets values as a list of paths.
     * @param xpathExpression XPath expression
     * @param defaultValue default value
     * @return the values
     */
    @SuppressWarnings("unchecked")
    public final List<Path> getPathList(
            String xpathExpression, List<Path> defaultValue) {
        return (List<Path>) getList(xpathExpression, Path.class, defaultValue);
    }


    //--- File -----------------------------------------------------------------
    /**
     * Gets a file, assuming the node value is a file system path.
     * @param xpathExpression XPath expression to the node containing the path
     * @return a File
     */
    public final File getFile(String xpathExpression) {
        return get(xpathExpression, File.class);
    }
    /**
     * Gets a file, assuming the node value is a file system path.
     * @param xpathExpression XPath expression to the node containing the path
     * @param defaultValue default file being returned if no file has been
     *        defined for the given expression.
     * @return a File
     */
    public final File getFile(String xpathExpression, File defaultValue) {
        return get(xpathExpression, File.class, defaultValue);
    }
    /**
     * Gets values as a list of files.
     * @param xpathExpression XPath expression
     * @return the values
     */
    @SuppressWarnings("unchecked")
    public final List<File> getFileList(String xpathExpression) {
        return (List<File>) getList(xpathExpression, File.class);
    }
    /**
     * Gets values as a list of files.
     * @param xpathExpression XPath expression
     * @param defaultValue default value
     * @return the values
     */
    @SuppressWarnings("unchecked")
    public final List<File> getFileList(
            String xpathExpression, List<File> defaultValue) {
        return (List<File>) getList(xpathExpression, File.class, defaultValue);
    }

    private static String readerToString(Reader reader) {
        try {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            throw new XMLException("Could not read XML.", e);
        }
    }
    private static String fileToString(File file) {
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new XMLException(
                    "Could not read XML file: " + file.getAbsolutePath(), e);
        }
    }

    private String getNodeString(Node n) {
        if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
            return n.getNodeValue();
        }
        String str = n.getTextContent();
        if (StringUtils.isEmpty(str)) {
            Node attr = n.getAttributes().getNamedItem("xml:space");
            if (attr != null && "empty".equalsIgnoreCase(attr.getNodeValue())) {
                return "";
            }
            return null;
        }
        return str;
    }

    //TODO isDisabled (which also reads disable, ignore, ignored
    // and give warnings when not "disabled" (or rely on validation, changing all ignore to "disabled"

    public Class<?> getClass(String xpathExpression) {
        return get(xpathExpression, Class.class);
    }
    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(
            String xpathExpression, Class<T> defaultValue) {
        return get(xpathExpression, Class.class, defaultValue);
    }
    /**
     * Gets values as a list of files.
     * @param xpathExpression XPath expression
     * @param <T> returned list type
     * @return the values
     */
    @SuppressWarnings("unchecked")
    public final <T> List<Class<T>> getClassList(String xpathExpression) {
        return (List<Class<T>>) getList(xpathExpression, Class.class);
    }
    /**
     * Gets values as a list of files.
     * @param xpathExpression XPath expression
     * @param defaultValue default value
     * @param <T> returned list type
     * @return the values
     */
    @SuppressWarnings("unchecked")
    public final <T> List<Class<? extends T>> getClassList(
            String xpathExpression, List<Class<? extends T>> defaultValue) {
        return (List<Class<? extends T>>) getList(
                xpathExpression, Class.class, defaultValue);
    }

    public <T> T parseXML(
            String xpathExpression, Function<XML, T> parser) {
        return parseXML(xpathExpression, parser, null);
    }
    public <T> T parseXML(
            String xpathExpression,
            Function<XML, T> parser,
            T defaultValue) {
        Objects.requireNonNull(parser, "Parser argument cannot be null.");
        XML xml = getXML(xpathExpression);
        if (xml == null) {
            return defaultValue;
        }
        return parser.apply(xml);
    }

    //TODO allow to specify collection implementation?
    public <T> List<T> parseXMLList(
            String xpathExpression, Function<XML, T> parser) {
        return parseXMLList(xpathExpression, parser, null);
    }
    public <T> List<T> parseXMLList(
            String xpathExpression,
            Function<XML, T> parser,
            List<T> defaultValue) {
        Objects.requireNonNull(parser, "Parser argument cannot be null.");
        List<T> list = new ArrayList<>();
        List<XML> xmls = getXMLList(xpathExpression);
        for (XML xml : xmls) {
            if (xml != null) {
                T obj = parser.apply(xml);
                if (obj != null) {
                    list.add(obj);
                }
            }
        }
        if (list.isEmpty()) {
            return defaultValue;
        }
        return list;
    }


    //TODO have a formatXMLMap and others
    //TODO allow to specify map implementation?
    public <K,V> Map<K,V> parseXMLMap(
            String xpathExpression, Function<XML, Entry<K, V>> parser) {
        return parseXMLMap(xpathExpression, parser, null);
    }
    public <K,V> Map<K,V> parseXMLMap(
            String xpathExpression,
            Function<XML, Entry<K, V>> parser,
            Map<K,V> defaultValue) {
        Objects.requireNonNull(parser, "Parser argument cannot be null.");
        Map<K,V> map = new HashMap<>();
        List<XML> xmls = getXMLList(xpathExpression);
        for (XML xml : xmls) {
            if (xml != null) {
                Entry<K,V> entry = parser.apply(xml);
                if (entry != null) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (map.isEmpty()) {
            return defaultValue;
        }
        return map;
    }

    /**
     * Checks whether a deprecated configuration entry was specified
     * and log a warning or throw an {@link XMLException}.
     * @param deprecatedXPath xpath to the invalid entry
     * @param validXPath xpath to the valid entry
     * @param throwException <code>true</code> to throw exception, else log
     *        a warning
     */
    public void checkDeprecated(
            String deprecatedXPath, String validXPath, boolean throwException) {
        if (contains(deprecatedXPath)) {
            String msg = "\"" + deprecatedXPath
                    + "\" has been deprecated in favor of \"" + validXPath
                    + "\".  Please update your XML configuration accordingly.";
            if (throwException) {
                throw new XMLException(msg);
            }
            LOG.warn(msg);
        }
    }
}
