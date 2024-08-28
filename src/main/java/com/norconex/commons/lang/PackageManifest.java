/* Copyright 2022 Norconex Inc.
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

import static com.norconex.commons.lang.text.StringUtil.ifBlank;
import static com.norconex.commons.lang.text.StringUtil.ifNotBlank;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Optional;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.Failable;

import com.norconex.commons.lang.version.SemanticVersion;
import com.norconex.commons.lang.version.SemanticVersionParser;
import com.norconex.commons.lang.version.SemanticVersionParserException;
import com.norconex.commons.lang.xml.XML;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A package manifest containing project or library identifying information.
 * @since 3.0.0
 */
@Slf4j
@EqualsAndHashCode
@Getter
@Builder(access = AccessLevel.PACKAGE)
public final class PackageManifest {

    private final String version;
    private final String vendor;
    private final String title;

    /**
     * Creates a new a <code>PackageManifest</code>.
     *
     * given class.
     * @param cls the class for which to get the package manifest
     * @return package manifest, never <code>null</code>
     */
    public static PackageManifest of(@NonNull Class<?> cls) {
        var pmb = new PackageManifestBuilder();
        fromJarManifest(pmb, cls);
        if (isIncomplete(pmb)) {
            fromClassloaderManifest(pmb, cls);
        }
        if (isIncomplete(pmb)) {
            fromUnpackedPomXml(pmb, cls);
        }
        return pmb.build();
    }

    /**
     * Converts the version string to a {@link SemanticVersion} using a
     * strict parser.  If the package manifest does not provide a version,
     * this method will return
     * {@link SemanticVersion#UNVERSIONED}.
     * @return semantic version, never <code>null</code>
     * @throws SemanticVersionParserException
     *     if the string version could not be parsed
     */
    public SemanticVersion getSemanticVersion() {
        if (StringUtils.isBlank(version)) {
            return SemanticVersion.UNVERSIONED;
        }
        return SemanticVersionParser.STRICT.parse(version);
    }

    /**
     * Gets whether this package manifest does not have any
     * attributes set.
     * @return <code>true</code> if empty
     */
    public boolean isEmpty() {
        return StringUtils.isAllBlank(version, vendor, title);
    }

    /**
     * Returns a friendly string representation of this package manifest
     * or an empty string if this package manifest is empty (no attributes
     * set).
     */
    @Override
    public String toString() {
        var b = new StringBuilder();
        ifNotBlank(title, t -> b.append(t).append(' '));
        ifNotBlank(version, v -> b.append(v).append(' '));
        ifNotBlank(vendor, v -> b.append('(').append(v).append(')'));
        return b.toString().trim();
    }

    //--- Private methods ------------------------------------------------------

    // Tip: Can be build with Maven Jar plugin with
    // addDefaultImplementationEntries = true
    private static void fromJarManifest(
            PackageManifestBuilder pmb, Class<?> cls) {
        var p = cls.getPackage();
        pmb.version(p.getImplementationVersion());
        pmb.vendor(p.getImplementationVendor());
        pmb.title(p.getImplementationTitle());
    }

    private static void fromClassloaderManifest(
            PackageManifestBuilder pmb, Class<?> cls) {
        if (cls.getClassLoader() == null) {
            return;
        }
        try (var is = cls.getClassLoader().getResourceAsStream(
                "META-INF/MANIFEST.MF")) {
            if (is != null) {
                var attrs = new Manifest(is).getMainAttributes();
                ifBlank(pmb.version,
                        () -> attrs.getValue("Implementation-Version"));
                ifBlank(pmb.title,
                        () -> attrs.getValue("Implementation-Title"));
                ifBlank(pmb.vendor,
                        () -> attrs.getValue("Implementation-Vendor"));
            }
        } catch (IOException e) {
            LOG.trace("Could not obtain manifest from class loader.", e);
        }
    }

    // When unpacked, if Maven structure is respected, the pom.xml location
    // should be found at: ../../pom.xml
    // (from current directory: [...]/target/classes/).
    private static void fromUnpackedPomXml(
            PackageManifestBuilder pmb, Class<?> cls) {
        try {
            Optional.ofNullable(cls.getProtectionDomain().getCodeSource())
                    .map(CodeSource::getLocation)
                    .map(url -> Failable.<URI, URISyntaxException>call(
                            () -> url.toURI().resolve("../../pom.xml")))
                    .map(uri -> new File(uri.getPath()))
                    .map(pomFile -> XML.of(pomFile).create())
                    .ifPresent(xml -> {
                        ifBlank(pmb.version, () -> pmb.version(
                                xml.getString("version")));
                        ifBlank(pmb.version, () -> pmb.version(
                                xml.getString("parent/version")));
                        ifBlank(pmb.title, () -> pmb.title(
                                xml.getString("name")));
                        ifBlank(pmb.vendor, () -> pmb.vendor(
                                xml.getString("organization/name")));
                    });
        } catch (Exception e) {
            LOG.trace("Could not obtain pom.xml from code source.", e);
        }
    }

    private static boolean isIncomplete(PackageManifestBuilder pmb) {
        return StringUtils.isAnyBlank(pmb.version, pmb.vendor, pmb.title);
    }
}
