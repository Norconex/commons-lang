/* Copyright 2019-2021 Norconex Inc.
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

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.XMLException;

/**
 * Version-related convenience methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class VersionUtil {

    private static final Logger LOG =
            LoggerFactory.getLogger(VersionUtil.class);

    private VersionUtil() {
        super();
    }

    //TODO Extract from pom if not found in manifest?
    //TODO Create a Version class that breaks the parts and use it in JarFile

    /**
     * <p>
     * Gets the version from the project or library the provided class belongs
     * to. This method attempts to read that version information from the
     * class package implementation details, normally found in jar manifest
     * files.
     * </p>
     * <p>
     * The manifest is often created when the code is packaged for distribution.
     * For instance, if you use Maven, you can ask it to automaticall store
     * implementation details with the manifest file when creating a jar file
     * by setting <code>addDefaultImplementationEntries</code> set
     * to <code>true</code>
     * (see <a href="https://maven.apache.org/shared/maven-archiver/index.html">
     * https://maven.apache.org/shared/maven-archiver/</a>).
     * </p>
     * <p>
     * If developing (not yet packaged), an attempt is made to locate
     * a pom.xml and load the version from it.
     * </p>
     * @param cls the class used to extract version
     * @return the version number or <code>null</code> if not found
     */
    public static String getVersion(Class<?> cls) {
        return getVersion(cls, null);
    }

    /**
     * <p>
     * Gets the version from the project or library the provided class belongs
     * to. This method attempts to read that version information from the
     * class package implementation details, normally found in jar manifest
     * files.
     * </p>
     * <p>
     * The manifest is often created when the code is packaged for distribution.
     * For instance, if you use Maven, you can ask it to automaticall store
     * implementation details with the manifest file when creating a jar file
     * by setting <code>addDefaultImplementationEntries</code> set
     * to <code>true</code>
     * (see <a href="https://maven.apache.org/shared/maven-archiver/index.html">
     * https://maven.apache.org/shared/maven-archiver/</a>).
     * </p>
     * <p>
     * If developing (not yet packaged), an attempt is made to locate
     * a pom.xml and load the version from it.
     * </p>
     * @param cls the class used to extract version
     * @param fallback text to return when no version could be found
     * @return the version number or the fallback value if not found
     */
    public static String getVersion(Class<?> cls, String fallback) {
        Objects.requireNonNull(cls, "'cls' must not be null.");
        DetailedVersion dv = doGetDetailedVersion(cls);
        if (dv == null) {
            return fallback;
        }
        return dv.version;
    }

    /**
     * <p>
     * Gets the detailed version from the project or library the provided
     * class belongs to.
     * This method attempts to read that version information from the
     * class package implementation details, normally found in jar manifest
     * files.
     * </p>
     * <p>
     * The manifest is often created when the code is packaged for distribution.
     * For instance, if you use Maven, you can ask it to automaticall store
     * implementation details with the manifest file when creating a jar file
     * by setting <code>addDefaultImplementationEntries</code> set
     * to <code>true</code>
     * (see <a href="https://maven.apache.org/shared/maven-archiver/index.html">
     * https://maven.apache.org/shared/maven-archiver/</a>).
     * </p>
     * <p>
     * If developing (not yet packaged), an attempt is made to locate
     * a pom.xml and load the version from it.
     * </p>
     * @param cls the class used to extract version
     * @return the version number or the fallback value if not found
     */
    public static String getDetailedVersion(Class<?> cls) {
        return getDetailedVersion(cls, null);
    }
    /**
     * <p>
     * Gets the detailed version from the project or library the provided
     * class belongs to.
     * This method attempts to read that version information from the
     * class package implementation details, normally found in jar manifest
     * files.
     * </p>
     * <p>
     * The manifest is often created when the code is packaged for distribution.
     * For instance, if you use Maven, you can ask it to automatically store
     * implementation details with the manifest file when creating a jar file
     * by setting <code>addDefaultImplementationEntries</code> set
     * to <code>true</code>
     * (see <a href="https://maven.apache.org/shared/maven-archiver/index.html">
     * https://maven.apache.org/shared/maven-archiver/</a>).
     * </p>
     * <p>
     * If developing (not yet packaged), an attempt is made to locate
     * a pom.xml and load the version from it.
     * </p>
     * @param cls the class used to extract version
     * @param fallback text to return when no version could be found
     * @return the version number or the fallback value if not found
     */
    public static String getDetailedVersion(Class<?> cls, String fallback) {
        Objects.requireNonNull(cls, "'cls' must not be null.");
        DetailedVersion dv = doGetDetailedVersion(cls);
        if (dv == null) {
            return fallback;
        }

        StringBuilder b = new StringBuilder();
        if (StringUtils.isNotBlank(dv.title)) {
            b.append(dv.title).append(' ');
        }
        b.append(dv.version);
        if (StringUtils.isNotBlank(dv.vendor)) {
            b.append(" (").append(dv.vendor).append(')');
        }
        return b.toString();
    }

    private static DetailedVersion doGetDetailedVersion(Class<?> cls) {
        return ObjectUtils.firstNonNull(
                fromJarManifest(cls),
                fromUnpackedMavenPomXml(cls)
        );
    }

    // When unpacked, if Maven structure is respected, the pom.xml location
    // should be found at: ../../pom.xml
    // (from current directory: [...]/target/classes/).
    private static DetailedVersion fromUnpackedMavenPomXml(Class<?> cls) {
        try {
            CodeSource source = cls.getProtectionDomain().getCodeSource();
            if (source != null) {
                File pom = new File(source.getLocation().toURI().resolve(
                        "../../pom.xml").getPath());
                XML xml = XML.of(pom).create();
                String version = xml.getString("version");
                if (StringUtils.isBlank(version)) {
                    return null;
                }
                return new DetailedVersion(
                        version,
                        xml.getString("name"),
                        xml.getString("organization/name"));
            }
        } catch (XMLException | URISyntaxException | SecurityException e) {
            LOG.trace("Could not obtain pom.xml from source.", e);
        }
        return null;
    }

    // Maven Jar plugin with addDefaultImplementationEntries = true
    private static DetailedVersion fromJarManifest(Class<?> cls) {
        Package p = cls.getPackage();
        String version = p.getImplementationVersion();
        if (StringUtils.isBlank(version)) {
            return null;
        }
        return new DetailedVersion(
                version,
                p.getImplementationTitle(),
                p.getImplementationVendor());
    }

    private static class DetailedVersion {
        final private String version;
        final private String title;
        final private String vendor;
        public DetailedVersion(String version, String title, String vendor) {
            super();
            this.version = version;
            this.title = title;
            this.vendor = vendor;
        }
    }
}
