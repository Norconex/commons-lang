/* Copyright 2010-2017 Norconex Inc.
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
package com.norconex.commons.lang.config;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.xerces.xni.NamespaceContext;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import com.norconex.commons.lang.time.DurationParser;
import com.norconex.commons.lang.time.DurationParserException;
import com.norconex.commons.lang.xml.ClasspathResourceResolver;

//TODO Add some of the convenience methods found in Collector/Crawler Loader?

//TODO consider checking for a "disable=false|true" and setting it on 
// a method if this method exists, and/or do not load if set to true.

/**
 * Utility methods when dealing with XML configuration files.
 * @author Pascal Essiembre
 * @since 1.13.0
 */
public final class XMLConfigurationUtil {
    private static final Logger LOG = 
            LogManager.getLogger(XMLConfigurationUtil.class);

    public static final String W3C_XML_SCHEMA_NS_URI_1_1 = 
            "http://www.w3.org/XML/XMLSchema/v1.1";
    
    private XMLConfigurationUtil() {
        super();
    }
   

    /**
     * Disables delimiter parsing for both attributes and elements.
     * @param xml XML configuration
     */
    public static void disableDelimiterParsing(XMLConfiguration xml) {
        xml.setListDelimiter('\0');
        xml.setDelimiterParsingDisabled(true);
        xml.setAttributeSplittingDisabled(true);
    }
    
    
    /**
     * This load method will return an Apache XML Configuration from
     * from a {@link HierarchicalConfiguration}, with delimiter parsing 
     * disabled. 
     * @param c hierarchical configuration
     * @return XMLConfiguration
     * @since 1.5.0
     */
    public static XMLConfiguration newXMLConfiguration(
            HierarchicalConfiguration c) {
        XMLConfiguration xml = new XMLConfiguration(c);
        disableDelimiterParsing(xml);
        return xml;
    }
    /**
     * <p>This load method will return an Apache XML Configuration from
     * from a reader, with delimiter parsing disabled.</p>
     * <p><b>Note:</b> Leading and trailing white spaces are not preserved by 
     * default.
     * To preserve them, add <code>xml:space="preserve"</code> 
     * to your tag, like this:
     * </p>
     * <pre>
     *   &lt;mytag xml:space="preserve"&gt; &lt;/mytag&gt;
     * </pre>
     * <p>The above example will preserve the white space in the tag's body.
     * @param in input stream
     * @return XMLConfiguration
     * @since 1.5.0
     */
    public static XMLConfiguration newXMLConfiguration(Reader in) {
        XMLConfiguration xml = new XMLConfiguration();
        disableDelimiterParsing(xml);
        try {
            xml.load(in);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Cannot load XMLConfiguration", e);
        }
        return xml;
    }

