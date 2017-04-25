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

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Utility methods when dealing with configuration files.
 * @author Pascal Essiembre
 * @deprecated Since 1.13.0, use {@link XMLConfigurationUtil}
 */
@Deprecated
public final class ConfigurationUtil {

    private ConfigurationUtil() {
        super();
    }
   

    /**
     * Disables delimiter parsing for both attributes and elements.
     * @param xml XML configuration
     */
    public static void disableDelimiterParsing(XMLConfiguration xml) {
        XMLConfigurationUtil.disableDelimiterParsing(xml);
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
        return XMLConfigurationUtil.newXMLConfiguration(c);
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
        return XMLConfigurationUtil.newXMLConfiguration(in);
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
        return XMLConfigurationUtil.newInstance(node);
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
     *        Since 1.13.0, this flag is always considered <code>true</code>.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node,
            boolean supportXMLConfigurable) {
        return XMLConfigurationUtil.newInstance(node);
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
        return XMLConfigurationUtil.newInstance(node, defaultObject);
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
     *        Since 1.13.0, this flag is always considered <code>true</code>.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, T defaultObject,
            boolean supportXMLConfigurable) {
        return XMLConfigurationUtil.newInstance(node, defaultObject);
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
        return XMLConfigurationUtil.newInstance(node, key);
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
     *        Since 1.13.0, this flag is always considered <code>true</code>.
     * @param <T> the type of the return value
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key, 
            boolean supportXMLConfigurable) {
        return XMLConfigurationUtil.newInstance(node, key);
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
        return XMLConfigurationUtil.newInstance(node, key, defaultObject);
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
     *        Since 1.13.0, this flag is always considered <code>true</code>.
     * @param <T> the type of the return value
     * @return a new object.
     */
    public static <T extends Object> T newInstance(
            HierarchicalConfiguration node, String key, 
            T defaultObject, boolean supportXMLConfigurable) {
        return XMLConfigurationUtil.newInstance(node, key, defaultObject);
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
        return XMLConfigurationUtil.newReader(node);
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
        return XMLConfigurationUtil.getXmlAt(node, key);
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
        XMLConfigurationUtil.assertWriteRead(xmlConfiurable);
    }
}
