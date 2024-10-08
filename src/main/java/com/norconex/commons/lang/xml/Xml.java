/* Copyright 2010-2022 Norconex Inc.
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

import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import com.norconex.commons.lang.ClassFinder;
import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.ConverterException;
import com.norconex.commons.lang.convert.GenericConverter;
import com.norconex.commons.lang.time.DurationParser;
import com.norconex.commons.lang.time.DurationParserException;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.unit.DataUnitParser;

import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

//MAYBE: consider checking for a "disable=false|true" and setting it on
//a method if this method exists, and/or do not load if set to true.

//MAYBE: add "addStringMap"

//MAYBE: have a <generic> getObject to reduce the number of get methods.

//MAYBE getElement(s) and getAttribute(s) which are direct references to
// elements attributes (not xpath)

/**
 * <p>
 * XML DOM wrapper facilitating node querying and automatically creating,
 * validating, and populating classes from/to XML, with support
 * for {@link XmlConfigurable} and {@link JAXB}.
 * </p>
 * <h2>XML syntax and white spaces</h2>
 * <p>
 * Some white spaces in element text may be removed when parsed.
 * To keep them, add the XML standard attribute
 * <code>xml:space="preserve"</code> to your element.  For instance, the
 * following ensures the four spaces are kept when parsed:
 * </p>
 * <pre>
 *   &lt;example xml:space="preserve"&gt;    &lt;/example&gt;
 * </pre>
 * <p>
 * Empty tags are interpreted as having an empty strings while
 * self-closing tags have their value interpreted as <code>null</code>.
 * Non-existing tags have no effect (when loading over an object, that
 * object current value should remain unchanged).
 * </p>
 * <p>
 * Checked exceptions are wrapped into an {@link XmlException}.
 * </p>
 * @since 2.0.0
 */
@Slf4j
public class Xml implements Iterable<XmlCursor> {

    private static final String DEFAULT_DELIM_REGEX = "(\\s*,\\s*)+";
    private static final String NULL_XML_VALUE = "\u0000";
    private static final List<String> NULL_XML_LIST = new ArrayList<>(0);
    private static final String XPATH_ATT_CLASS = "@class";
    private static final String ATT_XML_SPACE = "xml:space";

    private final Node node;
    private final ErrorHandler errorHandler;
    private final DocumentBuilderFactory documentBuilderFactory;

    //--- Constructors ---------------------------------------------------------

    /**
     * <p>Parse an XML file into an XML document, without consideration
     * for namespaces.</p>
     * @param file the XML file to parse
     * @see #of(Path)
     */
    public Xml(Path file) {
        this(Xml.of(file).create().node);
    }

    /**
     * <p>Parse an XML file into an XML document, without consideration
     * for namespaces.</p>
     * @param file the XML file to parse
     * @see #of(File)
     */
    public Xml(File file) {
        this(Xml.of(file).create().node);
    }

    /**
     * <p>Parse an XML stream into an XML document, without consideration
     * for namespaces.</p>
     * @param reader the XML stream to parse
     * @see #of(Reader)
     */
    public Xml(Reader reader) {
        this(Xml.of(reader).create().node);
    }

    /**
     * <p>
     * Parse an XML string into an XML document, without consideration
     * for namespaces.
     * </p>
     * <p>
     * The supplied "xml" string can either be a well-formed XML or
     * a string without angle brackets. When the later is supplied,
     * it is interpreted as the XML root element name (for a fresh XML).
     * </p>
     * @param xml the XML string to parse
     * @see #of(String)
     */
    public Xml(String xml) {
        this(Xml.of(xml).create().node);
    }

    /**
     * Creates a new XML with the supplied root element (i.e., tag name),
     * and populate it with the supplied object.
     * @param rootElement XML root element name
     * @param obj the object to populate this XML with.
     * @see #of(String, Object)
     */
    public Xml(String rootElement, Object obj) {
        this(Xml.of(rootElement, obj).create().node);
    }

    /**
     * <p>Creates an XML with the given DOM node.</p>
     * @param node the node representing the XML
     */
    public Xml(Node node) {
        this(node, null, null, null);
    }

    /**
     * <p>Creates an XML with the given node.</p>
     * @param node the node representing the XML
     */
    private Xml(
            Node node,
            Object sourceObject,
            ErrorHandler errorHandler,
            DocumentBuilderFactory documentBuilderFactory) {
        this.node = node;
        this.errorHandler = defaultIfNull(errorHandler);
        this.documentBuilderFactory = defaultIfNull(documentBuilderFactory);
        if (sourceObject != null) {
            if (sourceObject instanceof Class) {
                setAttribute(
                        "class", ((Class<?>) sourceObject).getCanonicalName());
            } else if (GenericConverter.defaultInstance().isConvertible(
                    sourceObject.getClass())) {
                setTextContent(GenericConverter.convert(sourceObject));
            } else {
                setAttribute("class",
                        sourceObject.getClass().getCanonicalName());
                if (isJAXB(sourceObject)) {
                    jaxbMarshall(sourceObject);
                }
                if (isXMLConfigurable(sourceObject)) {
                    ((XmlConfigurable) sourceObject).saveToXML(this);
                }
            }
        }
    }

    //--- Builders -------------------------------------------------------------

    public static Builder of(File file) {
        return new Builder(file);
    }

    public static Builder of(Path path) {
        return new Builder(path);
    }

    public static Builder of(Node node) {
        return new Builder(node);
    }

    public static Builder of(InputStream is) {
        return new Builder(is);
    }

    public static Builder of(Reader reader) {
        return new Builder(reader);
    }

    public static Builder of(String xml) {
        return new Builder(xml);
    }

    public static Builder of(String rootElementName, Object object) {
        return new Builder(object, rootElementName);
    }

    public static class Builder {
        private DocumentBuilderFactory documentBuilderFactory;
        private ErrorHandler errorHandler;

        private final Object source;
        // if root element is set, it means it came "fromObject".
        private final String rootElementName;

        private Builder(Object source) {
            this(source, null);
        }

        private Builder(Object source, String rootElementName) {
            this.source = source;
            this.rootElementName = rootElementName;
        }

        public Builder setDocumentBuilderFactory(
                DocumentBuilderFactory documentBuilderFactory) {
            this.documentBuilderFactory = documentBuilderFactory;
            return this;
        }