    /**
     * <p>Creates a new instance of the class represented by the "class" 
     * attribute on the supplied XML. 
     *  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object 
     * created will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.
     * The reader is expected to hold an XML, which will
     * first be converted to an {@link XMLConfiguration}. This method has the 
     * same effect as invoking: 
     * </p>
     * <pre>
     * XMLConfigurationUtil.newInstance(
     *         XMLConfigurationUtil.newXMLConfiguration(reader));
     * </pre>
     * 
     * @param reader the XML representing the class to instantiate.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(Reader reader) {
        return XMLConfigurationUtil.newInstance(
                XMLConfigurationUtil.newXMLConfiguration(reader));
    }
    
    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object 
     * created will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.
     * @param node the node representing the class to instantiate.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node) {
        return newInstance(node, (String) null);
    }
    
    
    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, the object 
     * created will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.
     * @param node the node representing the class to instantiate.
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    @SuppressWarnings("unchecked")
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, T defaultObject) {
        T obj;
        String clazz;
        if (node == null) {
            return defaultObject;
        }
        clazz = node.getString("[@class]", null);
        if (clazz != null) {
            try {
                obj = (T) Class.forName(clazz).newInstance();
            } catch (Exception e) {
                throw new ConfigurationException(
                        "This class could not be instantiated: \""
                        + clazz + "\".", e);
            }
        } else {
            LOG.debug("A configuration entry was found without class "
                   + "reference where one could have been provided; "
                   + "using default value:" + defaultObject);
            obj = defaultObject;
        }
        if (obj == null) {
            return defaultObject;
        }
        if (obj instanceof IXMLConfigurable) {
            loadFromXML((IXMLConfigurable) obj, node);
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
     * {@link ConfigurationException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     * 
     * @param node the node representing the class to instantiate.
     * @param key sub-node name/hierarchical path
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key) {
        return newInstance(node, key, (T) null, true);
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
     * @param node the node representing the class to instantiate.
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param key sub-node name/hierarchical path
     * @param <T> the type of the return value
     * @return a new object.
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key, T defaultObject) {
        return newInstance(node, key, defaultObject, false);
    }
    private static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key, 
            T defaultObject, boolean canThrowException) {
        if (node == null) {
            return defaultObject;
        }
        
        try {
            if (key == null && defaultObject == null) {
                return newInstance(node, (T) null);
            }
            HierarchicalConfiguration subconfig = 
                    safeConfigurationAt(node, key);
            return newInstance(subconfig, defaultObject);
        } catch (Exception e) {
            handleException(node.getRootNode(), key, e, canThrowException);
            return defaultObject;
        }
    }
    private static void handleException(
            ConfigurationNode rootNode, String key,
            Exception e, boolean canThrowException) {
        
        // Throw exception
        if (canThrowException) {
            if (e instanceof ConfigurationException) {
                throw (ConfigurationException) e;
            } else {
                throw new ConfigurationException(
                        "Could not instantiate object from configuration "
                      + "for \"" + rootNode.getName() 
                      + " -> " + key + "\".", e);
            }
        }

        // Log exception
        if (e instanceof ConfigurationException
                && e.getCause() != null) {
            if (e.getCause() instanceof ClassNotFoundException) {
                LOG.error("You declared a class that does not exists "
                        + "for \"" + rootNode.getName() 
                        + " -> " + key + "\". Check for typos in your "
                        + "XML and make sure that "
                        + "class is part of your Java classpath.", e);
            } else if (e.getCause() instanceof SAXParseException) {
                    String systemId = ((SAXParseException ) 
                            e.getCause()).getSystemId();
                    if (StringUtils.endsWith(systemId, ".xsd")) {
                        LOG.error("XML Schema parsing error for \""
                                + rootNode.getName() 
                                + " -> " + key + "\". Schema: " + systemId, e);
                    } else {
                        LOG.error("XML parsing error for \""
                                + rootNode.getName() 
                                + " -> " + key + "\".", e);
                    }
            }
        } else{ 
            LOG.debug("Could not instantiate object from configuration "
                    + "for \"" + rootNode.getName() 
                    + " -> " + key + "\".", e);
        }
    }
    
    /**
     * Creates a new {@link Reader} from a {@link XMLConfiguration}.
     * Do not forget to close the reader instance when you are done with it.
     * @param node the xml configuration to convert to a reader instance.
     * @return reader
     * @throws ConfigurationException cannot read configuration
     * @throws IOException cannot read configuration
     */
    public static Reader newReader(HierarchicalConfiguration node)
            throws IOException {
        XMLConfiguration xml;
        if (node instanceof XMLConfiguration) {
            xml = (XMLConfiguration) node;
        } else {
            xml = new XMLConfiguration(node);
            disableDelimiterParsing(xml);
        }
        StringWriter w = new StringWriter();
        try {
            xml.save(w);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException(
                    "Could transform XML node to reader.", e);
        }
        StringReader r = new StringReader(w.toString());
        w.close();
        return r;
    }
    
    
    
