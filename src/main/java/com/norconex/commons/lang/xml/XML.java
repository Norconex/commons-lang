/* Copyright 2010-2021 Norconex Inc.
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
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
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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
import com.norconex.commons.lang.convert.Converter;
import com.norconex.commons.lang.convert.ConverterException;
import com.norconex.commons.lang.time.DurationParser;
import com.norconex.commons.lang.time.DurationParserException;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.commons.lang.unit.DataUnitParser;

//TODO consider checking for a "disable=false|true" and setting it on
//a method if this method exists, and/or do not load if set to true.

//TODO add "addStringMap"

//TODO have a look at Collections.emptyList() and do similar to have
// getObject return any type instead of having tons of get methods.

//TODO getElement(s) and getAttribute(s) which are direct references to
// elements attributes (not xpath)


/**
 * <p>
 * XML DOM wrapper facilitating node querying and automatically creating,
 * validating, and populating classes from/to XML, with support
 * for {@link IXMLConfigurable} and {@link JAXB}.
 * </p>
 * <h3>XML syntax and white spaces</h3>
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
 * Checked exceptions are wrapped into an {@link XMLException}.
 * </p>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class XML {

    private static final Logger LOG = LoggerFactory.getLogger(XML.class);

    private static final String DEFAULT_DELIM_REGEX = "(\\s*,\\s*)+";
    private static final String NULL_XML_VALUE = "\u0000";
    private static final List<String> NULL_XML_LIST = new ArrayList<>(0);
    private static final String XPATH_ATT_CLASS = "@class";
    private static final String ATT_XML_SPACE = "xml:space";

    private final Node node;
    private final ErrorHandler errorHandler;
    private final DocumentBuilderFactory documentBuilderFactory;


    /**
     * <p>Parse an XML file into an XML document, without consideration
     * for namespaces.</p>
     * @param file the XML file to parse
     * @see #of(Path)
     */
    public XML(Path file) {
        this(XML.of(file).create().node);
    }
    /**
     * <p>Parse an XML file into an XML document, without consideration
     * for namespaces.</p>
     * @param file the XML file to parse
     * @see #of(File)
     */
    public XML(File file) {
        this(XML.of(file).create().node);
    }
    /**
     * <p>Parse an XML stream into an XML document, without consideration
     * for namespaces.</p>
     * @param reader the XML stream to parse
     * @see #of(Reader)
     */
    public XML(Reader reader) {
        this(XML.of(reader).create().node);
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
     * @see #of(String)
     */
    public XML(String xml) {
        this(XML.of(xml).create().node);
    }

    public XML(String rootElement, Object obj) {
        this(XML.of(rootElement, obj).create().node);
    }

    /**
     * <p>Creates an XML with the given node.</p>
     * @param node the node representing the XML
     */
    public XML(Node node) {
        this(node, null, null, null);
    }

    /**
     * <p>Creates an XML with the given node.</p>
     * @param node the node representing the XML
     */
    private XML(
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
            } else if (Converter.defaultInstance().isConvertible(
                    sourceObject.getClass())) {
                setTextContent(Converter.convert(sourceObject));
            } else {
                setAttribute("class",
                        sourceObject.getClass().getCanonicalName());
                if (isXMLConfigurable(sourceObject)) {
                    ((IXMLConfigurable) sourceObject).saveToXML(this);
                } else if (isJAXB(sourceObject)) {
                    jaxbMarshall(sourceObject);
                }
            }
        }
    }

    private static DocumentBuilderFactory defaultIfNull(
            DocumentBuilderFactory dbf) {
        return Optional.ofNullable(dbf).orElseGet(() -> {
            DocumentBuilderFactory factory =
                    XMLUtil.createDocumentBuilderFactory();
            factory.setNamespaceAware(false);
            factory.setIgnoringElementContentWhitespace(false);
            return factory;
        });
    }
    private static ErrorHandler defaultIfNull(ErrorHandler eh) {
        return Optional.ofNullable(eh).orElseGet(
                    () -> new ErrorHandlerFailer(XML.class));
    }

    private void jaxbMarshall(Object obj) {
        try {
            String name = node.getNodeName();
            List<Attr> attributes = new ArrayList<>();
            NamedNodeMap nattributes = node.getAttributes();
            for (int i = 0; i < nattributes.getLength(); i++) {
                attributes.add((Attr) nattributes.item(i));
            }

            JAXBContext contextObj = JAXBContext.newInstance(obj.getClass());
            Marshaller marshallerObj = contextObj.createMarshaller();
            marshallerObj.marshal(obj, node);

            unwrap();

            Element el = ((Element) node);
            for (Attr at : attributes) {
                el.setAttributeNS(
                        at.getNamespaceURI(), at.getName(), at.getValue());
            }
            rename(name);
        } catch (Exception e) {
            throw new XMLException(
                    "This object could not be JAXB-marshalled: " + obj, e);
        }
    }

    private boolean isDefined() {
        return node != null;
    }

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

    public Node toNode() {
        return node;
    }

    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on this XML root node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.
     * If the class is annotated with an
     * {@link XmlRootElement}, it will use JAXB to unmarshall it to an object.
     * </p>
     * <p>
     * Performs XML validation if the target object has an associated schema.
     * </p>
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLValidationException if the XML has validation errors
     * @throws XMLException if something prevented object creation
     */
    public <T> T toObject() {
        return toObject(null);
    }
    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
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
     * @throws XMLException if something prevented object creation
     */
    @SuppressWarnings("unchecked")
    public <T> T toObject(T defaultObject) {
        return toObject((Class<T>) getClass(XPATH_ATT_CLASS, null), defaultObject);
    }

    private <T> T toObject(Class<T> objClass, T defaultObject) {
        if (node == null) {
            return defaultObject;
        }
        T obj;
        if (objClass != null) {
            try {
                obj = objClass.newInstance();
            } catch (Exception e) {
                throw new XMLException(
                        "This class could not be instantiated: " + objClass, e);
            }
        } else {
            LOG.debug("A configuration entry was found without a class "
                   + "attribute where one could have been provided; "
                   + "using default value: {}", defaultObject);
            obj = defaultObject;
        }

        populate(obj);
        return obj;
    }

    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on this XML root node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
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
     * more than one is found, an {@link XMLException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #toObject()} method, which is faster.
     * </p>
     * @param type the expected class (sub)type to return
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLValidationException if the XML has validation errors
     * @throws XMLException if something prevented object creation
     */
    public <T> T toObjectImpl(Class<?> type) {
        return toObjectImpl(type, null);
    }
    /**
     * <p>
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
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
     * more than one is found, an {@link XMLException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #toObject(Object)} method, which is faster.
     * </p>
     * @param type the expected class (sub)type to return
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLException if something prevented object creation
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
            if (e.getCause() instanceof ClassNotFoundException) {
                String partialName = getString(XPATH_ATT_CLASS);
                List<?> results = ClassFinder.findSubTypes(
                        type, s -> s.endsWith(partialName));
                if (results.size() > 1) {
                    throw new XMLException(results.size()
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

                if (results.isEmpty()) {
                    throw new XMLException(
                              "No class implementing \""
                            + type.getName() + "\" "
                            + "and ending with \"" + partialName + "\" "
                            + "could be found. Check your classpath or "
                            + "consider using fully qualified class name.");
                }
                obj = toObject((Class<T>) results.get(0), defaultObject);
            } else {
                throw e;
            }
        }
        if (obj != null && !type.isInstance(obj)) {
            throw new XMLException(
                    obj.getClass() + " is not an instance of " + type);
        }
        return obj;
    }

    /**
     * <p>
     * Populates supplied object with the XML matching the given expression.
     * If there is no match, the object does not get populated.
     * Takes into consideration whether the target object implements
     * {@link IXMLConfigurable} or JAXB.
     * </p>
     * <p>
     * Performs XML validation if the target object has an associated schema.
     * </p>
     * @param xpathExpression XPath expression
     * @param targetObject object to populate with this XML
     * @return validation errors of an empty list if none
     *         (never <code>null</code>)
     */
    public List<XMLValidationError> populate(
            Object targetObject, String xpathExpression) {
        List<XMLValidationError> errs = new ArrayList<>();
        ifXML(xpathExpression, x -> errs.addAll(x.populate(targetObject)));
        return errs;
    }

    /**
     * <p>
     * Populates supplied object with this XML. Takes into consideration
     * whether the target object implements {@link IXMLConfigurable} or
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
     * @return validation errors or an empty list if none
     *         (never <code>null</code>)
     */
    public List<XMLValidationError> populate(Object targetObject) {
        if (node == null || targetObject == null) {
            return Collections.emptyList();
        }
        try {
            List<XMLValidationError> errs = validate(targetObject.getClass());
            if (isXMLConfigurable(targetObject)) {
                ((IXMLConfigurable) targetObject).loadFromXML(this);
            } else if (isJAXB(targetObject)) {
                jaxbUnmarshall(targetObject);
            }
            return errs;
        } catch (XMLException e) {
            throw e;
        } catch (Exception e) {
            throw new XMLException("XML (tag: <" + getName() + ">) "
                    + "could not be converted to object of type: "
                            + targetObject.getClass(), e);
        }
    }

    private void jaxbUnmarshall(Object obj) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
            Unmarshaller unmarsh = jaxbContext.createUnmarshaller();
            JAXBElement<?> newObj = unmarsh.unmarshal(node, obj.getClass());
            BeanUtil.copyProperties(obj, newObj.getValue());
        } catch (Exception e) {
            throw new XMLException("XML (tag: <" + getName() + ">) "
                    + " could not be JAXB-unmarshalled: " + this, e);
        }
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
        return getObject(xpathExpression, (T) null);
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
                    node.getNodeName(), xpathExpression, e);
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
        Optional<List<XML>> xmls = getXMLListOptional(xpathExpression);
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
        for (XML xml : xmls.get()) {
            if (xml != null) {
                T obj = xml.toObject();
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
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #getObject(String)} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XMLException} will be thrown.
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
     * If the class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
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
     * more than one is found, an {@link XMLException} will be thrown.
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
            XML xml = getXML(xpathExpression);
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
     * If a class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
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
     * more than one is found, an {@link XMLException} will be thrown.
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
     * @throws XMLException if instance cannot be created/populated
     */
    public <T> List<T> getObjectListImpl(
            Class<?> type, String xpathExpression, List<T> defaultObjects) {

        Optional<List<XML>> xmls = getXMLListOptional(xpathExpression);
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
        for (XML xml : xmls.get()) {
            if (xml != null) {
                T obj = xml.toObjectImpl(type);
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
     * If a class is an instance of {@link IXMLConfigurable}, the object
     * created will be automatically populated by invoking the
     * {@link IXMLConfigurable#loadFromXML(XML)} method,
     * passing it the node XML.</p>
     *
     * <p>This method should throw a
     * {@link XMLException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     * <p>
     * Other than making sure the class is a subtype of the specified
     * super class, the main difference between method this and
     * {@link #getObjectList(String)} is the support for partial class names.
     * That is, this method will scan the current class loader for a class
     * with its name ending with the value of the "class" attribute.  If
     * more than one is found, an {@link XMLException} will be thrown.
     * If you are expecting fully qualified class names, use the
     * {@link #getObjectList(String)} method, which is faster.
     * </p>
     *
     * @param type the expected class (sub)type to return
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLException if instance cannot be created/populated
     */
    public <T> List<T> getObjectListImpl(
            Class<?> type, String xpathExpression) {
        return getObjectListImpl(
                type, xpathExpression, Collections.emptyList());
    }

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
        return createAndInitXML(XML.of(xmlNode));
    }
    /**
     * If the given expression matches an element, consume that
     * element.
     * @param xpathExpression expression
     * @param then XML consumer
     */
    public void ifXML(String xpathExpression, Consumer<XML> then) {
        XML xml = getXML(xpathExpression);
        if (xml != null && xml.isDefined() && then != null) {
            then.accept(xml);
        }
    }



    /**
     * If the given expression matches one or more elements, consume those
     * element one by one.
     * @param xpathExpression expression
     * @param action The action to be performed for each element
     */
    public void forEach(String xpathExpression, Consumer<XML> action) {
        List<XML> xmlList = getXMLList(xpathExpression);
        xmlList.forEach(x -> {
            if (x != null && x.isDefined() && action != null) {
                action.accept(x);
            }
        });
    }

    private XML createAndInitXML(Builder builder) {
        return builder
                .setDocumentBuilderFactory(documentBuilderFactory)
                .setErrorHandler(errorHandler)
                .create();
    }

    /**
     * Gets the XML subsets matching the xpath expression.
     * @param xpathExpression expression to match
     * @return XML list, never <code>null</code>
     */
    public List<XML> getXMLList(String xpathExpression) {
        Optional<List<XML>> xmls = getXMLListOptional(xpathExpression);
        // We return:
        //   - an empty list if optional is empty.
        //   - otherwise return the matching list
        if (!xmls.isPresent()) {
            return Collections.emptyList();
        }
        return xmls.get();
    }

    private Optional<List<XML>> getXMLListOptional(String xpathExpression) {

        Optional<NodeArrayList> nodeList = getNodeList(xpathExpression);
        // We return:
        //   - an empty Optional if nodeList Optional is empty.
        //   - otherwise return the matching list
        if (!nodeList.isPresent()) {
            return Optional.empty();
        }
        List<XML> list = new ArrayList<>();
        for (Node n : nodeList.get()) {
            list.add(createAndInitXML(XML.of(n)));
        }
        return Optional.of(list);
    }

    private static void handleException(
            String rootNode, String key, Exception e) {
        // Throw exception
        if (e instanceof XMLException) {
            throw (XMLException) e;
        }
        throw new XMLException(
                "Could not instantiate object from configuration "
              + "for \"" + rootNode + " -> " + key + "\".", e);
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

            fixIndent(indent);

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
            // convert self-closing tags with "empty" attribute to empty tags
            // instead
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
    // For some reason, the following is required as a workaround
    // to indentation not working properly. Taken from:
    // https://myshittycode.com/2014/02/10/
    //         java-properly-indenting-xml-string/
    private void fixIndent(int indent) {
        if (indent > 0) {
            XPath xPath = XPathFactory.newInstance().newXPath();
            try {
                NodeList nodeList = (NodeList) xPath.evaluate(
                        "//text()[normalize-space()='']",
                        node, XPathConstants.NODESET);
                for (int i = 0; i < nodeList.getLength(); ++i) {
                    Node n = nodeList.item(i);
                    n.getParentNode().removeChild(n);
                }
            } catch (XPathExpressionException e) {
                LOG.error("Could not indent XML.", e);
            }
        }
    }

    /**
     * <p>
     * Validates this XML against an XSD schema attached to the class
     * represented in this XML root tag "class" attribute.
     * In addition to being returned, some validation errors/warnings may be
     * logged.
     * The schema is expected to be found at the same classpath location and
     * have the same name as the object class, but with the ".xsd" extension.
     * </p>
     * <p>
     * This method is the same as invoking
     * <code>validate(getClass("@class"))</code>
     * </p>
     * @return list of errors/warnings or empty (never <code>null</code>)
     */
    public List<XMLValidationError> validate() {
        return validate(getClass(XPATH_ATT_CLASS));
    }

    /**
     * <p>
     * Validates this XML for objects having an XSD schema attached,
     * and logs any error/warnings.
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
        if (obj == null) {
            return validate(null);
        }
        return validate(obj.getClass());
    }

    /**
     * Validates this XML for classes having an XSD schema attached,
     * and logs any error/warnings.
     * The schema expected to be found at the same classpath location and have
     * the same name as the object class, but with the ".xsd" extension.
     * @param clazz the class to validate the XML for
     * @return unmodifiable list of errors/warnings or empty
     *         (never <code>null</code>)
     */
    public List<XMLValidationError> validate(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }

        // Only validate if .xsd file exist in classpath for class
        String xsdResource = ClassUtils.getSimpleName(clazz) + ".xsd";
        LOG.debug("Validating XML for class {}",
                ClassUtils.getSimpleName(clazz));
        if (clazz.getResource(xsdResource) == null) {
            LOG.debug("XSD schema not found for validation: {}", xsdResource);
            return Collections.emptyList();
        }

        try (InputStream xsdStream = clazz.getResourceAsStream(xsdResource);
                Reader xmlReader = toReader()) {
            return validate(clazz, xsdStream, xmlReader);
        } catch (SAXException | IOException e) {
            throw new XMLException("Could not validate class: " + clazz, e);
        }
    }
    private List<XMLValidationError> validate(
            Class<?> clazz,
            InputStream xsdStream,
            Reader reader) throws SAXException, IOException {

        // See also: https://github.com/OWASP/CheatSheetSeries/blob/master/
        // cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.md

        List<XMLValidationError> errors = new ArrayList<>();

        SchemaFactory schemaFactory = XMLUtil.createSchemaFactory();
        schemaFactory.setResourceResolver(new ClasspathResourceResolver(clazz));

        Schema schema = schemaFactory.newSchema(
                new StreamSource(xsdStream, getXSDResourcePath(clazz)));
        Validator validator = XMLUtil.createSchemaValidator(schema);

        validator.setErrorHandler(errorHandler);
        XMLReader xmlReader = XMLUtil.createXMLReader();

        SAXSource saxSource = new SAXSource(
                new W3XMLNamespaceFilter(xmlReader), new InputSource(reader));
        validator.validate(saxSource);
        return Collections.unmodifiableList(errors);
    }

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
            XML xml = XML.of(elementName, xmlConfigurable).create();
            xml.write(out);
            xmlStr = out.toString();
        } catch (IOException e) {
            throw new XMLException("Could not save XML.", e);
        }
        LOG.trace(xmlStr);

        // Read
        XML xml = XML.of(xmlStr).create();
        IXMLConfigurable readConfigurable = xml.toObject();
        if (!xmlConfigurable.equals(readConfigurable)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(" SAVED: {}", xmlConfigurable);
                LOG.error("LOADED: {}", readConfigurable);
                LOG.error("  DIFF: \n{}\n",
                        BeanUtil.diff(xmlConfigurable, readConfigurable));
            }
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
            for (int i = 0; i < atts.getLength(); i++) {
                if (XMLConstants.XML_NS_URI.equals(atts.getURI(i))) {
                    AttributesImpl modifiedAtts = new AttributesImpl(atts);
                    modifiedAtts.removeAttribute(i);
                    super.startElement(uri, localName, qName, modifiedAtts);
                    return;
                }
            }
            super.startElement(uri, localName, qName, atts);
        }
    }

    public static XPath newXPath() {
        // Consider caching w/ ThreadLocal if performance becomes a concern
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

    private Optional<NodeArrayList> getNodeList(String xpathExpression) {
        try {
            NodeList nodeList = (NodeList) newXPathExpression(xpathExpression)
                    .evaluate(node, XPathConstants.NODESET);

            if (nodeList != null && nodeList.getLength() > 0) {
                return Optional.of(new NodeArrayList(nodeList));
            }
            // When there are no node list returned, we check if it is because
            // the xpath expression did not match anything (in which case
            // it may suggest to use a default value) or it did match
            // a tag but it was empty (indicating wanting to clear any
            // existing list).
            String xpath = substringBeforeLast(xpathExpression, "/");
            XML xmlTag = getXML(xpath);
            if (xmlTag == null || StringUtils.isBlank(xmlTag.toString())) {
                return Optional.of(new NodeArrayList((NodeList) null));
            }
            // If we get this far, there was a tag declared, so we treat
            // it as an explicit request to clear so we do not return anything
            // as a way to communicate that.
            return Optional.empty();
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate XPath expression: '"
                            + xpathExpression + "'.", e);
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
        Optional<NodeArrayList> nodeList = getNodeList(xpathExpression);
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
     * @return map of strings, never <code>null</code> unless default value
     *         is returned and is <code>null</code>
     */
    public Map<String, String> getStringMap(String xpathList, String xpathKey,
            String xpathValue, Map<String, String> defaultValues) {

        Optional<List<XML>> xmls = getXMLListOptional(xpathList);
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
        for (XML xml : xmls.get()) {
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
        BigDecimal sz = DataUnitParser.parse(getString(
                xpathExpression, null), targetUnit, BigDecimal.valueOf(-1));
        if (sz.longValue() == -1) {
            return defaultValue;
        }
        return sz.longValue();
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

    public String getName() {
        if (node == null) {
            return null;
        }
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
     * {@link XML#of(String, Object)}
     * @param tagName element name
     * @param value element value
     * @return XML of the added element or <code>null</code> if value is
     *         <code>null</code>
     */
    public XML addElement(String tagName, Object value) {
        XML xml = createAndInitXML(XML.of(tagName, value));
        Node newNode = node.getOwnerDocument().importNode(xml.node, true);
        return createAndInitXML(XML.of(node.appendChild(newNode)));
    }

    public List<XML> addElementList(String tagName, List<?> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        List<XML> xmlList = new ArrayList<>();
        for (Object value : values) {
            xmlList.add(addElement(tagName, value));
        }
        return Collections.unmodifiableList(xmlList);
    }
    public XML addElementList(
            String parentTagName, String tagName, List<?> values) {
        Objects.requireNonNull(
                parentTagName, "'parentTagName' must not be null");
        XML parentXml = addElement(parentTagName);
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
    public XML addDelimitedElementList(String name, List<?> values) {
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
    public XML addDelimitedElementList(
            String name, String delim, List<?> values) {
        if (values.isEmpty()) {
            return addElement(name, "");
        }
        return addElement(name, join(delim, values));
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
     * conversion to string (see {@link Converter}).
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
    public List<XML> addElementMap(
            String tagName, String attributeName, Map<?, ?> map) {
        if (MapUtils.isEmpty(map)) {
            return Collections.emptyList();
        }
        List<XML> xmlList = new ArrayList<>();
        for (Entry<?, ?> en: map.entrySet()) {
            String name = Converter.convert(en.getKey());
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
     * conversion to string (see {@link Converter}).
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
    public XML addElementMap(String parentTagName,
            String tagName, String attributeName, Map<?, ?> map) {
        Objects.requireNonNull(
                parentTagName, "'parentTagName' must not be null");
        XML parentXml = addElement(parentTagName);
        parentXml.addElementMap(tagName, attributeName, map);
        return parentXml;
    }

    /**
     * Removes an element from this XML.
     * @param tagName element name
     * @return XML of the removed element
     */
    public XML removeElement(String tagName) {
        Element el = (Element) node;
        return new XML(el.removeChild(getNode(tagName)));
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
     * Removes an attribute on this XML element.
     * @param name attribute name
     * @return this element
     */
    public XML removeAttribute(String name) {
        Element el = (Element) node;
        el.removeAttribute(name);
        return this;
    }

    /**
     * Sets the text content of an XML element.
     * @param textContent text content
     * @return this element
     */
    public XML setTextContent(Object textContent) {
        String content = Objects.toString(textContent, null);

        // When no content to set, return write away
        if (content == null) {
            return this;
        }

        // Writing element text:
        Element el = (Element) node;
        // remove existing xml:space=... attributes so set appropriate
        // ones based on the nature of the content.
        el.removeAttribute(ATT_XML_SPACE);
        if ("".equals(content)) {
            // If an empty string, mark as empty to prevent it from being
            // interpreted as null when read back. See getNodeString(...)
            el.setAttribute(ATT_XML_SPACE, "empty");
        } else if (StringUtils.isWhitespace(content)) {
            // if contains only white space and not empty, add space preserve
            // to make sure white spaces are kept when read back.
            el.setAttribute(ATT_XML_SPACE, "preserve");
            el.setTextContent(content);
        } else {
            el.setTextContent(content);
        }
        return this;
    }

    // returns the newly added XML
    public XML addXML(Reader xml) {
        return addXML(createAndInitXML(XML.of(xml)));
    }
    // returns the newly added XML
    public XML addXML(String xml) {
        return addXML(createAndInitXML(XML.of(xml)));
    }
    // returns the newly added XML
    public XML addXML(XML xml) {
        Node childNode = node.getOwnerDocument().importNode(xml.node, true);
        node.appendChild(childNode);
        return createAndInitXML(XML.of(childNode));
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
        replace(createAndInitXML(XML.of(children.item(0))));
        return this;
    }

    /**
     * Rename this XML (element tag name).
     * @param newName new name for this XML
     * @return this XML, renamed
     */
    public XML rename(String newName) {
        Document doc = node.getOwnerDocument();
        doc.renameNode(node, null, newName);
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

    //--- URL ------------------------------------------------------------------
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

    //--------------------------------------------------------------------------

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

        String str = n.getTextContent();

        Optional<String> xmlSpace = Optional
            .ofNullable(n.getAttributes().getNamedItem(ATT_XML_SPACE))
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

    //TODO isDisabled (which also reads disable, ignore, ignored
    // and give warnings when not "disabled" (or rely on validation,
    // changing all ignore to "disabled"

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

        Optional<List<XML>> xmls = getXMLListOptional(xpathExpression);
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
        for (XML xml : xmls.get()) {
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

        Optional<List<XML>> xmls = getXMLListOptional(xpathExpression);
        // We return:
        //   - an empty map if optional is empty.
        //   - otherwise return the parsed map
        if (!xmls.isPresent()) {
            return Collections.emptyMap();
        }

        Map<K,V> map = new ListOrderedMap<>();
        for (XML xml : xmls.get()) {
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
     * @param replacement new xpath or instructions to replace
     * @param throwException <code>true</code> to throw exception, else log
     *        a warning
     */
    public void checkDeprecated(String deprecatedXPath,
            String replacement, boolean throwException) {
        if (contains(deprecatedXPath)) {
            StringBuilder b = new StringBuilder();
            b.append('"');
            b.append(StringUtils.substringAfterLast(deprecatedXPath, "@"));
            b.append('"');
            if (deprecatedXPath.contains("@")) {
                b.append(" attribute ");
            } else {
                b.append(" element ");
            }
            b.append("has been deprecated");
            if (StringUtils.isNotBlank(replacement)) {
                b.append(" in favor of: ");
                b.append(replacement);
            }
            b.append(". Update your XML configuration accordingly.");
            if (throwException) {
                throw new XMLException(b.toString());
            }
            if (LOG.isWarnEnabled()) {
                LOG.warn(b.toString());
            }
        }
    }
    /**
     * Checks whether a deprecated configuration entry (without replacement)
     * was specified and log a warning or throw an {@link XMLException}.
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
        return obj instanceof IXMLConfigurable;
    }
    public static boolean isJAXB(Object obj) {
        return obj != null && obj.getClass().isAnnotationPresent(
                XmlRootElement.class);
    }

    public static Builder of(File file) {
        return new Builder(file);
    }
    public static Builder of(Path path) {
        return new Builder(path);
    }
    public static Builder of(Node node) {
        return new Builder(node);
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
            super();
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
        public XML create() {
            this.errorHandler = defaultIfNull(errorHandler);
            this.documentBuilderFactory = defaultIfNull(documentBuilderFactory);

            if (source instanceof Node) {
                return new XML((Node) source,
                        null, errorHandler, documentBuilderFactory);
            }

            String xmlStr = null;
            if (StringUtils.isNotBlank(rootElementName)) {
                xmlStr = "<" + rootElementName + "/>";
            } else if (source instanceof Path) {
                xmlStr = fileToString(((Path) source).toFile());
            } else if (source instanceof File) {
                xmlStr = fileToString((File) source);
            } else if (source instanceof Reader) {
                xmlStr = readerToString((Reader) source);
            } else if (source instanceof String) {
                xmlStr = (String) source;
            }

            if (StringUtils.isBlank(xmlStr)) {
                return new XML((Node) null,
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
                documentBuilderFactory.setNamespaceAware(false);
                node = documentBuilderFactory.newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xmlStr)))
                            .getDocumentElement();
            } catch (ParserConfigurationException
                    | SAXException | IOException e) {
                throw new XMLException("Could not parse XML.", e);
            }

            Object sourceObject = null;
            if (rootElementName != null && source != null) {
                sourceObject = source;
            }
            return new XML(
                    node, sourceObject, errorHandler, documentBuilderFactory);
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
                throw new XMLException("Could not read XML file: "
                        + file.getAbsolutePath(), e);
            }
        }
    }
}