        public Builder setErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Xml create() {
            errorHandler = defaultIfNull(errorHandler);
            documentBuilderFactory = defaultIfNull(documentBuilderFactory);

            if (source instanceof Node n) {
                return new Xml(n, null, errorHandler, documentBuilderFactory);
            }

            String xmlStr = null;
            if (StringUtils.isNotBlank(rootElementName)) {
                xmlStr = "<" + rootElementName + "/>";
            } else if (source instanceof Path p) {
                xmlStr = fileToString(p.toFile());
            } else if (source instanceof File f) {
                xmlStr = fileToString(f);
            } else if (source instanceof InputStream is) {
                xmlStr = readerToString(new InputStreamReader(is));
            } else if (source instanceof Reader r) {
                xmlStr = readerToString(r);
            } else if (source instanceof String s) {
                xmlStr = s;
            }

            if (StringUtils.isBlank(xmlStr)) {
                return new Xml((Node) null,
                        null, errorHandler, documentBuilderFactory);
            }

            xmlStr = xmlStr.trim();

            if (!xmlStr.contains("<")) {
                xmlStr = "<" + xmlStr + "/>";
            }

            //--- Ensure proper reading of null and empty values ---

            // Add xml:space="empty" to empty tags.
            xmlStr = xmlStr.replaceAll(
                    "(<\\s*)([^\\s>]+)([^>]*)(\\s*><\\s*\\/\\s*\\2\\s*>)",
                    "$1$2 xml:space=\"empty\" $3$4");
            Element node = null;
            try {
                node = documentBuilderFactory.newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xmlStr)))
                        .getDocumentElement();
            } catch (ParserConfigurationException
                    | SAXException | IOException e) {
                throw new XmlException("Could not parse XML.", e);
            }

            Object sourceObject = null;
            if (rootElementName != null && source != null) {
                sourceObject = source;
            }
            return new Xml(
                    node, sourceObject, errorHandler, documentBuilderFactory);
        }

        private static String readerToString(Reader reader) {
            try {
                return IOUtils.toString(reader);
            } catch (IOException e) {
                throw new XmlException("Could not read XML.", e);
            }
        }

        private static String fileToString(File file) {
            try {
                return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new XmlException("Could not read XML file: "
                        + file.getAbsolutePath(), e);
            }
        }
    }

    //--- Populate Object ------------------------------------------------------

    /**
     * <p>
     * Populates supplied object with the XML matching the given expression.
     * If there is no match, the object does not get populated.
     * Takes into consideration whether the target object implements
     * {@link XmlConfigurable} or JAXB.
     * </p>
     * <p>
     * Performs XML validation if the target object has an associated schema.
     * </p>
     * @param xpathExpression XPath expression
     * @param targetObject object to populate with this XML
     */
    public void populate(Object targetObject, String xpathExpression) {
        ifXML(xpathExpression, x -> x.populate(targetObject));
    }

    /**
     * <p>
     * Populates supplied object with this XML. Takes into consideration
     * whether the target object implements {@link XmlConfigurable} or
     * JAXB.
     * </p>
     * <p>
     * Performs XML validation if the target object has an associated schema.
     * </p>
     * <p>
     * Invoking this method with a <code>null</code> target has no effect
     * (returns an empty list).
     * </p>
     * @param targetObject object to populate with this XML
     */
    public void populate(Object targetObject) {
        if (node == null || targetObject == null) {
            return;
        }
        try {
            validate(targetObject.getClass());
            if (isJAXB(targetObject)) {
                jaxbUnmarshall(targetObject);
            }
            if (isXMLConfigurable(targetObject)) {
                ((XmlConfigurable) targetObject).loadFromXML(this);
            }
        } catch (XmlException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlException("XML (tag: <" + getName() + ">) "
                    + "could not be converted to object of type: "
                    + targetObject.getClass(), e);
        }
    }

    //--- To Object ------------------------------------------------------------

    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on this XML root node.  The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.
     * If the class is annotated with an
     * {@link XmlRootElement}, it will use JAXB to unmarshall it to an object.
     * </p>
     * <p>
     * Performs XML validation if the target object has an associated schema.
     * </p>
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XmlValidationException if the XML has validation errors
     * @throws XmlException if something prevented object creation
     */
    public <T> T toObject() {
        return toObject(null);
    }

    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.
     * If the class is annotated with an
     * {@link XmlRootElement}, it will use JAXB to unmarshall it to an object.
     * </p>
     * <p>
     * Performs XML validation if the target object has an associated schema.
     * </p>
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XmlException if something prevented object creation
     */
    @SuppressWarnings("unchecked")
    public <T> T toObject(T defaultObject) {
        return toObject(
                (Class<T>) getClass(XPATH_ATT_CLASS, null), defaultObject);
    }

    private <T> T toObject(Class<T> objClass, T defaultObject) {
        if (node == null) {
            return defaultObject;
        }
        T obj;
        if (objClass != null) {
            try {
                obj = objClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new XmlException(
                        "This class could not be instantiated: " + objClass, e);
            }
        } else {
            if (isExplicitNull()) {
                return null;
            }
            LOG.debug("""
                A configuration entry was found without a class\s\
                attribute where one could have been provided;\s\
                using default value: {}""", defaultObject);
            obj = defaultObject;
        }

        populate(obj);
        return obj;
    }

    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on this XML root node.  The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.
     * If the class is annotated with an
     * {@link XmlRootElement}, it will use JAXB to unmarshall it to an object.
     * </p>
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #toObject()} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XmlException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #toObject()} method, which is faster.
     * </p>
     * @param type the expected class (sub)type to return
     * @param <T> the type of the return value
     * @return a new object or <code>null</code>.
     * @throws XmlValidationException if the XML has validation errors
     * @throws XmlException if something prevented object creation
     */
    public <T> T toObjectImpl(Class<?> type) {
        return toObjectImpl(type, null);
    }

    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.
     * If the class is annotated with an
     * {@link XmlRootElement}, it will use JAXB to unmarshall it to an object.
     * </p>
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #toObject(Object)} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XmlException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #toObject(Object)} method, which is faster.
     * </p>
     * @param type the expected class (sub)type to return
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param <T> the type of the return value
     * @return a new object if not <code>null</code>, else the default object.
     * @throws XmlException if something prevented object creation
     */
    @SuppressWarnings("unchecked")
    public <T> T toObjectImpl(Class<?> type, T defaultObject) {
        if (node == null || type == null) {
            return defaultObject;
        }

        T obj;
        try {
            obj = toObject(defaultObject);
        } catch (ConverterException e) {
            if (!(e.getCause() instanceof ClassNotFoundException)) {
                throw e;
            }
            var partialName = getString(XPATH_ATT_CLASS);
            List<?> results = ClassFinder.findSubTypes(
                    type, s -> s.endsWith(partialName));
            if (results.size() > 1) {
                // see if only one of them matches a segment exactly.
                List<?> filteredResults = results.stream()
                        .filter(c -> ((Class<?>) c).getName()
                                .endsWith("." + partialName))
                        .toList();
                if (filteredResults.size() != 1) {
                    throw new XmlException(results.size()
                            + " classes implementing \""
                            + type.getName() + "\" "
                            + "and ending with \"" + partialName + "\" "
                            + "where found when only 1 was expected. "
                            + "Consider using fully qualified class name. "
                            + "Found classes: "
                            + results.stream()
                                    .map(c -> ((Class<?>) c).getName())
                                    .collect(Collectors.joining(", ")));
                }
                LOG.debug("""
                    {} classes implementing "{}" and ending\s\
                    with "{}" were found, but only one\s\
                    matched an exact class name or class name\s\
                    and package segment: {}""",
                        results.size(),
                        type.getName(),
                        partialName,
                        ((Class<?>) filteredResults.get(0)).getName());
                results.retainAll(filteredResults);
            }

            if (results.isEmpty()) {
                throw new XmlException(
                        "No class implementing \""
                                + type.getName() + "\" "
                                + "and ending with \"" + partialName + "\" "
                                + "could be found. Check your classpath or "
                                + "consider using fully qualified class name.");
            }
            obj = toObject((Class<T>) results.get(0), defaultObject);
        }
        if (obj != null && !type.isInstance(obj)) {
            throw new XmlException(
                    obj.getClass() + " is not an instance of " + type);
        }
        return obj;
    }

    //--- Get: Object ----------------------------------------------------------

    /**
     * <p>Creates a new instance of the class represented by the "class"
     * attribute on the node matching the expression.
     * The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should throw a
     * {@link XmlException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     *
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     */
    public <T extends Object> T getObject(String xpathExpression) {
        return getObject(xpathExpression, (T) null);
    }

    /**
     * <p>Creates a new instance of the class represented by the "class"
     * attribute on the node matching the expression.
     * The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
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
        if (node == null) {
            return defaultObject;
        }

        try {
            if (xpathExpression == null && defaultObject == null) {
                return toObject((T) null);
            }
            var xml = getXML(xpathExpression);
            if (xml == null) {
                return defaultObject;
            }
            return xml.toObject(defaultObject);
        } catch (Exception e) {
            handleException(node.getNodeName(), xpathExpression, e);
            return defaultObject;
        }
    }

    /**
     * <p>Creates an instance list from classes represented by the "class"
     * attribute on the nodes matching the expression.
     * The classes must have an empty constructor.
     * If a class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
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
     * @throws XmlException if instance cannot be created/populated
     */
    public <T> List<T> getObjectList(
            String xpathExpression, List<T> defaultObjects) {
        var xmls = getXMLListOptional(xpathExpression);
        // We return:
        //   - an empty list if optional is empty.
        //   - the default list if optional is not emtpy but node list is
        //   - otherwise return the matching list

        if (!xmls.isPresent()) {
            return Collections.emptyList();
        }
        if (xmls.get().isEmpty()) {
            return defaultObjects;
        }
        List<T> list = new ArrayList<>();
        for (Xml xml : xmls.get()) {
            if (xml != null) {
                var obj = xml.<T>toObject();
                if (obj != null) {
                    list.add(obj);
                }
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
     * If a class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should throw a
     * {@link XmlException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     *
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XmlException if instance cannot be created/populated
     */
    public <T> List<T> getObjectList(String xpathExpression) {
        return getObjectList(xpathExpression, Collections.emptyList());
    }

    /**
     * <p>Creates a new instance of the class represented by the "class"
     * attribute on the node matching the expression.
     * The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should throw a
     * {@link XmlException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #getObject(String)} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XmlException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #getObject(String)} method, which is faster.
     * </p>
     * @param type the expected class (sub)type to return
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     */
    public <T extends Object> T getObjectImpl(
            Class<?> type, String xpathExpression) {
        return getObjectImpl(type, xpathExpression, (T) null);
    }

    /**
     * <p>Creates a new instance of the class represented by the "class"
     * attribute on the node matching the expression.
     * The class must have an empty constructor.
     * If the class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should not throw exception upon errors, but will return
     * the default value instead (even if null). Use a method without
     * a default value argument to get exceptions on errors.</p>
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #getObject(String, Object)} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XmlException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #getObject(String, Object)} method, which is faster.
     * </p>
     *
     * @param type the expected class (sub)type to return
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     */
    public <T> T getObjectImpl(
            Class<?> type, String xpathExpression, T defaultObject) {
        if (node == null || type == null) {
            return defaultObject;
        }

        try {
            if (xpathExpression == null && defaultObject == null) {
                return toObjectImpl(type, (T) null);
            }
            var xml = getXML(xpathExpression);
            if (xml == null) {
                return defaultObject;
            }
            return xml.toObjectImpl(type, defaultObject);
        } catch (Exception e) {
            handleException(
                    node.getNodeName(), xpathExpression, e);
            return defaultObject;
        }
    }

    /**
     * <p>Creates an instance list from classes represented by the "class"
     * attribute on the nodes matching the expression.
     * The classes must have an empty constructor.
     * If a class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should not throw exception upon errors, but will return
     * the default value instead (even if null). Use a method without
     * a default value argument to get exceptions on errors.</p>
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #getObjectList(String, List)} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XmlException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #getObjectList(String, List)} method, which is faster.
     * </p>
     *
     * @param type the expected class (sub)type to return
     * @param xpathExpression xpath expression
     * @param defaultObjects if returned list is empty,
     *        returns this default list.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XmlException if instance cannot be created/populated
     */
    public <T> List<T> getObjectListImpl(
            Class<?> type, String xpathExpression, List<T> defaultObjects) {

        var xmls = getXMLListOptional(xpathExpression);
        // We return:
        //   - an empty list if optional is empty.
        //   - the default list if optional is not emtpy but node list is
        //   - otherwise return the matching list
        if (!xmls.isPresent()) {
            return Collections.emptyList();
        }
        if (xmls.get().isEmpty()) {
            return defaultObjects;
        }

        List<T> list = new ArrayList<>();
        for (Xml xml : xmls.get()) {
            if (xml != null) {
                var obj = xml.<T>toObjectImpl(type);
                if (obj != null) {
                    list.add(obj);
                }
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
     * If a class is an instance of {@link XmlConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link XmlConfigurable#loadFromXML(Xml)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should throw a
     * {@link XmlException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #getObjectList(String)} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XmlException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #getObjectList(String)} method, which is faster.
     * </p>
     *
     * @param type the expected class (sub)type to return
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XmlException if instance cannot be created/populated
     */
    public <T> List<T> getObjectListImpl(
            Class<?> type, String xpathExpression) {
        return getObjectListImpl(
                type, xpathExpression, Collections.emptyList());
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

        var values = getDelimitedStringList(
                xpathExpression, delimRegex, NULL_XML_LIST);
        if (values == null) {
            return (List<T>) defaultValues;
        }
        if (values.isEmpty() || values == NULL_XML_LIST) {
            return Collections.emptyList();
        }
        return CollectionUtil.toTypeList(values, type);

    }

    //--- Get: XML -------------------------------------------------------------

    /**
     * Gets the xml subset matching the xpath expression.
     * @param xpathExpression expression to match
     * @return XML or <code>null</code> is xpath has no match
     */
    public Xml getXML(String xpathExpression) {
        var xmlNode = getNode(xpathExpression);
        if (xmlNode == null) {
            return null;
        }
        return createAndInitXML(Xml.of(xmlNode));
    }

    /**
     * Gets the XML subsets matching the xpath expression.
     * @param xpathExpression expression to match
     * @return XML list, never <code>null</code>
     */
    public List<Xml> getXMLList(String xpathExpression) {
        var xmls = getXMLListOptional(xpathExpression);
        // We return:
        //   - an empty list if optional is empty.
        //   - otherwise return the matching list
        if (!xmls.isPresent()) {
            return Collections.emptyList();
        }
        return xmls.get();
    }

    private Optional<List<Xml>> getXMLListOptional(String xpathExpression) {

        var nodeList = getNodeList(xpathExpression);
        // We return:
        //   - an empty Optional if nodeList Optional is empty.
        //   - otherwise return the matching list
        if (!nodeList.isPresent()) {
            return Optional.empty();
        }
        List<Xml> list = new ArrayList<>();
        for (Node n : nodeList.get()) {
            list.add(createAndInitXML(Xml.of(n)));
        }
        return Optional.of(list);
    }

    //--- Get: Node ------------------------------------------------------------

    public Node getNode(String xpathExpression) {
        return getNode(xpathExpression, node);
    }

    public Node getNode() {
        return node;
    }

    public Node toNode() {
        return node;
    }

    private Node getNode(String xpathExpression, Node parentNode) {
        try {
            return (Node) XpathUtil.newXPathExpression(
                    xpathExpression).evaluate(parentNode, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new XmlException(
                    "Could not evaluate XPath expression.", e);
        }
    }

    private Optional<NodeArrayList> getNodeList(String xpathExpression) {
        try {
            var nodeList = (NodeList) XpathUtil.newXPathExpression(
                    xpathExpression).evaluate(node, XPathConstants.NODESET);

            if (nodeList != null && nodeList.getLength() > 0) {
                return Optional.of(new NodeArrayList(nodeList));
            }
            // When there are no node list returned, we check if it is because
            // the xpath expression did not match anything (in which case
            // it may suggest to use a default value) or it did match
            // a tag but it was empty (indicating wanting to clear any
            // existing list).
            var xpath = substringBeforeLast(xpathExpression, "/");
            var xmlTag = getXML(xpath);
            if (xmlTag == null || StringUtils.isBlank(xmlTag.toString())) {
                return Optional.of(new NodeArrayList((NodeList) null));
            }
            // If we get this far, there was a tag declared, so we treat
            // it as an explicit request to clear so we do not return anything
            // as a way to communicate that.
            return Optional.empty();
        } catch (XPathExpressionException e) {
            throw new XmlException(
                    "Could not evaluate XPath expression: '"
                            + xpathExpression + "'.",
                    e);
        }
    }

    //--- Get: String ----------------------------------------------------------

    public String getString(String xpathExpression) {
        return getString(xpathExpression, null);
    }

    public String getString(String xpathExpression, String defaultValue) {
        var n = getNode(xpathExpression);
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
        var list = getStringList(xpathExpression, null);
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
        var nodeList = getNodeList(xpathExpression);
        // We return:
        //   - an empty list if optional is empty.
        //   - the default list if optional is not emtpy but node list is
        //   - otherwise return the matching list
        if (!nodeList.isPresent()) {
            return Collections.emptyList();
        }
        if (nodeList.get().isEmpty()) {
            return defaultValues;
        }
        List<String> list = new ArrayList<>();
        for (Node n : nodeList.get()) {
            var str = getNodeString(n);
            if (str != null) {
                list.add(str);
            }
        }
        return list;
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
        var values =
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
        var values =
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

        var delimList = getStringList(xpathExpression, NULL_XML_LIST);
        if (delimList == null) {
            return defaultValues;
        }
        if (delimList.isEmpty() || delimList == NULL_XML_LIST) {
            return Collections.emptyList();
        }

        List<String> splitList = new ArrayList<>();
        for (String str : delimList) {
            var values = split(str, delimRegex);
            if (CollectionUtils.isEmpty(values)) {
                continue;
            }
            for (String val : values) {
                var trimmed = StringUtils.trimToNull(val);
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

    //--- Get: Enum ------------------------------------------------------------

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

    //--- Get: Path ------------------------------------------------------------

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

    //--- Get: File ------------------------------------------------------------

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

    //--- Get: URL -------------------------------------------------------------

    public final URL getURL(String xpathExpression) {
        return get(xpathExpression, URL.class);
    }

    public final URL getURL(String xpathExpression, URL defaultValue) {
        return get(xpathExpression, URL.class, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public final List<URL> getURLList(String xpathExpression) {
        return (List<URL>) getList(xpathExpression, URL.class);
    }

    @SuppressWarnings("unchecked")
    public final List<URL> getURLList(
            String xpathExpression, List<URL> defaultValue) {
        return (List<URL>) getList(xpathExpression, URL.class, defaultValue);
    }

    //--- Get: Integer ---------------------------------------------------------

    public Integer getInteger(String xpathExpression) {
        return get(xpathExpression, Integer.class);
    }

    public Integer getInteger(String xpathExpression, Integer defaultValue) {
        return get(xpathExpression, Integer.class, defaultValue);
    }

    //--- Get: Long ------------------------------------------------------------

    public Long getLong(String xpathExpression) {
        return get(xpathExpression, Long.class);
    }

    public Long getLong(String xpathExpression, Long defaultValue) {
        return get(xpathExpression, Long.class, defaultValue);
    }

    //--- Get: Float -----------------------------------------------------------

    public Float getFloat(String xpathExpression) {
        return get(xpathExpression, Float.class);
    }

    public Float getFloat(String xpathExpression, Float defaultValue) {
        return get(xpathExpression, Float.class, defaultValue);
    }

    //--- Get: Dimension -------------------------------------------------------

    public Dimension getDimension(String xpathExpression) {
        return get(xpathExpression, Dimension.class);
    }

    public Dimension getDimension(
            String xpathExpression, Dimension defaultValue) {
        return get(xpathExpression, Dimension.class, defaultValue);
    }

    //--- Get: Double ----------------------------------------------------------

    public Double getDouble(String xpathExpression) {
        return get(xpathExpression, Double.class);
    }

    public Double getDouble(String xpathExpression, Double defaultValue) {
        return get(xpathExpression, Double.class, defaultValue);
    }

    //--- Get: Boolean ---------------------------------------------------------

    public Boolean getBoolean(String xpathExpression) {
        return get(xpathExpression, Boolean.class);
    }

    public Boolean getBoolean(String xpathExpression, Boolean defaultValue) {
        return get(xpathExpression, Boolean.class, defaultValue);
    }

    //--- Get: Locale ----------------------------------------------------------

    public Locale getLocale(String xpathExpression) {
        return get(xpathExpression, Locale.class);
    }

    public Locale getLocale(String xpathExpression, Locale defaultValue) {
        return get(xpathExpression, Locale.class, defaultValue);
    }

    //--- Get: Charset ---------------------------------------------------------

    public Charset getCharset(String xpathExpression) {
        return get(xpathExpression, Charset.class);
    }

    public Charset getCharset(String xpathExpression, Charset defaultValue) {
        return get(xpathExpression, Charset.class, defaultValue);
    }

    //--- Get: Data Size -------------------------------------------------------

    /**
     * Gets the size of a data expression, in bytes (e.g., 2KB, 1GiB,
     * 3 megabytes, etc).  Without a unit specified, the value is assumed
     * to represent bytes.
     * @param xpathExpression xpath to the element/attribute with the size
     * @return size in bytes
     * @since 2.0.0
     */
    public Long getDataSize(String xpathExpression) {
        return getDataSize(xpathExpression, null, null);
    }

    /**
     * Gets the size of a data expression, in bytes (e.g., 2KB, 1GiB,
     * 3 megabytes, etc).  Without a unit specified, the value is assumed
     * to represent bytes.
     * @param xpathExpression xpath to the element/attribute with the size
     * @param defaultValue default value
     * @return size in bytes or default value if size is <code>null</code>
     * @since 2.0.0
     */
    public Long getDataSize(
            String xpathExpression, Long defaultValue) {
        return getDataSize(xpathExpression, null, defaultValue);
    }

    /**
     * Gets the size of a data expression, in the specified target unit
     * (e.g., 2KB, 1GiB, 3 megabytes, etc).  Without a unit specified
     * in the value, the value is assumed to represent bytes.
     * @param xpathExpression xpath to the element/attribute with the size
     * @param targetUnit the unit to convert the value into
     * @param defaultValue default value
     * @return size in bytes or default value if size is <code>null</code>
     * @since 2.0.0
     */
    public Long getDataSize(
            String xpathExpression, DataUnit targetUnit, Long defaultValue) {
        var sz = DataUnitParser.parse(getString(
                xpathExpression, null), targetUnit, BigDecimal.valueOf(-1));
        if (sz.longValue() == -1) {
            return defaultValue;
        }
        return sz.longValue();
    }

    //--- Get: Duration --------------------------------------------------------

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
        var d = getDuration(xpathExpression);
        if (d == null) {
            return defaultValue;
        }
        return d.toMillis();
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
    public Duration getDuration(String xpathExpression, Duration defaultValue) {
        return get(xpathExpression, Duration.class, defaultValue);
    }

    //--- Get: Pattern ---------------------------------------------------------

    /**
     * Gets a regular expression pattern.
     * @param xpathExpression xpath to the element/attribute with the pattern
     * @return pattern
     * @since 3.0.0
     */
    public Pattern getPattern(String xpathExpression) {
        return get(xpathExpression, Pattern.class);
    }

    /**
     * Gets a regular expression pattern.
     * @param xpathExpression xpath to the element/attribute with the pattern
     * @param defaultValue default value
     * @return pattern
     * @since 3.0.0
     */
    public Pattern getPattern(String xpathExpression, Pattern defaultValue) {
        return get(xpathExpression, Pattern.class, defaultValue);
    }

    //--- Jaxb -----------------------------------------------------------------

    private void jaxbMarshall(Object obj) {
        try {
            var name = node.getNodeName();
            List<Attr> attributes = new ArrayList<>();
            var nattributes = node.getAttributes();
            for (var i = 0; i < nattributes.getLength(); i++) {
                attributes.add((Attr) nattributes.item(i));
            }

            var contextObj = JAXBContext.newInstance(obj.getClass());
            var marshaller = contextObj.createMarshaller();
            marshaller.marshal(obj, node);

            unwrap();

            var el = ((Element) node);
            for (Attr at : attributes) {
                el.setAttributeNS(
                        at.getNamespaceURI(), at.getName(), at.getValue());
            }
            rename(name);
        } catch (Exception e) {
            throw new XmlException(
                    "This object could not be JAXB-marshalled: " + obj, e);
        }
    }

    // Object is never null
    private void jaxbUnmarshall(Object obj) {
        try {
            Class<?> clas = obj.getClass();
            var jaxbContext = JAXBContext.newInstance(clas);
            var unmarsh = jaxbContext.createUnmarshaller();
            JAXBElement<?> newObj = unmarsh.unmarshal(node, obj.getClass());
            BeanUtil.copyProperties(obj, newObj.getValue());
        } catch (Exception e) {
            throw new XmlException("XML (tag: <" + getName() + ">) "
                    + " could not be JAXB-unmarshalled: " + this, e);
        }
    }

    //--- Validate -------------------------------------------------------------

    /**
     * <p>
     * Validates this XML against an XSD schema attached to the class
     * represented in this XML root tag "class" attribute.
     * </p>
     * <h4>Error handling</h4>
     * <p>
     * The expected behavior when encountering validation errors is tied
     * to the registered {@link ErrorHandler}. When no error handler is
     * specified, the default is {@link ErrorHandlerFailer} (which
     * throws {@link XmlValidationException}). Error handlers can be
     * specified when using the builder obtained with one of the
     * <code>XML.of(...)</code>) methods.
     * </p>
     * <p>
     * The XSD schema used for validation is expected to be found at the
     * same classpath location and have the same name as the object class,
     * but with the ".xsd" extension.
     * </p>
     * <p>
     * This method is the same as invoking
     * <code>validate(getClass("@class"))</code>
     * </p>
     * @see ErrorHandlerFailer
     * @see ErrorHandlerCapturer
     * @see ErrorHandlerLogger
     */
    public void validate() {
        validate(getClass(XPATH_ATT_CLASS));
    }

    /**
     * <p>
     * Validates this XML against an XSD schema attached to the class
     * represented in this XML root tag "class" attribute.
     * </p>
     * <h4>Error handling</h4>
     * <p>
     * The expected behavior when encountering validation errors is tied
     * to the registered {@link ErrorHandler}. When no error handler is
     * specified, the default is {@link ErrorHandlerFailer} (which
     * throws {@link XmlValidationException}). Error handlers can be
     * specified when using the builder obtained with one of the
     * <code>XML.of(...)</code>) methods..
     * </p>
     * <p>
     * The XSD schema used for validation is expected to be found at the
     * same classpath location and have the same name as the object class,
     * but with the ".xsd" extension.
     * </p>
     * <p>
     * This method is the same as invoking <code>validate(obj.getClass())</code>
     * </p>
     * @param obj the object used to locate the XML schema used for validation
     * @see ErrorHandlerFailer
     * @see ErrorHandlerCapturer
     * @see ErrorHandlerLogger
     */
    public void validate(Object obj) {
        if (obj == null) {
            validate((Class<?>) null);
            return;
        }
        validate(obj.getClass());
    }

    /**
     * <p>
     * Validates this XML against an XSD schema attached to the class
     * represented in this XML root tag "class" attribute.
     * </p>
     * <h4>Error handling</h4>
     * <p>
     * The expected behavior when encountering validation errors is tied
     * to the registered {@link ErrorHandler}. When no error handler is
     * specified, the default is {@link ErrorHandlerFailer} (which
     * throws {@link XmlValidationException}). Error handlers can be
     * specified when using the builder obtained with one of the
     * <code>XML.of(...)</code>) methods..
     * </p>
     * <p>
     * The XSD schema used for validation is expected to be found at the
     * same classpath location and have the same name as the object class,
     * but with the ".xsd" extension.
     * </p>
     * @param clazz the class with XSD schema attached, used for validation
     * @see ErrorHandlerFailer
     * @see ErrorHandlerCapturer
     * @see ErrorHandlerLogger
     */
    public void validate(Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        // Only validate if .xsd file exist in classpath for class
        var xsdResource = ClassUtils.getShortCanonicalName(clazz) + ".xsd";
        LOG.debug("Validating XML for class {}",
                ClassUtils.getSimpleName(clazz));
        if (clazz.getResource(xsdResource) == null) {
            LOG.debug("XSD schema not found for validation: {}", xsdResource);
            return;
        }

        try (var xsdStream = clazz.getResourceAsStream(xsdResource);
                var xmlReader = toReader()) {
            validate(clazz, xsdStream, xmlReader);
        } catch (SAXException | IOException e) {
            throw new XmlException("Could not validate class: " + clazz, e);
        }
    }

    private void validate(
            Class<?> clazz,
            InputStream xsdStream,
            Reader reader) throws SAXException, IOException {

        // See also: https://github.com/OWASP/CheatSheetSeries/blob/master/
        // cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.md

        new ArrayList<>();

        var schemaFactory = XmlUtil.createSchemaFactory();
        schemaFactory.setResourceResolver(new ClasspathResourceResolver(clazz));

        var schema = schemaFactory.newSchema(
                new StreamSource(xsdStream, getXSDResourcePath(clazz)));
        var validator = XmlUtil.createSchemaValidator(schema);

        validator.setErrorHandler(errorHandler);

        var xmlReader = XmlUtil.createXMLReader();

        var saxSource = new SAXSource(
                new W3XMLNamespaceFilter(xmlReader), new InputSource(reader));
        validator.validate(saxSource);
    }

    //--- Enabled/Disabled -----------------------------------------------------

    // "enabled" should by default always be false, so it has to be enabled
    // explicitly.  Then it must be defined and "true".
    public boolean isEnabled() {
        return isDefined() && getBoolean("@enabled", false);
    }

    // "disabled" should by default always be false, so it has to be set
    // explicitly.  Then it must be defined and "true".
    public boolean isDisabled() {
        return isDefined() && getBoolean("@disabled", false);
    }

    //--- Misc. Public Methods -------------------------------------------------

    /**
     * Gets whether the given expression matches an existing element. A
     * blank expression always returns <code>false</code>.
     * @param xpathExpression expression
     * @return <code>true</code> if the expression matches an element
     * @since 3.0.0
     */
    public boolean isElementPresent(String xpathExpression) {
        var xml = getXML(xpathExpression);
        return xml != null && xml.isDefined();
    }

    /**
     * If the given expression matches an element, consume that
     * element.
     * @param xpathExpression expression
     * @param then XML consumer
     */
    public void ifXML(String xpathExpression, Consumer<Xml> then) {
        var xml = getXML(xpathExpression);
        if (xml != null && xml.isDefined() && then != null) {
            then.accept(xml);
        }
    }

    /**
     * Creates a new {@link Reader} from a {@link Node}.
     * Do not forget to close the reader instance when you are done with it.
     * @return reader
     * @throws XmlException cannot read configuration
     */
    public Reader toReader() {
        return new StringReader(toString());
    }

    @Override
    public boolean equals(Object obj) {
        return toString().equals(Objects.toString(obj, null));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Gets a string representation of this XML.
     * @return XML string
     * @throws XmlException cannot read configuration
     */
    @Override
    public String toString() {
        return toString(0);
    }

    /**
     * Gets a string representation of this XML.
     * @param indent whether to indent the XML
     * @return XML string
     * @throws XmlException cannot read configuration
     */
    public String toString(int indent) {
        try {
            node.normalize();

            fixIndent(indent);

            var w = new StringWriter();
            Result outputTarget = new StreamResult(w);

            var factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            var t = factory.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, indent > 0 ? "yes" : "no");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            if (indent > 0) {
                t.setOutputProperty(
                        "{http://xml.apache.org/xslt}indent-amount",
                        Integer.toString(indent));
            }
            t.transform(new DOMSource(node), outputTarget);

            var xmlStr = w.toString();
            // convert self-closing tags with "empty" attribute to empty tags
            // instead
            return xmlStr.replaceAll(
                    "<\\s*([^\\s>]+)([^>]*) xml:space=\"empty\"([^>]*)/\\s*>",
                    "<$1$2></$1>");
        } catch (TransformerFactoryConfigurationError
                | TransformerException e) {
            throw new XmlException(
                    "Could not convert node to reader "
                            + "for node \"" + node.getNodeName() + "\".",
                    e);
        }
    }

    /**
     * If the given expression matches one or more elements, consume those
     * element one by one.
     * @param xpathExpression expression
     * @param action The action to be performed for each element
     */
    public void forEach(String xpathExpression, Consumer<Xml> action) {
        var xmlList = getXMLList(xpathExpression);
        xmlList.forEach(x -> {
            if (x != null && x.isDefined() && action != null) {
                action.accept(x);
            }
        });
    }

    /**
     * Convenience class for testing that an object annotated with JAXB
     * {@link XmlRootElement} or implementing {@link XmlConfigurable}
     * (or both) can be written, and read back into an new instance
     * that is equal as per {@link #equals(Object)}.
     * @param object the instance object to test if it written/read properly
     * @param elementName the tag name of the root element being written
     * @throws XmlException Cannot write/read configuration or the read
     *     object is not equal to the original one that was written.
     */
    public static void assertWriteRead(Object object, String elementName) {
        LOG.debug("Writing/Reading this: {}", object);

        // Write
        String xmlStr;
        try (var out = new StringWriter()) {
            var xml = Xml.of(elementName, object).create();
            xml.write(out);
            xmlStr = out.toString();
        } catch (IOException e) {
            throw new XmlException("Could not save XML.", e);
        }
        LOG.trace(xmlStr);

        // Read
        var xml = Xml.of(xmlStr).create();
        var readConfigurable = xml.toObject();
        if (!object.equals(readConfigurable)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(" SAVED: {}", object);
                LOG.error("LOADED: {}", readConfigurable);
                LOG.error("  DIFF: \n{}\n",
                        BeanUtil.diff(object, readConfigurable));
            }
            throw new XmlException("Saved and loaded XML are not the same.");
        }
    }

    public boolean contains(String xpathExpression) {
        try {
            return XpathUtil.newXPathExpression(xpathExpression).evaluate(
                    node, XPathConstants.NODE) != null;
        } catch (XPathExpressionException e) {
            throw new XmlException(
                    "Could not evaluate expression: " + xpathExpression, e);
        }
    }

    /**
     * Gets a new XPath instance from the default object model.
     * @return new XPath instance
     * @deprecated Use {@link XpathUtil#newXPath()} instead
     */
    @Deprecated(since = "3.0.0")
    public static XPath newXPath() { //NOSONAR
        return XpathUtil.newXPath();
    }

    /**
     * Gets a new compiled {@link XPathExpression} from the given string.
     * @param expression the XPath string
     * @return compiled XPath expression
     * @deprecated Use {@link XpathUtil#newXPathExpression(String)} instead
     */
    @Deprecated(since = "3.0.0")
    public static XPathExpression newXPathExpression( //NOSONAR
            String expression) {
        return XpathUtil.newXPathExpression(expression);
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
        var value = getString(xpathExpression, NULL_XML_VALUE);
        if (value == null) {
            return null;
        }
        if (NULL_XML_VALUE.equals(value)) {
            return defaultValue;
        }
        return GenericConverter.convert(value, type, defaultValue);
    }

    /**
     * Gets the matching list of elements/attributes, converted from
     * string to the given type.
     * @param xpathExpression XPath expression to the node values
     * @param type target class type of returned list
     * @param <T> returned list type
     * @return list of given type, never <code>null</code>
     */
    public <T> List<? extends T> getList( //NOSONAR
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
    public <T> List<? extends T> getList(String xpathExpression, //NOSONAR
            Class<T> type, List<? extends T> defaultValues) {
        var list = getStringList(xpathExpression, null);
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
        var map = getStringMap(
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
     * @return map of strings, never <code>null</code> unless default value
     *         is returned and is <code>null</code>
     */
    public Map<String, String> getStringMap(String xpathList, String xpathKey,
            String xpathValue, Map<String, String> defaultValues) {

        var xmls = getXMLListOptional(xpathList);
        // We return:
        //   - an empty map if optional is empty.
        //   - the default map if optional is not empty but node list is
        //   - otherwise return the matching map
        if (!xmls.isPresent()) {
            return Collections.emptyMap();
        }
        if (xmls.get().isEmpty()) {
            return defaultValues;
        }

        Map<String, String> map = new HashMap<>();
        for (Xml xml : xmls.get()) {
            if (xml != null) {
                map.put(xml.getString(xpathKey), xml.getString(xpathValue));
            }
        }
        if (map.isEmpty()) {
            return defaultValues;
        }
        return map;
    }

    public String getName() {
        if (node == null) {
            return null;
        }
        return node.getNodeName();
    }

    /**
     * Computes and adds the element returned by the provided function
     * if an element with the same name is not already present, else, return
     * the existing one.
     * Passing a <code>null</code> function or <code>null</code> value returned
     * by the function when there are no existing element will add a new empty
     * element.
     * @param tagName element name
     * @param function taking the new element name and returning the element
     * @return XML of the added element, or the already existing element
     * @since 3.0.0
     */
    public Xml computeElementIfAbsent(
            String tagName, Function<String, Object> function) {
        var xml = getXML(tagName);
        if (xml != null && xml.isDefined()) {
            return xml;
        }
        if (function == null) {
            return addElement(tagName);
        }
        return addElement(tagName, function.apply(tagName));
    }

    /**
     * Adds an empty child element to this XML root element.
     * @param tagName element name
     * @return XML of the added element
     */
    public Xml addElement(String tagName) {
        return addElement(tagName, null);
    }

    /**
     * <p>
     * Adds a child element to this XML root element.
     * If the element value is blank, and empty element is created.
     * Otherwise, the value is handled as
     * {@link Xml#of(String, Object)}
     * @param tagName element name
     * @param value element value
     * @return XML of the added element
     */
    public Xml addElement(String tagName, Object value) {
        var xml = createAndInitXML(Xml.of(tagName, value));
        var newNode = node.getOwnerDocument().importNode(xml.node, true);
        return createAndInitXML(Xml.of(node.appendChild(newNode)));
    }

    /**
     * Adds a list of values to the current XML and return the added list.
     * There will be one element added for each entry of the supplied list,
     * all with the tag name supplied.
     * @param tagName the tag name for each values added
     * @param values values to add
     * @return the added list as a list of XML instances
     */
    public List<Xml> addElementList(String tagName, List<?> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        List<Xml> xmlList = new ArrayList<>();
        for (Object value : values) {
            xmlList.add(addElement(tagName, value));
        }
        return Collections.unmodifiableList(xmlList);
    }

    /**
     * Adds a list of values under a new parent tag name to the current XML
     * and return the added list.
     * There will be one element added for each entry of the supplied list,
     * all with the tag name supplied, all grouped under the parent tag
     * supplied.
     * @param parentTagName the name of the new parent tag that will hold
     *    all added values.
     * @param tagName the tag name for each values added
     * @param values values to add
     * @return the XML for the parent tag
     */
    public Xml addElementList(
            @NonNull String parentTagName, String tagName, List<?> values) {
        var parentXml = addElement(parentTagName);
        parentXml.addElementList(tagName, values);
        return parentXml;
    }

    /**
     * Adds a list of values as a new element after joining them with
     * a comma (CSV). Values are trimmed and blank entries removed.
     * Values can be of any types, as they converted to String by
     * invoking their "toString()" method.
     * @param name attribute name
     * @param values attribute values
     * @return the newly added element
     */
    public Xml addDelimitedElementList(String name, List<?> values) {
        return addDelimitedElementList(name, ",", values);
    }

    /**
     * Adds a list of values as a new element after joining them with
     * the given delimiter. Values are trimmed and blank entries removed.
     * Values can be of any types, as they converted to String by
     * invoking their "toString()" method.
     * @param name attribute name
     * @param delim delimiter
     * @param values attribute values
     * @return the newly added element
     */
    public Xml addDelimitedElementList(
            String name, String delim, List<?> values) {
        if (values.isEmpty()) {
            return addElement(name, "");
        }
        return addElement(name, join(delim, values)); //NOSONAR
    }

    /**
     * <p>
     * Adds a {@link Map} as a series of elements without a parent element
     * wrapping that group. Map keys are defined as element attributes
     * and the map value is the element content. The structure can be
     * visualized like this:
     * </p>
     * <pre>
     * &lt;tagName attributeName="(key)"&gt;(value)&lt;/tagName&gt;
     * &lt;tagName attributeName="(key)"&gt;(value)&lt;/tagName&gt;
     * ...
     * </pre>
     * <p>
     * Map keys are assumed to be strings or single objects with supported
     * conversion to string (see {@link GenericConverter}).
     * Map values can be single values or multi-values.  Arrays or collections
     * will have their values be treated as individual elements with the same
     * key name. In any case, single or multiple values are otherwise converted
     * to strings just like keys.
     * </p>
     * @param tagName name of tags for each map entries
     * @param attributeName name of the tag attribute holding the map entry key
     * @param map map to add
     * @return XML of parent tag, with nested element for each map entries, or
     *     <code>null</code> if map is <code>null</code>.
     */
    public List<Xml> addElementMap(
            String tagName, String attributeName, Map<?, ?> map) {
        if (MapUtils.isEmpty(map)) {
            return Collections.emptyList();
        }
        List<Xml> xmlList = new ArrayList<>();
        for (Entry<?, ?> en : map.entrySet()) {
            var name = GenericConverter.convert(en.getKey());
            CollectionUtil.toStringList(
                    CollectionUtil.adaptedList(en.getValue())).forEach(
                            v -> xmlList.add(addXML(tagName).setAttribute(
                                    attributeName, name).setTextContent(v)));
        }
        return Collections.unmodifiableList(xmlList);
    }

    /**
     * <p>
     * Adds a {@link Map} as a series of elements with a parent element
     * wrapping that group. Map keys are defined as element attributes
     * and the map value is the element content. The structure can be
     * visualized like this:
     * </p>
     * <pre>
     * &lt;parentTagName&gt;
     *   &lt;tagName attributeName="(key)"&gt;(value)&lt;/tagName&gt;
     *   &lt;tagName attributeName="(key)"&gt;(value)&lt;/tagName&gt;
     *   ...
     * &lt;/parentTagName&gt;
     * </pre>
     * <p>
     * Map keys are assumed to be strings or single objects with supported
     * conversion to string (see {@link GenericConverter}).
     * Map values can be single values or multi-values.  Arrays or collections
     * will have their values be treated as individual elements with the same
     * key name. In any case, single or multiple values are otherwise converted
     * to strings just like keys.
     * </p>
     * @param parentTagName required name of map elements wrapper tag
     * @param tagName name of tags for each map entries
     * @param attributeName name of the tag attribute holding the map entry key
     * @param map map to add
     * @return XML of parent tag, with nested element for each map entries, or
     *     <code>null</code> if map is <code>null</code>.
     */
    public Xml addElementMap(String parentTagName,
            String tagName, String attributeName, Map<?, ?> map) {
        Objects.requireNonNull(
                parentTagName, "'parentTagName' must not be null");
        var parentXml = addElement(parentTagName);
        parentXml.addElementMap(tagName, attributeName, map);
        return parentXml;
    }

    /**
     * Removes an element from this XML.
     * @param tagName element name
     * @return XML of the removed element
     */
    public Xml removeElement(String tagName) {
        var el = (Element) node;
        return new Xml(el.removeChild(getNode(tagName)));
    }

    /**
     * Removes itself from its XML parent (if any).
     * @return a new instance of this removed XML or this instance if
     * it is not attached to any parent
     */
    public Xml remove() {
        var parentNode = getNode().getParentNode();
        if (parentNode != null) {
            return new Xml(parentNode.removeChild(getNode()));
        }
        return this;
    }

    /**
     * Inserts a new XML node before this one, as a sibling of a shared parent.
     * If there is no parent to this XML, the new XML is not inserted.
     * @param newXML the XML to insert
     * @return a new instance of the inserted XML, or the inserted node
     * if this node is not attached to any parent.
     */
    public Xml insertBefore(Xml newXML) {
        var parentNode = getNode().getParentNode();
        if (parentNode != null) {
            var newNode = parentNode.getOwnerDocument().importNode(
                    newXML.getNode(), true);
            return new Xml(parentNode.insertBefore(newNode, getNode()));
        }
        return newXML;
    }

    /**
     * Inserts a new XML node after this one, as a sibling of a shared parent.
     * If there is no parent to this XML, the new XML is not inserted.
     * @param newXML the XML to insert
     * @return a new instance of the inserted XML, or the inserted node
     * if this node is not attached to any parent.
     */
    public Xml insertAfter(Xml newXML) {
        var parentNode = getNode().getParentNode();
        if (parentNode != null) {
            var newNode = parentNode.getOwnerDocument().importNode(
                    newXML.getNode(), true);
            return new Xml(parentNode.insertBefore(
                    newNode, getNode().getNextSibling()));
        }
        return newXML;
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
    public Xml setAttribute(String name, Object value) {
        //MAYBE: check if not a node, throw exception
        var el = (Element) node;
        if (value == null) {
            el.removeAttribute(name);
        } else if (GenericConverter.defaultInstance().isConvertible(
                value.getClass())) {
            el.setAttribute(name, GenericConverter.convert(value));
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
    public Xml setAttributes(Map<String, ?> attribs) {
        //MAYBE: check if not a node, throw exception
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
    public Xml setDelimitedAttributeList(String name, List<?> values) {
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
    public Xml setDelimitedAttributeList(
            String name, String delim, List<?> values) {
        if (values.isEmpty()) {
            return this;
        }
        setAttribute(name, join(delim, values)); //NOSONAR
        return this;
    }

    /**
     * Removes an attribute on this XML element.
     * @param name attribute name
     * @return this element
     */
    public Xml removeAttribute(String name) {
        var el = (Element) node;
        el.removeAttribute(name);
        return this;
    }

    /**
     * Sets the <strong>direct</strong> text content of an XML element.
     * To replace all child nodes with text, use
     * <code>xml.getNode().setTextContent("my content")</code>.
     * Setting <code>null</code> content is effectively the same as
     * invoking {@link #removeTextContent()}.
     * @param textContent text content
     * @return this element
     */
    public Xml setTextContent(Object textContent) {
        var content = Objects.toString(textContent, null);

        removeTextContent();

        // When no content to set, return right away
        if (content == null) {
            return this;
        }

        // Writing element text:
        var el = (Element) node;
        // remove existing xml:space=... attributes so set appropriate
        // ones based on the nature of the content.
        el.removeAttribute(ATT_XML_SPACE);
        if ("".equals(content)) {
            // If an empty string, mark as empty to prevent it from being
            // interpreted as null when read back. See getNodeString(...)
            el.setAttribute(ATT_XML_SPACE, "empty");
        } else {
            if (StringUtils.isWhitespace(content)) {
                // if contains only white space and not empty, add space preserve
                // to make sure white spaces are kept when read back.
                el.setAttribute(ATT_XML_SPACE, "preserve");
            }

            var document = node.getOwnerDocument();
            var textNode = document.createTextNode(content);
            node.appendChild(textNode);
        }
        return this;
    }

    /**
     * Gets this XML <strong>direct</strong> text content, if any, joining
     * multiple text nodes by a space separator. To
     * get all text content, including text content of child elements,
     * use <code>xml.getNode().getTextContent()</code>.
     * @return this XML text, or <code>null</code> if no text
     * @since 3.0.0
     */
    public String getTextContent() {
        var snippets = getStringList("text()");
        if (snippets.isEmpty()) {
            return null;
        }
        return StringUtils.join(snippets, " ");
    }

    /**
     * Removes this XML <strong>direct</strong> text content, without impact on
     * attributes and child elements and their content.
     * @return this XML without text content
     */
    public Xml removeTextContent() {
        var nodeList = node.getChildNodes();
        for (var i = 0; i < nodeList.getLength(); ++i) {
            var childNode = nodeList.item(i);
            if (childNode.getNodeType() == Node.TEXT_NODE) {
                node.removeChild(childNode);
            }
        }
        return this;
    }

    // returns the newly added XML
    public Xml addXML(Reader xml) {
        return addXML(createAndInitXML(Xml.of(xml)));
    }

    // returns the newly added XML
    public Xml addXML(String xml) {
        return addXML(createAndInitXML(Xml.of(xml)));
    }

    // returns the newly added XML
    public Xml addXML(Xml xml) {
        var childNode = node.getOwnerDocument().importNode(xml.node, true);
        node.appendChild(childNode);
        return createAndInitXML(Xml.of(childNode));
    }

    public Writer getXMLWriter() {
        return new StringWriter() {
            @Override
            public void close() throws IOException {
                var s = this.toString();
                if (StringUtils.isNotBlank(s)) {
                    addXML(s);
                }
            }
        };
    }

    @Deprecated(since = "3.0.0")
    public EnhancedXmlStreamWriter getXMLStreamWriter() {
        return new EnhancedXmlStreamWriter(getXMLWriter());
    }

    public void write(Writer writer) {
        write(writer, 0);
    }

    public void write(Writer writer, int indent) {
        try {
            writer.write(toString(indent));
        } catch (IOException e) {
            throw new XmlException("Could not write XML to Writer.", e);
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
            throw new XmlException(
                    "Could not write XML to file: " + file.getAbsolutePath(),
                    e);
        }
    }

    /**
     * Unwraps this XML by removing the root tag and keeping its child element
     * (and its nested element).
     * If there are no child (i.e., nothing to unwrap), invoking
     * this method has no effect.
     * If there are more than one child element, this method throws an
     * {@link XmlException}.
     * @return this XML, unwrapped
     */
    public Xml unwrap() {
        var children = node.getChildNodes();

        // If no child, end here
        if (children == null || children.getLength() == 0) {
            return this;
        }

        // If multiple children, throw exception
        if (children.getLength() > 1) {
            //MAYBE: do not throw to support XML without a parent?
            throw new XmlException("Cannot unwrap " + getName()
                    + " element as it contains multiple child elements.");
        }

        // Proceed with the unwrapping
        replace(createAndInitXML(Xml.of(children.item(0))));
        return this;
    }

    /**
     * Rename this XML (element tag name).
     * @param newName new name for this XML
     * @return this XML, renamed
     */
    public Xml rename(String newName) {
        var doc = node.getOwnerDocument();
        doc.renameNode(node, null, newName);
        return this;
    }

    /**
     * Wraps this XML by adding a parent element around it.
     * @param parentName name of wrapping element
     * @return this XML, wrapped
     */
    public Xml wrap(String parentName) {
        var doc = node.getOwnerDocument();
        var childNode = node.cloneNode(true);
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
    public Xml clear() {
        // clear child nodes
        while (node.hasChildNodes()) {
            node.removeChild(node.getFirstChild());
        }
        // clear attributes
        while (node.getAttributes().getLength() > 0) {
            var att = node.getAttributes().item(0);
            node.getAttributes().removeNamedItem(att.getNodeName());
        }
        return this;
    }

    /**
     * Gets whether this XML is empty. An XML is considered empty
     * if all of {@link #hasAttributes()}, {@link #hasChildElements()},
     * and {@link #hasTextContent()} return <code>false</code>.
     * @return <code>true</code> if empty
     * @since 3.0.0
     */
    public boolean isEmpty() {
        return !hasAttributes() && !hasTextContent() && !hasChildElements();
    }

    /**
     * Gets whether this XML element has any child elements.
     * @return <code>true</code> if this element has at least one child element
     * @since 3.0.0
     */
    public boolean hasChildElements() {
        return new NodeArrayList(node.getChildNodes()).stream()
                .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                .count() > 0;
    }

    /**
     * Gets whether this XML element has any attributes.
     * @return <code>true</code> if this element has at least one attribute
     * @since 3.0.0
     */
    public boolean hasAttributes() {
        return node.hasAttributes();
    }

    /**
     * Gets whether this XML has any text content.
     * @return <code>true</code> if non-<code>null</code> and non-empty.
     * @since 3.0.0
     */
    public boolean hasTextContent() {
        return StringUtils.isNotEmpty(node.getTextContent());
    }

    /**
     * Replaces the current XML with the provided one.
     * @param replacement replacing XML
     * @return this XML, replaced
     */
    public Xml replace(Xml replacement) {
        clear();
        var doc = node.getOwnerDocument();

        // overwrite parent node with child one
        var attrs = replacement.node.getAttributes();
        for (var i = 0; i < attrs.getLength(); i++) {
            node.getAttributes().setNamedItem(
                    doc.importNode(attrs.item(i), true));
        }
        while (replacement.node.hasChildNodes()) {
            var childNode = replacement.node.removeChild(
                    replacement.node.getFirstChild());
            node.appendChild(doc.importNode(childNode, true));
        }
        doc.renameNode(node, null, replacement.node.getNodeName());
        return this;
    }

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
            String xpathExpression, @NonNull Function<Xml, T> parser) {
        return parseXML(xpathExpression, parser, null);
    }

    public <T> T parseXML(
            String xpathExpression,
            @NonNull Function<Xml, T> parser,
            T defaultValue) {
        var xml = getXML(xpathExpression);
        if (xml == null) {
            return defaultValue;
        }
        return parser.apply(xml);
    }

    //MAYBE: allow to specify collection implementation?
    public <T> List<T> parseXMLList(
            String xpathExpression, @NonNull Function<Xml, T> parser) {
        return parseXMLList(xpathExpression, parser, null);
    }

    public <T> List<T> parseXMLList(
            String xpathExpression,
            @NonNull Function<Xml, T> parser,
            List<T> defaultValue) {

        var xmls = getXMLListOptional(xpathExpression);
        // We return:
        //   - an empty list if optional is empty.
        //   - the default list if optional is not emtpy but node list is
        //   - otherwise return the matching list
        if (!xmls.isPresent()) {
            return Collections.emptyList();
        }
        if (xmls.get().isEmpty()) {
            return defaultValue;
        }

        List<T> list = new ArrayList<>();
        for (Xml xml : xmls.get()) {
            if (xml != null) {
                var obj = parser.apply(xml);
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

    //MAYBE: have a formatXMLMap and others
    //MAYBE: allow to specify map implementation?
    public <K, V> Map<K, V> parseXMLMap(
            String xpathExpression, Function<Xml, Entry<K, V>> parser) {
        return parseXMLMap(xpathExpression, parser, null);
    }

    public <K, V> Map<K, V> parseXMLMap(
            String xpathExpression,
            Function<Xml, Entry<K, V>> parser,
            Map<K, V> defaultValue) {
        Objects.requireNonNull(parser, "Parser argument cannot be null.");

        var xmls = getXMLListOptional(xpathExpression);
        // We return:
        //   - an empty map if optional is empty.
        //   - otherwise return the parsed map
        if (!xmls.isPresent()) {
            return Collections.emptyMap();
        }

        Map<K, V> map = new ListOrderedMap<>();
        for (Xml xml : xmls.get()) {
            if (xml != null) {
                var entry = parser.apply(xml);
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
     * and log a warning or throw an {@link XmlException}.
     * @param deprecatedXPath xpath to the invalid entry
     * @param replacement new xpath or instructions to replace
     * @param throwException <code>true</code> to throw exception, else log
     *        a warning
     */
    public void checkDeprecated(String deprecatedXPath,
            String replacement, boolean throwException) {
        if (contains(deprecatedXPath)) {
            var b = new StringBuilder();
            b.append('"');
            if (deprecatedXPath.contains("@")) {
                b.append(StringUtils.substringAfterLast(deprecatedXPath, "@"));
                b.append('"');
                b.append(" attribute ");
            } else {
                b.append(deprecatedXPath);
                b.append('"');
                b.append(" element ");
            }
            b.append("has been deprecated");
            if (StringUtils.isNotBlank(replacement)) {
                b.append(" in favor of: ");
                b.append(replacement);
            }
            b.append(". Update your XML configuration accordingly.");
            if (throwException) {
                throw new XmlException(b.toString());
            }
            if (LOG.isWarnEnabled()) {
                LOG.warn(b.toString());
            }
        }
    }

    /**
     * Checks whether a deprecated configuration entry (without replacement)
     * was specified and log a warning or throw an {@link XmlException}.
     * @param deprecatedXPath xpath to the invalid entry
     * @param throwException <code>true</code> to throw exception, else log
     *        a warning
     */
    public void checkDeprecated(
            String deprecatedXPath, boolean throwException) {
        checkDeprecated(deprecatedXPath, null, throwException);
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public DocumentBuilderFactory getDocumentBuilderFactory() {
        return documentBuilderFactory;
    }

    public static boolean isXMLConfigurable(Object obj) {
        return obj instanceof XmlConfigurable;
    }

    public static boolean isJAXB(Object obj) {
        return obj != null && obj.getClass().isAnnotationPresent(
                XmlRootElement.class);
    }

    /**
     * Returns an {@link Iterator} of {@link XmlCursor} from this XML,
     * in sequential order.
     * Invoking a "read" methods on {@link XmlCursor} which reads child
     * elements will result in the iterator skipping those already read
     * elements.
     * @return XML cursor iterator
     */
    @Override
    public Iterator<XmlCursor> iterator() {
        return iterator(this);
    }

    /**
     * Returns a {@link Stream} of {@link XmlCursor} from this XML,
     * in sequential order.
     * Invoking a "read" methods on {@link XmlCursor} which reads child
     * elements will result in the stream skipping those already read
     * elements.
     * @return XML cursor stream
     */
    public Stream<XmlCursor> stream() {
        return stream(this);
    }

    /**
     * <p>
     * Returns an {@link Iterator} of {@link XmlCursor} from the supplied XML
     * object, in sequential order.
     * Invoking a "read" methods on {@link XmlCursor} which reads child
     * elements will result in the iterator skipping those already read
     * elements.
     * </p>
     * <p>
     * The object argument type must be one of the following:
     * </p>
     * <ul>
     *   <li>{@link Path}</li>
     *   <li>{@link File}</li>
     *   <li>{@link Node}</li>
     *   <li>{@link Xml}</li>
     *   <li>{@link String}</li>
     *   <li>{@link InputStream}</li>
     *   <li>{@link Reader}</li>
     *   <li>{@link XMLEventReader}</li>
     * </ul>
     *
     * @param obj the XML to iterate over
     * @return XML cursor iterator
     */
    public static Iterator<XmlCursor> iterator(Object obj) {
        return new XmlIterator(XmlUtil.createXMLEventReader(obj));
    }

    /**
     * <p>
     * Returns a {@link Stream} of {@link XmlCursor} from the supplied XML
     * object, in sequential order.
     * Invoking a "read" methods on {@link XmlCursor} which reads child
     * elements will result in the stream skipping those already read
     * elements.
     * </p>
     * <p>
     * The object argument type must be one of the following:
     * </p>
     * <ul>
     *   <li>{@link Path}</li>
     *   <li>{@link File}</li>
     *   <li>{@link Node}</li>
     *   <li>{@link Xml}</li>
     *   <li>{@link String}</li>
     *   <li>{@link InputStream}</li>
     *   <li>{@link Reader}</li>
     *   <li>{@link XMLEventReader}</li>
     * </ul>
     *
     * @param obj the XML to stream
     * @return XML cursor stream
     */
    public static Stream<XmlCursor> stream(Object obj) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iterator(obj),
                Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }

    /**
     * Joins multiple values using the supplied delimiter or a comma
     * if the delimiter is <code>null</code>.
     * @param delim delimiter
     * @param values the values to join
     * @return joined values, as a string
     * @deprecated Will be removed or visibility reduced in a future release.
     */
    @Deprecated(since = "3.0.0")
    public String join(String delim, List<?> values) { //NOSONAR
        var sep = Objects.toString(delim, ",");
        var b = new StringBuilder();
        for (Object obj : values) {
            var str = Objects.toString(obj, "").trim();
            if (StringUtils.isNotEmpty(str)) {
                if (b.length() > 0) {
                    b.append(sep);
                }
                b.append(str);
            }
        }
        return b.toString();
    }

    //--- Misc. Private Methods ------------------------------------------------

    private static DocumentBuilderFactory defaultIfNull(
            DocumentBuilderFactory dbf) {
        return Optional.ofNullable(dbf).orElseGet(() -> {
            var factory =
                    XmlUtil.createDocumentBuilderFactory();
            factory.setNamespaceAware(false);
            factory.setIgnoringElementContentWhitespace(false);
            return factory;
        });
    }

    private static ErrorHandler defaultIfNull(ErrorHandler eh) {
        return Optional.ofNullable(eh).orElseGet(
                () -> new ErrorHandlerFailer(Xml.class));
    }

    private boolean isDefined() {
        return node != null;
    }

    // When calling this method, empty tags would have added a xml:space="empty"
    // custom attribute. Else, if it has no child nodes
    // (attributes, text, elements), we consider it as an explicitly
    // self-closed tag, thus null.
    private boolean isExplicitNull() {
        return isDefined() && !node.hasAttributes() && !node.hasChildNodes();
    }

    private Xml createAndInitXML(Builder builder) {
        return builder
                .setDocumentBuilderFactory(documentBuilderFactory)
                .setErrorHandler(errorHandler)
                .create();
    }

    private static void handleException(
            String rootNode, String key, Exception e) {
        // Throw exception
        if (e instanceof XmlException xmlEx) {
            throw xmlEx;
        }
        throw new XmlException(
                "Could not instantiate object from configuration "
                        + "for \"" + rootNode + " -> " + key + "\".",
                e);
    }

    // For some reason, the following is required as a workaround
    // to indentation not working properly. Taken from:
    // https://myshittycode.com/2014/02/10/
    //         java-properly-indenting-xml-string/
    private void fixIndent(int indent) {
        if (indent > 0) {
            var xPath = XpathUtil.newXPath();
            try {
                var nodeList = (NodeList) xPath.evaluate(
                        "//text()[normalize-space()='']",
                        node, XPathConstants.NODESET);
                for (var i = 0; i < nodeList.getLength(); ++i) {
                    var n = nodeList.item(i);
                    n.getParentNode().removeChild(n);
                }
            } catch (XPathExpressionException e) {
                LOG.error("Could not indent XML.", e);
            }
        }
    }

    private static String getXSDResourcePath(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return "/" + clazz.getCanonicalName().replace('.', '/') + ".xsd";
    }

    // Filter out "xml:" name space so attributes like xml:space="preserve"
    // are validated OK even if name space not declared in a schema.
    private static class W3XMLNamespaceFilter extends XMLFilterImpl {
        public W3XMLNamespaceFilter(XMLReader parent) {
            super(parent);
        }

        @Override
        public void startElement(
                String uri, String localName, String qName, Attributes atts)
                throws SAXException {
            for (var i = 0; i < atts.getLength(); i++) {
                if (XMLConstants.XML_NS_URI.equals(atts.getURI(i))) {
                    var modifiedAtts = new AttributesImpl(atts);
                    modifiedAtts.removeAttribute(i);
                    super.startElement(uri, localName, qName, modifiedAtts);
                    return;
                }
            }
            super.startElement(uri, localName, qName, atts);
        }
    }

    private String getNodeString(Node n) {
        if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
            return n.getNodeValue();
        }

        // Unlike standard XML parsing, we distinguish between
        // self-closed tags (null) and empty/blank ones (non-null).
        // To do so, we need to check if empty tags were detected
        // BEFORE parsing the XML. Those would have been are identified
        // with an extra attribute xml:space="empty" by Builder#create.
        // Those containing white spaces cannot be confused with self-closed
        // so we do not rely on xml:space="empty", but we check if they
        // have the standard xml:space="preserve" to decide if we trim them
        // or not.

        var str = n.getTextContent();

        Optional<String> xmlSpace = Optional
                .ofNullable(n.getAttributes())
                .map(nnm -> nnm.getNamedItem(ATT_XML_SPACE))
                .map(Node::getNodeValue);

        // Empty tags are converted to "" while self-closed to null:
        if (StringUtils.isEmpty(str)) {
            return xmlSpace.filter("empty"::equals).isPresent() ? "" : null;
        }

        // Other values are trimmed unless xml:space is "preserve":
        if (!xmlSpace.filter("preserve"::equals).isPresent()) {
            str = str.trim();
        }
        return str;
    }
}
