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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.comparators.ReverseComparator;

import com.norconex.commons.lang.collection.CollectionUtil;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * Holds jar files that are considered duplicates. Jar duplicates are defined
 * as having the same base name, which is the regular file name, minus the
 * version information.
 * @since 1.10.0
 */
@ToString
@EqualsAndHashCode
public class JarDuplicates {

    private final List<JarFile> jarFiles;

    /**
     * Creates a group of 2 or more {@link JarFile} duplicates.
     * @param jarFiles duplicate jar files (must not be <code>null</code>)
     * @throws IllegalArgumentException if jar file array
     *     contains less than 2 non-<code>null</code> jar files.
     * @since 3.0.0
     */
    public JarDuplicates(@NonNull JarFile... jarFiles) {
        this(Arrays.asList(jarFiles));
    }

    /**
     * Creates a group of 2 or more {@link JarFile} duplicates.
     * @param jarFiles duplicate jar files (must not be <code>null</code>)
     * @throws IllegalArgumentException if jar file collection
     *     contains less than 2 non-<code>null</code> jar files.
     * @since 3.0.0
     */
    public JarDuplicates(@NonNull Collection<JarFile> jarFiles) {
        Set<JarFile> cleanSet = new HashSet<>(jarFiles);
        CollectionUtil.removeNulls(cleanSet);
        if (cleanSet.size() < 2) {
            throw new IllegalArgumentException(
                    "Must have 2 or more jar files.");
        }
        List<JarFile> cleanList = new ArrayList<>(cleanSet);
        Collections.sort(cleanList, new ReverseComparator<>());
        this.jarFiles = Collections.unmodifiableList(cleanList);
    }

    /**
     * @since 3.0.0
     * @return the file name, minus the version and file extension
     */
    public String getBaseName() {
        return jarFiles.get(0).getBaseName();
    }

    /**
     * Gets the jar files being considered duplicates of each other.
     * Since 3.0.0, returns a {@link List} instead of an array, sorted
     * from greatest to lowest.
     * @return jar files
     */
    public List<JarFile> getJarFiles() {
        return jarFiles;
    }

    /**
     * Gets the jar file that is considered the greatest of the batch
     * based on their version and last modified date (in case of equivalent
     * versions).
     * If two ore more jars are candidates to be greatest (equivalent versions
     * and modified date), there are no guarantees as to which one will be
     * returned.
     * Versions are expected to follow semantic versioning. Otherwise,
     * a best effort it made to identify and convert versions to semantic
     * ones for the purpose of comparing them.
     * Given non-semantic version patterns vary, there
     * are no guarantees of 100% accuracy in such case.
     * @return greatest jar file
     * @deprecated Use {@link #getGreatest()} instead.
     */
    @Deprecated(since = "3.0.0")
    public JarFile getLatestVersion() { //NOSONAR
        return jarFiles.iterator().next();
    }

    /**
     * Gets the duplicate jar file that is considered the greatest
     * based on its version and last modified date (in case of equivalent
     * versions).
     * If two ore more jars are candidates to be greatest (equivalent versions
     * and modified date), there are no guarantees as to which one will be
     * returned.
     * Versions are expected to follow semantic versioning. Otherwise,
     * a best effort it made to identify and convert versions to semantic
     * ones for the purpose of comparing them.
     * Given non-semantic version patterns vary, there
     * are no guarantees of 100% accuracy in such case.
     * @return greatest jar file
     * @since 3.0.0
     */
    public JarFile getGreatest() {
        return jarFiles.iterator().next();
    }

    /**
     * Gets all jar files of this duplicate batch except for the one
     * considered the greatest, as per {@link #getGreatest()}.
     * The jar files are sorted from greatest to lowest.
     * @return all but the greatest jar files
     * @since 3.0.0
     */
    public List<JarFile> getAllButGreatest() {
        JarFile greatest = getGreatest();
        return jarFiles
                .stream()
                .filter(jf -> !jf.equals(greatest))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Whether all jar files share the same version or if at least one of them
     * have a different version.
     * @return <code>true</code> if at least one jar file has a different
     *         version
     * @deprecated Use !{@link #areEquivalent()} instead.
     */
    @Deprecated(since = "3.0.0")
    public boolean hasVersionConflict() { //NOSONAR
        return !areEquivalent();
    }

    /**
     * Gets whether all jar files in this duplicate are equivalent
     * (equivalent versions and identical last modified dates).
     * @return <code>true</code> if all jars are equivalent
     * @since 3.0.0
     */
    public boolean areEquivalent() {
        JarFile firstJar = getGreatest();
        for (JarFile jarFile : jarFiles) {
            if (!firstJar.isEquivalentTo(jarFile)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the {@link JarFile} corresponding to the supplied file by comparing
     * their paths.
     * @param file file to get its corresponding {@link JarFile}
     * @return an optional with the matching jar file, or empty if none found
     * @since 3.0.0
     */
    public Optional<JarFile> get(File file) {
        return jarFiles.stream()
                .filter(j -> j.toFile().equals(file))
                .findFirst();
    }

    /**
     * Gets whether this duplicate batch contains the given jar file,
     * by comparing their paths.
     * Same as invoking <code>isPresent()</code> on the returned
     * value of {@link #get(File)}.
     * @param jarFile jar file
     * @return <code>true</code> if this instance contains the given jar file
     */
    public boolean contains(File jarFile) {
        for (JarFile jar : jarFiles) {
            if (Objects.equals(jar.toFile(), jarFile)) {
                return true;
            }
        }
        return false;
    }
}
