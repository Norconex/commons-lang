package com.norconex.commons.lang.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Assert;

/**
 * Utility methods when dealing with configuration files.
 * @author Pascal Essiembre
 */
public final class ConfigurationUtils {
    private static final Logger LOG = 
            LogManager.getLogger(ConfigurationUtils.class);

    private ConfigurationUtils() {
        super();
    }

    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, consider
     * invoking {@link #newXMLConfigurableInstance(HierarchicalConfiguration)} 
     * instead to have it automatically populated.
     * @param node the node representing the class to instanciate.
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    public static Object newInstance(
            HierarchicalConfiguration node)
            throws ConfigurationException {
        return newInstance(node, null);
    }
    /**
     * Creates a new instance of the class represented by the "class" attribute
     * on the given node.  The class must have an empty constructor.
     * If the class is an instance of {@link IXMLConfigurable}, consider
     * invoking {@link #newXMLConfigurableInstance(
     *      HierarchicalConfiguration, IXMLConfigurable)} instead to have
     * it automatically populated.
     * @param node the node representing the class to instantiate.
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     */
    @SuppressWarnings("nls")
    public static Object newInstance(
            HierarchicalConfiguration node, Object defaultObject)
            throws ConfigurationException {
        Object obj = null;
        if (node == null) {
            obj = defaultObject;
        } else {
            String clazz = node.getString("[@class]", null);
            if (clazz != null) {
                try {
                    obj = Class.forName(clazz).newInstance();
                } catch (Exception e) {
                    throw new ConfigurationException(
                            "This class could not be instantiated: \""
                            + clazz + "\".", e);
                }
            } else {
                LOG.warn("A configuration entry was found without class "
                       + "reference where one was needed; "
                       + "using default value:" + defaultObject);
                obj = defaultObject;
            }
        }
        return obj;
    }
    
    /**
     * Creates a new {@link IXMLConfigurable} instance of the class represented
     * by the "class" attribute on the given node.  The class must have an 
     * empty constructor.  The instance will be automatically populated by
     * invoking {@link IXMLConfigurable#loadFromXML(Reader)}.
     * @param node the node representing the class to instantiate.
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     * @throws IOException if instance cannot be created/populated
     */    
    public static IXMLConfigurable newXMLConfigurableInstance(
            HierarchicalConfiguration node)
            throws ConfigurationException, IOException {
        return newXMLConfigurableInstance(node, null);
    }
    
    /**
     * Creates a new {@link IXMLConfigurable} instance of the class represented
     * by the "class" attribute on the given node.  The class must have an 
     * empty constructor.  The instance will be automatically populated by
     * invoking {@link IXMLConfigurable#loadFromXML(Reader)}.
     * @param node the node representing the class to instantiate.
     * @param defaultObject if returned object is null or undefined,
     *        returns this default object.
     * @return a new object.
     * @throws ConfigurationException if instance cannot be created/populated
     * @throws IOException if instance cannot be created/populated
     */    
    public static IXMLConfigurable newXMLConfigurableInstance(
            HierarchicalConfiguration node, IXMLConfigurable defaultObject)
            throws ConfigurationException, IOException {
        
        IXMLConfigurable obj = (IXMLConfigurable) newInstance(node);
        if (obj == null) {
            return defaultObject;
        }
        if (obj != null && node != null) {
            obj.loadFromXML(newReader(node));
        }
        return obj;
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
            throws ConfigurationException, IOException {
        XMLConfiguration xml;
        if (node != null && node instanceof XMLConfiguration) {
            xml = (XMLConfiguration) node;
        } else {
            xml = new XMLConfiguration(node);
        }
        StringWriter w = new StringWriter();
        xml.save(w);
        StringReader r = new StringReader(w.toString());
        w.close();
        return r;
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
            throws IOException, ConfigurationException {
        
        File tempFile = File.createTempFile("XMLConfigurableTester", ".xml");
        
        // Write
        FileWriter out = new FileWriter(tempFile);
        xmlConfiurable.saveToXML(out);
        out.close();
        
        // Read
        XMLConfiguration xml = new ConfigurationLoader().loadXML(tempFile);
        IXMLConfigurable readConfigurable = 
                (IXMLConfigurable) ConfigurationUtils.newInstance(xml);
        StringWriter w = new StringWriter();
        xml.save(w);
        StringReader r = new StringReader(w.toString());
        readConfigurable.loadFromXML(r);
        w.close();
        r.close();

        tempFile.delete();

        Assert.assertEquals("Saved and loaded XML are not the same.", 
                xmlConfiurable, readConfigurable);
    }
}
