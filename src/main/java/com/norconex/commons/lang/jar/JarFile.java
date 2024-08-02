/* Copyright 2016-2022 Norconex Inc.
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
package com.norconex.commons.lang.jar;

import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;

import java.io.File;
import java.io.FileFilter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.stream.Streams;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.version.SemanticVersion;
import com.norconex.commons.lang.version.SemanticVersionParser;
import com.norconex.commons.lang.version.SemanticVersionParserException;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;


/**
 * Simple Jar file representation holding name and version information.
 * @since 1.10.0
 */
@Slf4j
@EqualsAndHashCode
public class JarFile implements Comparable<JarFile> {

    public static final FileFilter FILTER =
            new SuffixFileFilter(".jar");

    private static final Path[] emptyPaths = {};

    private final File file;
    private final String fullName;
    private final String baseName;
    private final String version;
    private final SemanticVersion semanticVersion;
    private final long lastModified;

    public JarFile(@NonNull File jarFile) {
        if (!jarFile.isFile()) {
            throw new IllegalArgumentException(
                    "Not a valid file: " + jarFile);
        }
        if (!jarFile.getName().endsWith(".jar")) {
            throw new IllegalArgumentException(
                    "File must end with a .jar extension: " + jarFile);
        }
        file = jarFile;
        fullName = jarFile.getName();

        var p = Pattern.compile("(.*?)-(\\d[\\.\\w-]*)\\.jar");
        var m = p.matcher(fullName);
        if (m.find()) {
            var jarName = m.group(1);
            var jarVersion = m.group(2);
            baseName = jarName;
            version = jarVersion;
        } else {
            baseName = StringUtils.removeEnd(fullName, ".jar");
            version = null;
        }

        var semVer = SemanticVersion.UNVERSIONED;
        if (StringUtils.isNotBlank(version)) {
            try {
                semVer = SemanticVersionParser.LENIENT.parse(version);
            } catch (SemanticVersionParserException e) {
                LOG.debug(e.getMessage());
            }
        }
        semanticVersion = semVer;
        lastModified = file.lastModified();
    }

    /**
     * Returns this jar file as a {@link File}.
     * @return file
     * @since 3.0.0
     */
    public File toFile() {
        return file;
    }
    /**
     * Gets this jar file as a {@link File}.
     * @return a file
     * @deprecated Use {@link #toFile()} instead.
     */
    @Deprecated(since = "3.0.0")
    public File getPath() { //NOSONAR
        return file;
    }
    public String getFullName() {
        return fullName;
    }
    public String getBaseName() {
        return baseName;
    }
    public String getVersion() {
        return version;
    }
    public Date getLastModified() {
        return new Date(file.lastModified());
    }

    public boolean isVersionGreaterThan(JarFile file) {
        return compareTo(file) > 0;
    }

    @Override
    public String toString() {
        return file.toString();
    }

    /**
     * Gets whether this Jar has the same name and version as
     * the provided jar.
     * @param jarFile jar file
     * @return <code>true</code> if the jar names and versions are the same.
     * @since 1.13.0
     * @deprecated Use {@link #isEquivalentTo(JarFile)} instead.
     */
    @Deprecated(since="3.0.0")
    public boolean isSameVersion(JarFile jarFile) { //NOSONAR
        if (jarFile == null) {
            return false;
        }
        return fullName.equals(jarFile.fullName);
    }

    /**
     * Gets whether a jar file is considered a duplicate of this one.
     * Two jar files are duplicates if they share the same base name, regardless
     * of their version or last modified dates. Equivalent jars are always
     * duplicates, but duplicates are not always equivalent.
     * @param jarFile the jar file to test
     * @return <code>true</code> if the jar file is a duplicate
     * @see #isEquivalentTo(JarFile)
     * @since 3.0.0
     */
    public boolean isDuplicateOf(JarFile jarFile) {
        if (jarFile == null) {
            return false;
        }
        return baseName.equals(jarFile.baseName);
    }

    /**
     * Gets whether this Jar has the same name and version as
     * the provided jar, as well as the same last modified date.
     * @param jarFile jar file
     * @return <code>true</code> if the jar names, versions and last modified
     *         dates are the same.
     * @since 1.13.0
     * @deprecated Use {@link #isEquivalentTo(JarFile)} instead.
     */
    @Deprecated(since="3.0.0")
    public boolean isSameVersionAndTime(JarFile jarFile) { //NOSONAR
        return isSameVersion(jarFile)  //NOSONAR
                && file.lastModified() == jarFile.file.lastModified();
    }

