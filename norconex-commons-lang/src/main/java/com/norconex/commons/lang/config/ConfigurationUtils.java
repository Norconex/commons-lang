package com.norconex.commons.lang.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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
     * If the class is an instance of {@link IXMLConfigurable}, it will 
     * automatically populate it.
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
     * If the class is an instance of {@link IXMLConfigurable}, it will 
     * automatically populate it.
     * @param node the node representing the class to instanciate.
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
        //this is needed??
//        if (obj != null && node != null && obj instanceof IXMLConfigurable) {
//            ((IXMLConfigurable) obj).loadFromXML(node);
//        }
        return obj;
    }
    
}
