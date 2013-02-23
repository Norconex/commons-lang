package com.norconex.commons.lang.meta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

//TODO consider having property change listeners?

/**
 * Represents a set of metadata (e.g. associated with a document).
 * @author Pascal Essiembre
 */
public class Metadata implements Cloneable, Serializable {

    private static final long serialVersionUID = 6812470691667930563L;
    private static final MetaProperty[] EMPTY_PROPERTIES = 
            new MetaProperty[] {};

    private final List<MetaProperty> properties = new ArrayList<MetaProperty>();
    private final boolean caseSensitiveProperties; 
    
    /**
     * Constructor.
     */
    public Metadata() {
        this(false);
    }

    /**
     * Constructor.
     * @param caseSensitiveProperties respect property character case
     */
    public Metadata(boolean caseSensitiveProperties) {
        super();
        this.caseSensitiveProperties = caseSensitiveProperties;
    }
    
    /**
     * Gets all properties.
     * @return all properties
     */
    public MetaProperty[] getProperties() {
        return properties.toArray(EMPTY_PROPERTIES);
    }
    /**
     * Gets the property matching the properties name
     * @param propertyName the property name
     * @return the property, or <code>null</code> if no property is found
     */
    public MetaProperty getProperty(String propertyName) {
        for (MetaProperty property : properties) {
            if (isSameName(propertyName, property.getName())) {
                return property;
            }               
        }
        return null;
    }
    /**
     * Sets one or more properties on this metadata instance.
     * @param property one or more properties to set
     */
    public void setProperty(MetaProperty... property) {
        if (property != null) {
            for (MetaProperty p : property) {
                if (p != null) {
                    MetaProperty found = getProperty(p.getName());
                    if (found != null) {
                        properties.remove(found);
                    }
                    properties.add(p);
                }
            }
        }
    }
    public void setPropertyValue(String propertyName, String... value) {
        MetaProperty p = getProperty(propertyName);
        if (p != null) {
            p.setValues(Arrays.asList(value));
            
        } else {
            setProperty(new MetaProperty(propertyName, value));
        }
    }
    public void addProperty(MetaProperty... property) {
        if (property != null) {
            for (MetaProperty p : property) {
                if (p != null) {
                    addPropertyValue(p.getName(), p.getValues().toArray(
                            ArrayUtils.EMPTY_STRING_ARRAY));
                }
            }
        }
    }
    public void addPropertyValue(String propertyName, String... value) {
        MetaProperty p = getProperty(propertyName);
        if (p != null) {
            p.addValues(Arrays.asList(value));
        } else {
            setProperty(new MetaProperty(propertyName, value));
        }
    }
    public String getPropertyValue(String propertyName) {
        MetaProperty p = getProperty(propertyName);
        if (p != null) {
            return p.getValue();
        }
        return null;
    }
    public List<String> getPropertyValues(String propertyName) {
        MetaProperty p = getProperty(propertyName);
        if (p != null) {
            return p.getValues();
        }
        return new ArrayList<String>(0);
    }
    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
    }
    
    
    /**
     * Clears this metadata instance of all its properties.
     */
    public void clear() {
        properties.clear();
    }
    /**
     * Returns <code>true</code> if this metadata has no properties.
     * @return <code>true</code> if this metadata has no properties.
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * Whether this metadata instance respects properties name character case. 
     * @return <code>true</code> if case is respected
     */
    public boolean isCaseSensitiveProperties() {
        return caseSensitiveProperties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Metadata))
            return false;
        Metadata other = (Metadata) obj;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "Metadata [properties=" + properties
                + ", caseSensitiveProperties=" + caseSensitiveProperties + "]";
    }

    synchronized public void store(OutputStream outputStream) 
            throws IOException {
        PrintWriter out = new PrintWriter(outputStream);
        MetaProperty[] props = getProperties();
        for (MetaProperty meta : props) {
            List<String> values = meta.getValues();
            for (String value : values) {
                out.print(URLEncoder.encode(
                        meta.getName(), CharEncoding.UTF_8));
                out.print('=');
                out.println(URLEncoder.encode(
                        ObjectUtils.toString(value), CharEncoding.UTF_8));
            }
        }
        out.close();
    }

    synchronized public void load(InputStream inputStream) 
            throws IOException {
        LineIterator iterator =
                IOUtils.lineIterator(inputStream, CharEncoding.UTF_8);
        while (iterator.hasNext()) {
             String line = iterator.nextLine();
             String key = StringUtils.substringBefore(line, "=");
             if (key != null) {
                 String value = StringUtils.substringAfter(line, "=");
                 addPropertyValue(
                		 URLDecoder.decode(key, CharEncoding.UTF_8), 
                		 URLDecoder.decode(value, CharEncoding.UTF_8));
             }
        }
    }

    @Override
    public Object clone() {
        Metadata meta = new Metadata(caseSensitiveProperties);
        for (MetaProperty property : properties) {
            meta.setPropertyValue(
                    property.getName(), property.getValues().toArray(
                            ArrayUtils.EMPTY_STRING_ARRAY));
        }
        return meta;
    }
    
    
    private boolean isSameName(String n1, String n2) {
        String name1 = n1;
        if (name1 != null && !caseSensitiveProperties) {
            name1 = name1.toLowerCase();
        }
        String name2 = n2;
        if (name2 != null && !caseSensitiveProperties) {
            name2 = name2.toLowerCase();
        }
        return ObjectUtils.equals(name1, name2);
    }
    
}
