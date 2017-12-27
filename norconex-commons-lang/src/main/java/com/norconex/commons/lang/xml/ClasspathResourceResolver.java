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
package com.norconex.commons.lang.xml;

import java.io.InputStream;

import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * <p>
 * Resolves XML Schema (XSD) include directives by looking for the 
 * specified resource on the Classpath.
 * </p>
 * <p>
 * To use, set this resolver on your {@link SchemaFactory}, like this:
 * </p>
 * <pre>
 * SchemaFactory schemaFactory = 
 *         SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
 * schemaFactory.setResourceResolver(
 *         new ClasspathResourceResolver(MyClass.class));
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 1.13.0
 */
public class ClasspathResourceResolver implements LSResourceResolver {

    private static final Logger LOG = 
            LoggerFactory.getLogger(ClasspathResourceResolver.class);
    
    private final String rootPath;

    public ClasspathResourceResolver() {
        this((String) null);
    }
    
    /**
     * Resolves the resource relative to the given class.
     * @param relativeTo class to use as base for resolution
     */
    public ClasspathResourceResolver(Class<?> relativeTo) {
        this(getPackageResourcePathFromClass(relativeTo));
    }
    /**
     * Resolves the resource relative to the given package path.
     * @param relativeTo package path to use as base for resolution
     */
    public ClasspathResourceResolver(String relativeTo) {
        super();
        this.rootPath = relativeTo;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI,
            String publicId, String systemId, String baseURI) {
        String path = rootPath;
        if (baseURI != null) {
            path = getPackageResourcePathFromBaseURI(baseURI);
        }
        String r = getResourcePath(path, systemId);
        InputStream resourceAsStream = getClass().getResourceAsStream(r);
        if (resourceAsStream == null) {
            LOG.error("Resource not found: " + r
                    + " (baseURI: " + baseURI + "; systemId: " + systemId);
        }
        return new ClasspathInput(publicId, r, resourceAsStream);
    }

    private String getResourcePath(String path, String systemId) {
        if (systemId.startsWith("/")) {
            // Absolute path, no need to resolve
            return systemId;
        }
        int upCount = StringUtils.countMatches(systemId, "../");
        String newPath = path;
        for (int i = 0; i < upCount; i++) {
            newPath = newPath.replaceFirst("(.*/)(.*/)$", "$1");
        }
        return newPath + systemId.replaceFirst("^(../)+", "");
    }
    private static String getPackageResourcePathFromClass(Class<?> klass) {
        if (klass == null) {
            return StringUtils.EMPTY;
        }
        return "/" + klass.getPackage().getName().replace('.', '/') + "/";
        
    }
    private String getPackageResourcePathFromBaseURI(String baseURI) {
        if (baseURI == null) {
            return StringUtils.EMPTY;
        }
        return baseURI.replaceFirst("^(file://)*(.*/).*", "$2");
    }
}
