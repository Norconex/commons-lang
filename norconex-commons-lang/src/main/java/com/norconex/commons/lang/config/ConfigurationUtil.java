/* Copyright 2010-2016 Norconex Inc.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.file.FileUtil;

//TODO move most methods to XMLConfigurationUtil under the .xml package,
//along with moving IXMLConfigurable there?

/**
 * Utility methods when dealing with configuration files.
 * @author Pascal Essiembre
 */
public final class ConfigurationUtil {
    private static final Logger LOG = 
            LogManager.getLogger(ConfigurationUtil.class);

    private ConfigurationUtil() {
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
        return newInstance(node, null, true);
    }

    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable} and 
     * <code>supportXMLConfigurable</code> is true, the object created
     * will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.
     * @param node the node representing the class to instantiate.
     * @param supportXMLConfigurable automatically populates the object from XML
     *        if it is implementing {@link IXMLConfigurable}.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node,
            boolean supportXMLConfigurable) {
        return newInstance(node, null, supportXMLConfigurable);
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
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, T defaultObject) {
        return newInstance(node, defaultObject, true);
    }
    
    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable} and 
     * <code>supportXMLConfigurable</code> is true, the object created
     * will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.
     * @param node the node representing the class to instantiate.
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @param supportXMLConfigurable automatically populates the object from XML
     *        if it is implementing {@link IXMLConfigurable}.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    @SuppressWarnings("unchecked")
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, T defaultObject,
            boolean supportXMLConfigurable) {
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
        if (obj instanceof IXMLConfigurable && supportXMLConfigurable) {
            try {
                ((IXMLConfigurable) obj).loadFromXML(newReader(node));
            } catch (IOException e) {
                throw new ConfigurationException(
                        "Could not load new instance from XML \""
                        + clazz + "\".", e);
            }
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
        return newInstance(node, key, null, true, true);
    }
    /**
     * <p>Creates a new instance of the class represented by the "class" 
     * attribute on the sub-node of the node argument, matching the key 
     * provided.
     * The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable} and 
     * <code>supportXMLConfigurable</code> is true, the object created
     * will be automatically populated by invoking the 
     * {@link IXMLConfigurable#loadFromXML(Reader)} method, 
     * passing it the node XML automatically populated.</p>
     * 
     * <p>Since 1.6.0, this method should throw a  
     * {@link ConfigurationException} upon error. Use a method
     * with a default value argument to avoid throwing exceptions.</p>
     * 
     * @param node the node representing the class to instantiate.
     * @param key sub-node name/hierarchical path
     * @param supportXMLConfigurable automatically populates the object from XML
     *        if it is implementing {@link IXMLConfigurable}.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key, 
            boolean supportXMLConfigurable) {
        return newInstance(node, key, null, supportXMLConfigurable, true);
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
            HierarchicalConfiguration node, String key, 
            T defaultObject) {
        return newInstance(node, key, defaultObject, true, false);
    }
    /**
     * <p>Creates a new instance of the class represented by the "class" 
     * attribute
     * on the sub-node of the node argument, matching the key provided.
     * The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable} and 
     * <code>supportXMLConfigurable</code> is true, the object created
     * will be automatically populated by invoking the 
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
     * @param supportXMLConfigurable automatically populates the object from XML
     *        if it is implementing {@link IXMLConfigurable}.
     * @param <T> the type of the return value
     * @return a new object.
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key, 
            T defaultObject, boolean supportXMLConfigurable) {
        return newInstance(node, key, defaultObject, 
                supportXMLConfigurable, false);
    }
    private static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key, 
            T defaultObject, boolean supportXMLConfigurable,
            boolean canThrowException) {
        if (node == null) {
            return defaultObject;
        }
        
        try {
            if (key == null && defaultObject == null) {
                return ConfigurationUtil.newInstance(
                        node, (T) null, supportXMLConfigurable);
            }
            HierarchicalConfiguration subconfig = 
                    safeConfigurationAt(node, key);
            return ConfigurationUtil.newInstance(
                    subconfig, defaultObject, supportXMLConfigurable);
        } catch (Exception e) {
            if (canThrowException) {
                if (e instanceof ConfigurationException) {
                    throw (ConfigurationException) e;
                } else {
                    throw new ConfigurationException(
                            "Could not instantiate object from configuration "
                          + "for \"" + node.getRoot().getName() 
                          + " -> " + key + "\".", e);
                }
            } else {
                if (e instanceof ConfigurationException
                        && e.getCause() != null
                        && e.getCause() instanceof ClassNotFoundException) {
                    LOG.error("You declared a class that does not exists "
                            + "for \"" + node.getRoot().getName() 
                            + " -> " + key + "\". "
                            + "Check for typos in your XML and make sure that "
                            + "class is part of your Java classpath.", e);
                } else{ 
                    LOG.debug("Could not instantiate object from configuration "
                            + "for \"" + node.getRoot().getName() 
                            + " -> " + key + "\".", e);
                }
            }
            return defaultObject;
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
        
        File tempFile = File.createTempFile("XMLConfigurableTester", ".xml");
        
        // Write
        Writer out = new OutputStreamWriter(
                new FileOutputStream(tempFile), CharEncoding.UTF_8);
        try {
            xmlConfiurable.saveToXML(out);
        } finally {
            out.close();
        }
        
        // Read
        XMLConfiguration xml = new ConfigurationLoader().loadXML(tempFile);
        IXMLConfigurable readConfigurable = 
                (IXMLConfigurable) ConfigurationUtil.newInstance(xml);

        FileUtil.delete(tempFile);

        if (!xmlConfiurable.equals(readConfigurable)) {
            LOG.error("BEFORE: " + xmlConfiurable);
            LOG.error(" AFTER: " + readConfigurable);
            throw new ConfigurationException(
                    "Saved and loaded XML are not the same.");
        }
    }

    /**
     * Gets a comma-separated-value string as a String array, trimming values 
     * and removing any blank entries.  
     * Commas can have any spaces before or after.
     * Since {@link #newXMLConfiguration(Reader)} disables delimiter parsing,
     * this method is an useful alternative to 
     * {@link HierarchicalConfiguration#getStringArray(String)}.
     * @param xml xml configuration
     * @param key key to the element/attribute containing the string
     * @return string array (or null)
     * @since 1.13.0
     */
    public static String[] getCSVArray(
            HierarchicalConfiguration xml, String key) {
        return getCSVArray(xml, key, null);
    }
    /**
     * Gets a comma-separated-value string as a String array, trimming values 
     * and removing any blank entries.  
     * Commas can have any spaces before or after.
     * Since {@link #newXMLConfiguration(Reader)} disables delimiter parsing,
     * this method is an useful alternative to 
     * {@link HierarchicalConfiguration#getStringArray(String)}.
     * @param xml xml configuration
     * @param key key to the element/attribute containing the string
     * @param defaultValues default values if the split returns null
     *        or an empty array
     * @return string array (or null)
     * @since 1.13.0
     */
    public static String[] getCSVArray(
            HierarchicalConfiguration xml, String key, String[] defaultValues) {
        return splitCSV(xml.getString(key, null), defaultValues);
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
    
    private static String[] splitCSV(String str, String[] defaultValues) {
        if (str == null) {
            return null;
        }
        String[] values = str.trim().split("(\\s*,\\s*)+");
        if (ArrayUtils.isEmpty(values) 
                && ArrayUtils.isNotEmpty(defaultValues)) {
            return defaultValues;
        }
        return values;
    }
    
}