    /**
     * For classes implementing {@link IXMLConfigurable}, validates XML against 
     * a class XSD schema and logs any error/warnings.
     * The schema expected to be found at the same classpath location and have
     * the same name as the class, but with the ".xsd" extension.
     * @param clazz the class to validate the XML for
     * @param node the XML to validate
     * @return the number of errors/warnings
     */
    public static int validate(Class<?> clazz, HierarchicalConfiguration node) {
        return doValidate(clazz, node);
    }
    /**
     * For classes implementing {@link IXMLConfigurable}, validates XML against 
     * a class XSD schema and logs any error/warnings.
     * The schema expected to be found at the same classpath location and have
     * the same name as the class, but with the ".xsd" extension.
     * @param clazz the class to validate the XML for
     * @param xml the XML to validate
     * @return the number of errors/warnings
     */
    public static int validate(Class<?> clazz, Reader xml) {
        return doValidate(clazz, xml);
    }
    private static int doValidate(Class<?> clazz, Object source) {
        // Only validate if IXMLConfigurable
        if (clazz == null || !IXMLConfigurable.class.isAssignableFrom(clazz)) {
            return 0;
        }
        
        // Only validate if .xsd file exist in classpath for class
        String xsdResource = ClassUtils.getSimpleName(clazz) + ".xsd";
        LOG.debug("Class to validate: " + ClassUtils.getSimpleName(clazz));
        if (clazz.getResource(xsdResource) == null) {
            LOG.debug("Resource not found for validation: " + xsdResource);
            return 0;
        }

        // Go ahead: validate
        SchemaFactory schemaFactory = 
                SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI_1_1);
        schemaFactory.setResourceResolver(new ClasspathResourceResolver(clazz));
        
