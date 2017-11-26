/* Copyright 2017 Norconex Inc.
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
package com.norconex.commons.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Utility class for loading resource from class loader, relative to 
 * a given class.
 * 
 * @author Pascal Essiembre
 * @since 1.14.0
 */
public final class ResourceLoader {

    private static final Logger LOG = Logger.getLogger(ResourceLoader.class);

    private ResourceLoader() {
        super();
    }

    /**
     * Gets a resource from class loader as stream. The resource is
     * relative to the given class, with the same name as the class, but 
     * with the supplied suffix.  
     * @param clazz the class to load related resource
     * @param suffix the suffix of the file to load with the same name 
     *        as class
     * @return input stream or <code>null</code> if class is <code>null</code>
     *         or resource could not be found. 
     */
    public static InputStream getStream(Class<?> clazz, String suffix) {
        if (clazz == null) {
            LOG.debug("Class is null for suffix: " + suffix);
            return null;
        }
        return clazz.getResourceAsStream(clazz.getSimpleName() + suffix);
        
    }
    /**
     * Gets a UTF-8 resource from class loader as a reader. The resource is
     * relative to the given class, with the same name as the class, but 
     * with the supplied suffix.  
     * @param clazz the class to load related resource
     * @param suffix the suffix of the file to load with the same name 
     *        as class
     * @return reader or <code>null</code> if class is <code>null</code>
     *         or resource could not be found. 
     */
    public static Reader getReader(Class<?> clazz, String suffix) {
        InputStream is = getStream(clazz, suffix);
        if (is == null) {
            LOG.debug("InputStream is null for class/suffix: "
                    + clazz + "/" + suffix);
            return null;
        }
        return new InputStreamReader(is, StandardCharsets.UTF_8);
    }
    /**
     * Gets a UTF-8 a resource from class loader as a string. The resource is
     * relative to the given class, with the same name as the class, but 
     * with the supplied suffix.  
     * @param clazz the class to load related resource
     * @param suffix the suffix of the file to load with the same name 
     *        as class
     * @return string or <code>null</code> if class is <code>null</code>
     *         or resource could not be found. 
     */
    public static String getString(Class<?> clazz, String suffix) {
        Reader r = getReader(clazz, suffix);
        if (r == null) {
            LOG.debug("Reader is null for class/suffix: "
                    + clazz + "/" + suffix);
            return null;
        }
        try {
            return IOUtils.toString(r);
        } catch (IOException e) {
            LOG.error("Could not load class/suffix as string: "
                    + clazz + "/" + suffix, e);
            return null;
        }
    }

    /**
     * Gets a stream from a resource matching class name with ".xml" suffix.
     * @param clazz the class to load related resource
     * @return XML stream or <code>null</code>
     */
    public static InputStream getXmlStream(Class<?> clazz) {
        return getStream(clazz, ".xml");
    }
    /**
     * Gets a UTF-8 reader from resource matching class name with ".xml" suffix.
     * @param clazz the class to load related resource
     * @return XML reader or <code>null</code>
     */
    public static Reader getXmlReader(Class<?> clazz) {
        return getReader(clazz, ".xml");
    }
    /**
     * Gets a UTF-8 string from resource matching class name with ".xml" suffix.
     * @param clazz the class to load related resource
     * @return XML string or <code>null</code>
     */
    public static String getXmlString(Class<?> clazz) {
        return getString(clazz, ".xml");
    }
    /**
     * Gets a stream from a resource matching class name with ".txt" suffix.
     * @param clazz the class to load related resource
     * @return text stream or <code>null</code>
     */
    public static InputStream getTxtStream(Class<?> clazz) {
        return getStream(clazz, ".txt");
    }
    /**
     * Gets a UTF-8 reader from resource matching class name with ".txt" suffix.
     * @param clazz the class to load related resource
     * @return text reader or <code>null</code>
     */
    public static Reader getTxtReader(Class<?> clazz) {
        return getReader(clazz, ".txt");
    }
    /**
     * Gets a UTF-8 string from resource matching class name with ".txt" suffix.
     * @param clazz the class to load related resource
     * @return text string or <code>null</code>
     */
    public static String getTxtString(Class<?> clazz) {
        return getString(clazz, ".txt");
    }
    /**
     * Gets a stream from a resource matching class name with ".html" suffix.
     * @param clazz the class to load related resource
     * @return HTML stream or <code>null</code>
     */
    public static InputStream getHtmlStream(Class<?> clazz) {
        return getStream(clazz, ".html");
    }
    /**
     * Gets a UTF-8 reader from resource matching class name with ".html" 
     * suffix.
     * @param clazz the class to load related resource
     * @return HTML reader or <code>null</code>
     */
    public static Reader getHtmlReader(Class<?> clazz) {
        return getReader(clazz, ".html");
    }
    /**
     * Gets a UTF-8 string from resource matching class name with ".html" 
     * suffix.
     * @param clazz the class to load related resource
     * @return HTML string or <code>null</code>
     */
    public static String getHtmlString(Class<?> clazz) {
        return getString(clazz, ".html");
    }
}