    /**
     * Gets whether this jar file is equivalent to the other if they both
     * have the same semantic version and last modified date. An equivalent
     * jar is always considered a duplicates (same base names), but duplicates
     * are not always equivalent.
     * @param other other jar file we are comparing to
     * @return <code>true</code> if this jar file is equivalent to the other
     * @see #isDuplicateOf(JarFile)
     */
    public boolean isEquivalentTo(JarFile other) {
        return compareTo(other) == 0;
    }
    /**
     * Gets whether this jar file is greater than the other,
     * by comparing semantic versions and last modified dates.
     * @param other other jar file we are comparing to
     * @return <code>true</code> if this jar file is greater than the other
     */
    public boolean isGreaterThan(JarFile other) {
        return compareTo(other) > 0;
    }
    /**
     * Gets whether this jar file is greater or equivalent to the other,
     * by comparing semantic versions and last modified dates.
     * @param other other jar file we are comparing to
     * @return <code>true</code> if this jar file is greater or equivalent
     *      to the other
     */
    public boolean isGreaterOrEquivalentTo(JarFile other) {
        return compareTo(other) >= 0;
    }
    /**
     * Gets whether this jar file lower than the other,
     * by comparing semantic versions and last modified dates.
     * @param other other jar file we are comparing to
     * @return <code>true</code> if this jar file is lower than the other
     */
    public boolean isLowerThan(JarFile other) {
        return compareTo(other) < 0;
    }
    /**
     * Gets whether this jar file is lower or equivalent to
     * the other.
     * @param other other jar file we are comparing to
     * @return <code>true</code> if this jar file is lower or equivalent
     *      to the other
     */
    public boolean isLowerOrEquivalentTo(JarFile other) {
        return compareTo(other) <= 0;
    }

    @Override
    public int compareTo(JarFile o) {
        if (o == null) {
            return 1;
        }
        if (equals(o)) { //NOSONAR
            return 0;
        }

        if (!baseName.equals(o.baseName)) {
            return baseName.compareTo(o.baseName);
        }

        var result = semanticVersion.compareTo(o.semanticVersion);
        if (result != 0) {
            return result;
        }

        return Long.compare(lastModified, o.lastModified) * -1;
    }

    /**
     * Returns the supplied paths as a list of {@link JarFile}. The paths
     * can be any combination of jar files or directories of jar files
     * (does not recurse).
     * A jar file is interpreted to be a file with the ".jar" extension.
     * Files with different extensions are ignored.
     * @param jarPaths paths to jar files or directories containing jar files
     * @return a list of jar files or an empty list, never <code>null</code>.
     * @throws UncheckedIOException if a problem occurs while accessing files
     * @since 3.0.0
     */
    public static List<JarFile> toJarFiles(Collection<Path> jarPaths) {
        if (CollectionUtils.isEmpty(jarPaths)) {
            return Collections.emptyList();
        }
        return toJarFiles(jarPaths.toArray(emptyPaths));
    }

    /**
     * Returns the supplied paths as a list of {@link JarFile}. The paths
     * can be any combination of jar files or directories of jar files
     * (does not recurse).
     * A jar file is interpreted to be a file with the ".jar" extension.
     * Files with different extensions are ignored.
     * @param jarPaths paths to jar files or directories containing jar files
     * @return a list of jar files or an empty list, never <code>null</code>.
     * @throws UncheckedIOException if a problem occurs while accessing files
     * @since 3.0.0
     */
    public static List<JarFile> toJarFiles(Path... jarPaths) {
        List<JarFile> jarFiles = new ArrayList<>();
        Streams.failableStream(CollectionUtil.asListOrEmpty(jarPaths))
                .forEach(jarPath -> {
            try (var stream = Files.isDirectory(jarPath)
                    ? Files.list(jarPath)
                    : Stream.of(jarPath)) {
                jarFiles.addAll(stream
                    .filter(f -> endsWithIgnoreCase(f.toString(), ".jar"))
                    .map(f -> new JarFile(f.toFile()))
                    .collect(Collectors.toList()));
            }
        });
        return jarFiles;
    }

    /**
     * Returns the supplied paths as a list of {@link JarFile}. The paths
     * can be any combination of jar files or directories of jar files
     * (does not recurse).
     * A jar file is interpreted to be a file with the ".jar" extension.
     * Files with different extensions are ignored.
     * @param jarPaths paths to jar files or directories containing jar files
     * @return a list of jar files or an empty list, never <code>null</code>.
     * @throws UncheckedIOException if a problem occurs while accessing files
     * @since 3.0.0
     */
    public static List<JarFile> toJarFiles(File... jarPaths) {
        return toJarFiles(Arrays.asList(jarPaths).stream()
                .map(File::toPath)
                .collect(Collectors.toList()));
    }
}