        Reader reader = null;
        try (InputStream xsdStream = clazz.getResourceAsStream(xsdResource)) {
            if (source instanceof Reader) {
                reader = (Reader) source;
            } else {
                reader = newReader((HierarchicalConfiguration) source);
            }
            Schema schema = schemaFactory.newSchema(
                    new StreamSource(xsdStream, getXSDResourcePath(clazz)));
            Validator validator = schema.newValidator();
            LogErrorHandler seh = new LogErrorHandler(clazz);
            validator.setErrorHandler(seh);
            SAXSource saxSource = new SAXSource(new W3XMLNamespaceFilter(
                    XMLReaderFactory.createXMLReader()), 
                            new InputSource(reader));
            validator.validate(saxSource);
            return seh.errorCount;
        } catch (SAXException | IOException e) {
            throw new ConfigurationException(
                    "Could not validate class: " + clazz, e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
    
    /**
     * Loads XML into the given object, performing validation first.
     * Except for validation, it is the same as calling 
     * {@link IXMLConfigurable#loadFromXML(Reader)} on an object.
     * @param obj object to have loaded
     * @param reader xml reader 
     */
    public static void loadFromXML(IXMLConfigurable obj, Reader reader) {
        if (obj == null || reader == null) {
            return;
        }
        loadFromXML(obj, newXMLConfiguration(reader));
    }
    /**
     * Loads XML into the given object, performing validation first.
     * @param obj object to have loaded
     * @param node XML node to have loaded
     */
    public static void loadFromXML(
            IXMLConfigurable obj, HierarchicalConfiguration node) {
        if (obj == null || node == null) {
            return;
        }
        try {
            validate(obj.getClass(), node);
            obj.loadFromXML(newReader(node));
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Could not load new instance from XML \""
                    + obj.getClass() + "\".", e);
        }
    }
    
    /**
     * This method is the same as 
     * {@link HierarchicalConfiguration#configurationAt(String)}, except that
     * it first checks if the key exists before attempting to retrieve it, 
     * and returns <code>null</code> on missing keys instead of an 
     * <code>IllegalArgumentException</code>
     * @param node the tree to extract a sub tree from
     * @param key the key that selects the sub tree
     * @return a XML configuration that contains this sub tree
     */
    public static XMLConfiguration getXmlAt(
            HierarchicalConfiguration node, String key) {
        if (node == null) {
            return null;
        }
        HierarchicalConfiguration sub = safeConfigurationAt(node, key);
        if (sub == null) {
            return null;
        }
        XMLConfiguration xml = new XMLConfiguration(sub);
        disableDelimiterParsing(xml);
        return xml;
    }
    
    /**
     * Convenience class for testing that a {@link IXMLConfigurable} instance
     * can be written, and read into an new instance that is equal as per
     * {@link #equals(Object)}.
     * @param xmlConfiurable the instance to test if it writes/read properly
     * @throws IOException Cannot read/write
     * @throws ConfigurationException Cannot load configuration
     */
    public static void assertWriteRead(IXMLConfigurable xmlConfiurable)
            throws IOException {
        
        // Write
        StringWriter out = new StringWriter();
        try {
            xmlConfiurable.saveToXML(out);
        } finally {
            out.close();
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(out.toString());
        }
        
        // Read
        XMLConfiguration xml = newXMLConfiguration(
                new StringReader(out.toString()));
        IXMLConfigurable readConfigurable = newInstance(xml);

        if (!xmlConfiurable.equals(readConfigurable)) {
            LOG.error("BEFORE: " + xmlConfiurable);
            LOG.error(" AFTER: " + readConfigurable);
            throw new ConfigurationException(
                    "Saved and loaded XML are not the same.");
        }
    }

    /**
     * Gets a BigDecimal from XML configuration. Same as 
     * {@link XMLConfiguration#getBigDecimal(String, BigDecimal)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static BigDecimal getNullableBigDecimal(
            HierarchicalConfiguration xml, 
            String key, BigDecimal defaultValue) {
        return keyExists(xml, key) 
                ? xml.getBigDecimal(key, null) : defaultValue;
    }
    /**
     * Gets a BigInteger from XML configuration. Same as 
     * {@link XMLConfiguration#getBigInteger(String, BigInteger)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static BigInteger getNullableBigInteger(
            HierarchicalConfiguration xml, 
            String key, BigInteger defaultValue) {
        return keyExists(xml, key)
                ? xml.getBigInteger(key, null) : defaultValue;
    }
    /**
     * Gets a Boolean from XML configuration. Same as 
     * {@link XMLConfiguration#getBoolean(String, Boolean)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Boolean getNullableBoolean(
            HierarchicalConfiguration xml, String key, Boolean defaultValue) {
        return keyExists(xml, key) ? xml.getBoolean(key, null) : defaultValue;
    }
    /**
     * Gets a Byte from XML configuration. Same as 
     * {@link XMLConfiguration#getByte(String, Byte)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Byte getNullableByte(
            HierarchicalConfiguration xml, String key, Byte defaultValue) {
        return keyExists(xml, key) ? xml.getByte(key, null) : defaultValue;
    }
    /**
     * Gets a Class from XML configuration. 
     * The default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Class<?> getNullableClass(
            HierarchicalConfiguration xml, String key, Class<?> defaultValue) {
        if (!keyExists(xml, key)) {
            return defaultValue;
        }
        String className = xml.getString(key, null);
        if (StringUtils.isBlank(className)) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Could not create Class: " + className, e);
        }
    }
    /**
     * Gets a Double from XML configuration. Same as 
     * {@link XMLConfiguration#getDouble(String, Double)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Double getNullableDouble(
            HierarchicalConfiguration xml, String key, Double defaultValue) {
        return keyExists(xml, key) ? xml.getDouble(key, null) : defaultValue;
    }
    /**
     * Gets a Float from XML configuration. Same as 
     * {@link XMLConfiguration#getFloat(String, Float)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Float getNullableFloat(
            HierarchicalConfiguration xml, String key, Float defaultValue) {
        return keyExists(xml, key) ? xml.getFloat(key, null) : defaultValue;
    }
    /**
     * Gets an Integer from XML configuration. Same as 
     * {@link XMLConfiguration#getInteger(String, Integer)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Integer getNullableInteger(
            HierarchicalConfiguration xml, String key, Integer defaultValue) {
        return keyExists(xml, key) ? xml.getInteger(key, null) : defaultValue;
    }
    /**
     * Gets a Long from XML configuration. Same as 
     * {@link XMLConfiguration#getLong(String, Long)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Long getNullableLong(
            HierarchicalConfiguration xml, String key, Long defaultValue) {
        return keyExists(xml, key) ? xml.getLong(key, null) : defaultValue;
    }
    /**
     * Gets a Short from XML configuration. Same as 
     * {@link XMLConfiguration#getShort(String, Short)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Short getNullableShort(
            HierarchicalConfiguration xml, String key, Short defaultValue) {
        return keyExists(xml, key) ? xml.getShort(key, null) : defaultValue;
    }
    /**
     * Gets a String from XML configuration. Same as 
     * {@link XMLConfiguration#getString(String, String)} except that 
     * the default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static String getNullableString(
            HierarchicalConfiguration xml, String key, String defaultValue) {
        if (keyExists(xml, key)) {
            return StringUtils.trimToNull(xml.getString(key, null));
        }
        return defaultValue;
    }
    /**
     * Gets a Dimension from XML configuration (e.g., 400x500, or 200).  
     * The default value will only be returned if the key does not exist.
     * If it exists and it is empty, <code>null</code> will be returned instead. 
     * @param xml xml configuration
     * @param key key to the element/attribute containing the value
     * @param defaultValue value to be returned if the key does not exists.
     * @return the actual value, the default value, or <code>null</code>
     * @since 1.14.0
     */
    public static Dimension getNullableDimension(
            HierarchicalConfiguration xml, String key, Dimension defaultValue) {
        if (keyExists(xml, key)) {
            String value = xml.getString(key, null);
            if (StringUtils.isBlank(value)) {
                return null;
            }
            String[] wh = value.split("[xX]"); 
            if (wh.length == 1) {
                int val = Integer.parseInt(wh[0].trim());
                return new Dimension(val, val);
            }
            return new Dimension(
                    Integer.parseInt(wh[0].trim()), 
                    Integer.parseInt(wh[1].trim()));
        }
        return defaultValue;
    }
    
    public static boolean keyExists(HierarchicalConfiguration xml, String key) {
        Iterator<String> it = xml.getKeys();
        while (it.hasNext()) {
            String itKey = it.next();
            if (!key.contains("[@")) {
                itKey = itKey.replaceAll("\\[@.*?\\]", "");
            }
            if (Objects.equals(itKey, key)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets a duration which can be a numerical value or a textual 
     * representation of a duration as per {@link DurationParser}.
     * If the duration does not exists for the given key or is blank, 
     * the default value is returned. 
     * If the key value is found but there are parsing errors, a 
     * {@link DurationParserException} will be thrown.
     * @param xml xml configuration
     * @param key key to the element/attribute containing the duration
     * @param defaultValue default duration
     * @return duration in milliseconds
     * @since 1.13.0
     */
    public static long getDuration(
            HierarchicalConfiguration xml, String key, long defaultValue) {
        String duration = xml.getString(key, null);
        if (StringUtils.isBlank(duration)) {
            return defaultValue;
        }
        return DurationParser.parse(duration);
    }
    
    /**
     * Gets a comma-separated-value string as a String array, trimming values 
     * and removing any blank entries.  
     * Commas can have any spaces before or after.
     * Since {@link #newXMLConfiguration(Reader)} disables delimiter parsing,
     * this method is an useful alternative to 
     * {@link HierarchicalConfiguration#getStringArray(String)}.
     * @param xml xml configuration
     * @param key key to the element/attribute containing the CSV string
     * @return string array (or null)
     * @since 1.13.0
     */
    public static String[] getCSVStringArray(
            HierarchicalConfiguration xml, String key) {
        return getCSVStringArray(xml, key, null);
    }
    /**
     * Gets a comma-separated-value string as a String array, trimming values 
     * and removing any blank entries.  
     * Commas can have any spaces before or after.
     * Since {@link #newXMLConfiguration(Reader)} disables delimiter parsing,
     * this method is an useful alternative to 
     * {@link HierarchicalConfiguration#getStringArray(String)}.
     * @param xml xml configuration
     * @param key key to the element/attribute containing the CSV string
     * @param defaultValues default values if the split returns null
     *        or an empty array
     * @return string array (or null)
     * @since 1.13.0
     */
    public static String[] getCSVStringArray(
            HierarchicalConfiguration xml, String key, String[] defaultValues) {
        String[] values = splitCSV(xml.getString(key, null));
        if (ArrayUtils.isEmpty(values)) {
            return defaultValues;
        }
        return values;
    }

    /**
     * Gets a comma-separated-value string as an int array, removing any 
     * blank entries.  
     * Commas can have any spaces before or after.
     * Invalid integers will log an error and assign zero instead.
     * @param xml xml configuration
     * @param key key to the element/attribute containing the CSV string
     * @return int array (or null)
     * @since 1.13.0
     */
    public static int[] getCSVIntArray(
            HierarchicalConfiguration xml, String key) {
        return getCSVIntArray(xml, key, null);
    }
    /**
     * Gets a comma-separated-value string as an int array, removing any 
     * blank entries.  
     * Commas can have any spaces before or after.
     * Invalid integers will log an error and assign zero instead.
     * @param xml xml configuration
     * @param key key to the element/attribute containing the CSV string
     * @param defaultValues default values if the split returns null
     *        or an empty array
     * @return int array (or null)
     * @since 1.13.0
     */
    public static int[] getCSVIntArray(
            HierarchicalConfiguration xml, String key, int[] defaultValues) {
        String[] strings = splitCSV(xml.getString(key, null));
        if (ArrayUtils.isEmpty(strings)) {
            return defaultValues;
        }
        int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            try {
                ints[i] = Integer.parseInt(strings[i]);
            } catch (NumberFormatException e) {
                LOG.error("Invalid integer: " + strings[i], e);
            }
        }
        return ints;
    }
    
    // CVS Split: trim + remove blank entries 
    private static String[] splitCSV(String str) {
        if (str == null) {
            return null;
        }
        if (StringUtils.isBlank(str)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return str.trim().split("(\\s*,\\s*)+");
    }
    
    private static String getXSDResourcePath(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return "/" + clazz.getCanonicalName().replace('.', '/') + ".xsd";
    }
    
    // This method is because the regular configurationAt MUST have 1
    // entry or will fail, and the containsKey(String) method is not reliable
    // since it expects a value (body text) or returns false.
    private static HierarchicalConfiguration safeConfigurationAt(
            HierarchicalConfiguration node, String key) {
        List<HierarchicalConfiguration> subs = node.configurationsAt(key);
        if (subs != null && !subs.isEmpty()) {
            return subs.get(0);
        }
        return null;
    }
    
    private static class LogErrorHandler implements ErrorHandler {
        private int errorCount = 0; 
        private final Class<?> clazz;
        public LogErrorHandler(Class<?> clazz) {
            super();
            this.clazz = clazz;
        }
        @Override
        public void warning(SAXParseException e) throws SAXException {
            errorCount++;
            LOG.warn(msg(e));
        }
        @Override
        public void error(SAXParseException e) throws SAXException {
            errorCount++;
            LOG.error(msg(e));
        }
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            errorCount++;
            LOG.fatal(msg(e));
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
}
