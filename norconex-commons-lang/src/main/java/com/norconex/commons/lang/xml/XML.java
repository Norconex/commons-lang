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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xni.NamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.XMLValidationError.Severity;

//TODO Add some of the convenience methods found in Collector/Crawler Loader?
//TODO consider checking for a "disable=false|true" and setting it on 
//a method if this method exists, and/or do not load if set to true.

/**
* XML DOM wrapper facilitating node querying and automatically creating,
* validating, and populating classes from XML.
* Checked exceptions are wrapped into an {@link XMLException}.
* @author Pascal Essiembre
* @since 2.0.0
*/
public class XML {

    private static final Logger LOG = 
            LoggerFactory.getLogger(XML.class);
    
    public static final String W3C_XML_SCHEMA_NS_URI_1_1 = 
            "http://www.w3.org/XML/XMLSchema/v1.1";
    
    private final Node node;
    
    /**
     * <p>Parse an XML stream into an XML document, without consideration
     * for namespaces.</p>
     * <p><b>Note:</b> Leading and trailing white spaces are not preserved by 
     * default.
     * To preserve them, add <code>xml:space="preserve"</code> 
     * to your tag, like this:
     * </p>
     * <pre>
     *   &lt;mytag xml:space="preserve"&gt; &lt;/mytag&gt;
     * </pre>
     * <p>The above example will preserve the white space in the tag's body.</p>
     * @param reader the XML stream to parse
     */
    public XML(Reader reader) {
        this(reader, createDefaultFactory());
    }
    /**
     * <p>Parse an XML stream into an XML document, using the provided
     * document builder factory.</p>
     * <p><b>Note:</b> Leading and trailing white spaces are not preserved by 
     * default.
     * To preserve them, add <code>xml:space="preserve"</code> 
     * to your tag, like this:
     * </p>
     * <pre>
     *   &lt;mytag xml:space="preserve"&gt; &lt;/mytag&gt;
     * </pre>
     * <p>The above example will preserve the white space in the tag's body.</p>
     * @param reader the XML stream to parse
     * @param factory the document builder factory
     */
    public XML(Reader reader, DocumentBuilderFactory factory) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.node = builder.parse(
                    new InputSource(reader)).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new XMLException("Could not parse XML.", e);
        }
    }
    /**
     * <p>Parse an XML string into an XML document, without consideration
     * for namespaces.</p>
     * <p><b>Note:</b> Leading and trailing white spaces are not preserved by 
     * default.
     * To preserve them, add <code>xml:space="preserve"</code> 
     * to your tag, like this:
     * </p>
     * <pre>
     *   &lt;mytag xml:space="preserve"&gt; &lt;/mytag&gt;
     * </pre>
     * <p>The above example will preserve the white space in the tag's body.</p>
     * @param xml the XML string to parse
     */
    public XML(String xml) {
        this(new StringReader(xml));
    }
    /**
     * <p>Creates an XML with the given node.</p>
     * <p><b>Note:</b> Leading and trailing white spaces are not preserved by 
     * default.
     * To preserve them, add <code>xml:space="preserve"</code> 
     * to your tag, like this:
     * </p>
     * <pre>
     *   &lt;mytag xml:space="preserve"&gt; &lt;/mytag&gt;
     * </pre>
     * <p>The above example will preserve the white space in the tag's body.</p>
     * @param node the node representing the XML
     */
    public XML(Node node) {
        this.node = node;
    }
    
    public Node toNode() {
        return node;
    }
    
    //TODO  make it a toObject(Reader, Object... args) method for non-empty
    // constructors?
    
    
    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object 
     * created will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.
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
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.
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
                obj = (T) Class.forName(clazz).newInstance();
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
            loadFromXML((IXMLConfigurable) obj);
        }
        return obj;
    }
    

    /**
     * <p>Creates a new instance of the class represented by the "class" 
     * attribute
     * on the sub-node of the node argument, matching the key provided.
     * The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object 
     * created will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.</p>
     * 
     * <p>Since 1.6.0, this method should throw a 
     * {@link XMLException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     * 
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     * @throws XMLException if instance cannot be created/populated
     */
    public <T extends Object> T getChildObject(String xpathExpression) {
        return getChildObject(xpathExpression, (T) null, true);
    }
    /**
     * <p>Creates a new instance of the class represented by the "class" 
     * attribute
     * on the sub-node of the node argument, matching the key provided.
     * The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object 
     * created will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.</p>
     * 
     * <p>This method should not throw exception upon errors, but will return
     * the default value instead (even if null). Use a method without
     * a default value argument to get exception on errors.</p>
     * 
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param xpathExpression xpath expression
     * @param <T> the type of the return value
     * @return a new object.
     */
    public <T extends Object> T getChildObject(
            String xpathExpression, T defaultObject) {
        return getChildObject(xpathExpression, defaultObject, false);
    }
    private <T extends Object> T getChildObject(String xpathExpression, 
            T defaultObject, boolean canThrowException) {
        if (node == null) {
            return defaultObject;
        }
        
        try {
            if (xpathExpression == null && defaultObject == null) {
                return toObject((T) null);
            }
            return getChildXML(xpathExpression).toObject(defaultObject);
        } catch (Exception e) {
            handleException(
                    node.getNodeName(), xpathExpression, e, canThrowException);
            return defaultObject;
        }
    }
    
    
    //TODO getElement(s) and getAttribute(s) which are direct references to 
    // elements attributes (not xpath)
    
    public XML getChildXML(String xpathExpression) {
        return new XML(getNode(xpathExpression));
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
    public String toString() {
        return toString(false);
    }
    /**
     * Gets a string representation of this XML.
     * @param indent whether to indent the XML
     * @return XML string
     * @throws XMLException cannot read configuration
     */
    public String toString(boolean indent) {
        try {
            StringWriter w = new StringWriter();
            Result outputTarget = new StreamResult(w);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
            t.transform(new DOMSource(node), outputTarget);
            return w.toString();
        } catch (TransformerFactoryConfigurationError
                | TransformerException e) {
            throw new XMLException(
                    "Could not convert node to reader "
                  + "for node \"" + node.getNodeName() + "\".", e);
        }
    }
    
    /**
     * For classes implementing {@link IXMLConfigurable}, validates XML against 
     * a class XSD schema and logs any error/warnings.
     * The schema expected to be found at the same classpath location and have
     * the same name as the class, but with the ".xsd" extension.
     * @param clazz the class to validate the XML for
     * @return list of errors/warnings or empty (never <code>null</code>)
     */
    public List<XMLValidationError> validate(Class<?> clazz) {
        return doValidate(clazz);
    }
    private List<XMLValidationError> doValidate(Class<?> clazz) {
        
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
        
        try (   InputStream xsdStream = clazz.getResourceAsStream(xsdResource);
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
            throw new XMLException(
                    "Could not validate class: " + clazz, e);
        }
    }
    
    /**
     * Loads XML into the given object, performing validation first.
     * @param obj object to have loaded
     * @return list of errors/warnings or empty (never <code>null</code>)
     */
    public List<XMLValidationError> loadFromXML(IXMLConfigurable obj) {
        if (obj == null || node == null) {
            return new ArrayList<>();
        }
        try {
            List<XMLValidationError> errors = validate(obj.getClass());
            obj.loadFromXML(toReader());
            return errors;
        } catch (IOException e) {
            throw new XMLException(
                    "Could not load new instance from XML \""
                    + obj.getClass() + "\".", e);
        }
    }
    
//    /**
//     * Saves an object as XML on the writer.  This effectively check
//     * if the object implement {@link IXMLConfigurable} and invokes
//     * {@link IXMLConfigurable#saveToXML(Writer)} if so. Otherwise,
//     * it simply write the class name with a "class" attribute on 
//     * the given default tag name.
//     * @param obj the object to write as XML
//     * @param writer where to write the XML
//     * @param defTagName default tag name to use when not implementing 
//     *                   {@link IXMLConfigurable}
//     * @since 2.0.0
//     */
//    public static void saveToXML(Object obj, Writer writer, String defTagName) {
//        if (obj == null) {
//            return;
//        }
//        try {
//            if (obj instanceof IXMLConfigurable) {
//                    ((IXMLConfigurable) obj).saveToXML(writer);
//            } else {
//                writer.write("<");
//                writer.write(defTagName);
//                writer.write(" class=\"");
//                writer.write(obj.getClass().getName());
//                writer.write("\"/>");
//            }
//            writer.flush();
//        } catch (IOException e) {
//            throw new XMLException(
//                    "Could not save object to XML \""
//                    + obj.getClass() + "\".", e);        
//        }
//    }
//    
//    /**
//     * This method is the same as 
//     * {@link HierarchicalConfiguration#configurationAt(String)}, except that
//     * it first checks if the key exists before attempting to retrieve it, 
//     * and returns <code>null</code> on missing keys instead of an 
//     * <code>IllegalArgumentException</code>
//     * @param node the tree to extract a sub tree from
//     * @param key the key that selects the sub tree
//     * @return a XML configuration that contains this sub tree
//     */
//    public static XMLConfiguration getXmlAt(
//            HierarchicalConfiguration<ImmutableNode> node, String key) {
//        if (node == null) {
//            return null;
//        }
//        HierarchicalConfiguration<ImmutableNode> sub = 
//                safeConfigurationAt(node, key);
//        if (sub == null) {
//            return null;
//        }
//        return new XMLConfiguration(sub);
//    }
//    
    /**
     * Convenience class for testing that a {@link IXMLConfigurable} instance
     * can be written, and read into an new instance that is equal as per
     * {@link #equals(Object)}.
     * @param xmlConfigurable the instance to test if it writes/read properly
     * @throws XMLException Cannot save/load configuration
     */
    public static void assertWriteRead(IXMLConfigurable xmlConfigurable) {
        
        // Write
        String xmlStr;
        try (StringWriter out = new StringWriter()) {
            xmlConfigurable.saveToXML(out);
            xmlStr = out.toString();
        } catch (IOException e) {
            throw new XMLException("Could not save XML.", e);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace(xmlStr);
        }
        
        // Read
        XML xml = new XML(xmlStr);
        IXMLConfigurable readConfigurable = xml.toObject();
        if (!xmlConfigurable.equals(readConfigurable)) {
            LOG.error("BEFORE: {}", xmlConfigurable);
            LOG.error(" AFTER: {}", readConfigurable);
            throw new XMLException(
                    "Saved and loaded XML are not the same.");
        }
    }
//
//    /**
//     * Gets a BigDecimal from XML configuration. Same as 
//     * {@link XMLConfiguration#getBigDecimal(String, BigDecimal)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static BigDecimal getNullableBigDecimal(
//            HierarchicalConfiguration<ImmutableNode> xml, 
//            String key, BigDecimal defaultValue) {
//        return keyExists(xml, key) 
//                ? xml.getBigDecimal(key, null) : defaultValue;
//    }
//    /**
//     * Gets a BigInteger from XML configuration. Same as 
//     * {@link XMLConfiguration#getBigInteger(String, BigInteger)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static BigInteger getNullableBigInteger(
//            HierarchicalConfiguration<ImmutableNode> xml, 
//            String key, BigInteger defaultValue) {
//        return keyExists(xml, key)
//                ? xml.getBigInteger(key, null) : defaultValue;
//    }
//    /**
//     * Gets a Boolean from XML configuration. Same as 
//     * {@link XMLConfiguration#getBoolean(String, Boolean)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Boolean getNullableBoolean(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, Boolean defaultValue) {
//        return keyExists(xml, key) ? xml.getBoolean(key, null) : defaultValue;
//    }
//    /**
//     * Gets a Byte from XML configuration. Same as 
//     * {@link XMLConfiguration#getByte(String, Byte)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Byte getNullableByte(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, Byte defaultValue) {
//        return keyExists(xml, key) ? xml.getByte(key, null) : defaultValue;
//    }
//    /**
//     * Gets a Class from XML configuration. 
//     * The default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Class<?> getNullableClass(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, Class<?> defaultValue) {
//        if (!keyExists(xml, key)) {
//            return defaultValue;
//        }
//        String className = xml.getString(key, null);
//        if (StringUtils.isBlank(className)) {
//            return null;
//        }
//        try {
//            return Class.forName(className);
//        } catch (ClassNotFoundException e) {
//            throw new XMLException(
//                    "Could not create Class: " + className, e);
//        }
//    }
//    /**
//     * Gets a Double from XML configuration. Same as 
//     * {@link XMLConfiguration#getDouble(String, Double)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Double getNullableDouble(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, Double defaultValue) {
//        return keyExists(xml, key) ? xml.getDouble(key, null) : defaultValue;
//    }
//    /**
//     * Gets a Float from XML configuration. Same as 
//     * {@link XMLConfiguration#getFloat(String, Float)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    //TODO remove these in favor of versions returning Float? e.g.:
//    //         xml.getFloat("key", (Float) null);
//    public static Float getNullableFloat(
//            HierarchicalConfiguration<ImmutableNode> xml, 
//            String key, Float defaultValue) {
//        return keyExists(xml, key) ? xml.getFloat(key, null) : defaultValue;
//    }
//    /**
//     * Gets an Integer from XML configuration. Same as 
//     * {@link XMLConfiguration#getInteger(String, Integer)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Integer getNullableInteger(
//            HierarchicalConfiguration<ImmutableNode> xml, 
//            String key, Integer defaultValue) {
//        return keyExists(xml, key) ? xml.getInteger(key, null) : defaultValue;
//    }
//    /**
//     * Gets a Long from XML configuration. Same as 
//     * {@link XMLConfiguration#getLong(String, Long)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Long getNullableLong(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, Long defaultValue) {
//        return keyExists(xml, key) ? xml.getLong(key, null) : defaultValue;
//    }
//    /**
//     * Gets a Short from XML configuration. Same as 
//     * {@link XMLConfiguration#getShort(String, Short)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Short getNullableShort(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, Short defaultValue) {
//        return keyExists(xml, key) ? xml.getShort(key, null) : defaultValue;
//    }
//    /**
//     * Gets a String from XML configuration. Same as 
//     * {@link XMLConfiguration#getString(String, String)} except that 
//     * the default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static String getNullableString(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, String defaultValue) {
//        if (keyExists(xml, key)) {
//            return StringUtils.trimToNull(xml.getString(key, null));
//        }
//        return defaultValue;
//    }
//    /**
//     * Gets a Dimension from XML configuration (e.g., 400x500, or 200).  
//     * The default value will only be returned if the key does not exist.
//     * If it exists and it is empty, <code>null</code> will be returned instead. 
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the value
//     * @param defaultValue value to be returned if the key does not exists.
//     * @return the actual value, the default value, or <code>null</code>
//     * @since 1.14.0
//     */
//    public static Dimension getNullableDimension(
//            HierarchicalConfiguration<ImmutableNode> xml, 
//            String key, Dimension defaultValue) {
//        if (keyExists(xml, key)) {
//            String value = xml.getString(key, null);
//            if (StringUtils.isBlank(value)) {
//                return null;
//            }
//            String[] wh = value.split("[xX]"); 
//            if (wh.length == 1) {
//                int val = Integer.parseInt(wh[0].trim());
//                return new Dimension(val, val);
//            }
//            return new Dimension(
//                    Integer.parseInt(wh[0].trim()), 
//                    Integer.parseInt(wh[1].trim()));
//        }
//        return defaultValue;
//    }
//    
    public static boolean exists(Node node, String xpathExpression) {
        try {
            return newXPathExpression(xpathExpression).evaluate(
                    node, XPathConstants.NODE) != null;
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate expression: " + xpathExpression, e) ;
        }
    }
//    
//    /**
//     * Gets a duration which can be a numerical value or a textual 
//     * representation of a duration as per {@link DurationParser}.
//     * If the duration does not exists for the given key or is blank, 
//     * the default value is returned. 
//     * If the key value is found but there are parsing errors, a 
//     * {@link DurationParserException} will be thrown.
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the duration
//     * @param defaultValue default duration
//     * @return duration in milliseconds
//     * @since 1.13.0
//     */
//    public static Duration getDuration(
//            HierarchicalConfiguration<ImmutableNode> xml, 
//            String key, Duration defaultValue) {
//        String duration = xml.getString(key, null);
//        if (StringUtils.isBlank(duration)) {
//            return defaultValue;
//        }
//        return new DurationParser().parse(duration);
//    }
//    
//    /**
//     * Gets a comma-separated-value string as a String array, trimming values 
//     * and removing any blank entries.  
//     * Commas can have any spaces before or after.
//     * Since {@link #toDocument(Reader)} disables delimiter parsing,
//     * this method is an useful alternative to 
//     * {@link HierarchicalConfiguration#getStringArray(String)}.
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the CSV string
//     * @return string array (or null)
//     * @since 1.13.0
//     */
//    public static String[] getCSVStringArray(
//            HierarchicalConfiguration<ImmutableNode> xml, String key) {
//        return getCSVStringArray(xml, key, null);
//    }
//    /**
//     * Gets a comma-separated-value string as a String array, trimming values 
//     * and removing any blank entries.  
//     * Commas can have any spaces before or after.
//     * Since {@link #toDocument(Reader)} disables delimiter parsing,
//     * this method is an useful alternative to 
//     * {@link HierarchicalConfiguration#getStringArray(String)}.
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the CSV string
//     * @param defaultValues default values if the split returns null
//     *        or an empty array
//     * @return string array (or null)
//     * @since 1.13.0
//     */
//    public static String[] getCSVStringArray(
//            HierarchicalConfiguration<ImmutableNode> xml, 
//            String key, String[] defaultValues) {
//        String[] values = splitCSV(xml.getString(key, null));
//        if (ArrayUtils.isEmpty(values)) {
//            return defaultValues;
//        }
//        return values;
//    }
//
//    /**
//     * Gets a comma-separated-value string as an int array, removing any 
//     * blank entries.  
//     * Commas can have any spaces before or after.
//     * Invalid integers will log an error and assign zero instead.
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the CSV string
//     * @return int array (or null)
//     * @since 1.13.0
//     */
//    public static int[] getCSVIntArray(
//            HierarchicalConfiguration<ImmutableNode> xml, String key) {
//        return getCSVIntArray(xml, key, null);
//    }
//    /**
//     * Gets a comma-separated-value string as an int array, removing any 
//     * blank entries.  
//     * Commas can have any spaces before or after.
//     * Invalid integers will log an error and assign zero instead.
//     * @param xml xml configuration
//     * @param key key to the element/attribute containing the CSV string
//     * @param defaultValues default values if the split returns null
//     *        or an empty array
//     * @return int array (or null)
//     * @since 1.13.0
//     */
//    public static int[] getCSVIntArray(
//            HierarchicalConfiguration<ImmutableNode> xml,
//            String key, int[] defaultValues) {
//        String[] strings = splitCSV(xml.getString(key, null));
//        if (ArrayUtils.isEmpty(strings)) {
//            return defaultValues;
//        }
//        int[] ints = new int[strings.length];
//        for (int i = 0; i < strings.length; i++) {
//            try {
//                ints[i] = Integer.parseInt(strings[i]);
//            } catch (NumberFormatException e) {
//                LOG.error("Invalid integer: " + strings[i], e);
//            }
//        }
//        return ints;
//    }
//    
//    // CVS Split: trim + remove blank entries 
//    private static String[] splitCSV(String str) {
//        if (str == null) {
//            return null;
//        }
//        if (StringUtils.isBlank(str)) {
//            return ArrayUtils.EMPTY_STRING_ARRAY;
//        }
//        return str.trim().split("(\\s*,\\s*)+");
//    }
//    
    private static String getXSDResourcePath(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return "/" + clazz.getCanonicalName().replace('.', '/') + ".xsd";
    }
    
    
    
    
    
//    // This method is because the regular configurationAt MUST have 1
//    // entry or will fail, and the containsKey(String) method is not reliable
//    // since it expects a value (body text) or returns false.
//    private static HierarchicalConfiguration<ImmutableNode> safeConfigurationAt(
//            HierarchicalConfiguration<ImmutableNode> node, String key) {
//        List<HierarchicalConfiguration<ImmutableNode>> subs = 
//                node.configurationsAt(key);
//        if (subs != null && !subs.isEmpty()) {
//            return subs.get(0);
//        }
//        return null;
//    }
//    
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
    
//  /**
//  * This load method will return an Apache XML Configuration from
//  * a {@link HierarchicalConfiguration}. 
//  * @param c hierarchical configuration
//  * @return XMLConfiguration
//  * @since 1.5.0
//  */
// //TODO still required given it just returns now???
// public static XMLConfiguration toDocument(
//         HierarchicalConfiguration<ImmutableNode> c) {
//     return new XMLConfiguration(c);
// }
    
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
        return getNodeList(newXPathExpression(xpathExpression));
    }
    public NodeArrayList getNodeList(XPathExpression expression) {
        try {
            return new NodeArrayList((NodeList) expression.evaluate(
                    node, XPathConstants.NODESET));
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate XPath expression.", e);
        }
    }
    public Node getNode(String xpathExpression) {
        return getNode(newXPathExpression(xpathExpression));
    }
    public Node getNode(XPathExpression expression) {
        try {
            return (Node) expression.evaluate(node, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate XPath expression.", e);
        }
    }
    public String getString(String xpathExpression) {
        return getString(xpathExpression, null);
    }
    public String getString(XPathExpression expression) {
        return getString(expression, null);
    }
    public String getString(
            String xpathExpression, String defaultValue) {
        return getString(
                newXPathExpression(xpathExpression), defaultValue);
    }
    public String getString(
            XPathExpression expression, String defaultValue) {
        try {
            Node n = (Node) expression.evaluate(node, XPathConstants.NODE);
            if (n == null) {
                return defaultValue;
            }
            if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
                return n.getNodeValue();
            }
            return n.getTextContent();
        } catch (XPathExpressionException e) {
            throw new XMLException(
                    "Could not evaluate XPath expression.", e);
        }
    }
    public Integer getInteger(String xpathExpression) {
        return getInteger(xpathExpression, null);
    }
    public Integer getInteger(String xpathExpression, Integer defaultValue) {
        String val = getString(xpathExpression, null);
        return val == null ? defaultValue : Integer.parseInt(val);
    }
    public Long getLong(String xpathExpression) {
        return getLong(xpathExpression, null);
    }
    public Long getLong(String xpathExpression, Long defaultValue) {
        String val = getString(xpathExpression, null);
        return val == null ? defaultValue : Long.parseLong(val);
    }

}
