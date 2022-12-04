/* Copyright 2019-2022 Norconex Inc.
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

import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.version.SemanticVersion;

import lombok.NonNull;

/**
 * Version-related convenience methods.
 * @since 2.0.0
 * @see PackageManifest
 * @see SemanticVersion
 * @deprecated Use {@link PackageManifest} instead.
 */
@Deprecated(forRemoval = true, since = "3.0.0")
public final class VersionUtil { //NOSONAR

    private VersionUtil() {}

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
    public static String getVersion(@NonNull Class<?> cls, String fallback) {
        String version = PackageManifest.of(cls).getVersion();
        return StringUtils.isBlank(version) ? fallback : version;
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
    public static String getDetailedVersion(
            @NonNull Class<?> cls, String fallback) {
        String str = PackageManifest.of(cls).toString();
        return StringUtils.isBlank(str) ? fallback : str;
    }
}
