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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.comparators.ReverseComparator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.norconex.commons.lang.file.FileUtil;

import lombok.NonNull;

/**
 * Utility class for finding multiple instances of the same Jar that exists
 * either in different folder or the same, with the same name and version,
 * or same name but different versions.  This class will consider Jar as
 * being duplicates based on their base name, not their content or any
 * other indicator.
 * @author Pascal Essiembre
 * @since 1.10.0
 */
public final class JarDuplicateFinder {

    private static final File[] emptyFiles = {};

    private JarDuplicateFinder() {}

    /**
     * Finds all jar duplicates.
     * Supplied paths can be any combination of jar files or directory of
     * jar files. A jar file is any file with the ".jar" extension.
     * Files with different extensions are ignored.
     * @param jarPaths paths to either jar files or directories containing jars
     * @return list of jar duplicates, or an empty list when no duplicate Jars
     *    are found, never <code>null</code>.
     * @since 3.0.0
     */
    public static List<JarDuplicates> findJarDuplicates(
            @NonNull Collection<? extends File> jarPaths) {
        return findJarDuplicates(jarPaths.toArray(emptyFiles));
    }

    /**
     * Finds all jar duplicates.
     * Supplied paths can be any combination of jar files or directory of
     * jar files. A jar file is any file with the ".jar" extension.
     * Files with different extensions are ignored.
     * @param jarPaths paths to either jar files or directories containing jars
     * @return list of jar duplicates, or an empty list when no duplicate Jars
     *    are found, never <code>null</code>.
     */
    public static List<JarDuplicates> findJarDuplicates(
            @NonNull String... jarPaths) {
        if (ArrayUtils.isEmpty(jarPaths)) {
            return new ArrayList<>();
        }
        File[] dirs = new File[jarPaths.length];
        for (int i = 0; i < jarPaths.length; i++) {
            dirs[i] = new File(jarPaths[i]);
        }
        return findJarDuplicates(dirs);
    }

    /**
     * Finds all jar duplicates.
     * Supplied paths can be any combination of jar files or directory of
     * jar files. A jar file is any file with the ".jar" extension.
     * Files with different extensions are ignored.
     * @param jarPaths paths to either jar files or directories containing jars
     * @return list of jar duplicates, or an empty list when no duplicate Jars
     *    are found, never <code>null</code>.
     */
    public static List<JarDuplicates> findJarDuplicates(
            @NonNull File... jarPaths) {
        MultiValuedMap<String, JarFile> jarPathsByBaseName =
                MultiMapUtils.newListValuedHashMap();

        JarFile.toJarFiles(FileUtil.toPaths(jarPaths))
            .forEach(jf -> jarPathsByBaseName.put(jf.getBaseName(), jf));

        return jarPathsByBaseName.asMap().entrySet().stream()
            .filter(en -> en.getValue().size() > 1)
            .map(en -> new JarDuplicates(en.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * Finds duplicates of a specific jar file, without returning that jar
     * itself.
     * Supplied target paths can be any combination of jar files or directory of
     * jar files. A jar file is any file with the ".jar" extension.
     * Files with different extensions are ignored.
     * @param jarFile the Jar file being compared
     * @param paths paths to potential duplicate jar files or
     *     directories containing jar files
     * @return jar file duplicates, sorted from greatest to lowest, or an
     *     empty list when no duplicate Jars are found, never <code>null</code>
     * @since 3.0.0
     */
    public static List<JarFile> findJarDuplicatesOf(
            @NonNull File jarFile, @NonNull List<File> paths) {
        JarFile source = new JarFile(jarFile);
        List<JarFile> duplicates = JarFile.toJarFiles(
                FileUtil.toPaths(paths)).stream()
            .filter(source::isDuplicateOf)
            .collect(Collectors.toList());
        Collections.sort(duplicates, new ReverseComparator<>());
        return Collections.unmodifiableList(duplicates);
    }

    public static void main(String[] args) {
        List<JarDuplicates> dups = JarDuplicateFinder.findJarDuplicates(args);
        System.out.println("Found " + dups.size() //NOSONAR
                + " Jar(s) having one or more duplicates (equivalent ones "
                + "marked with *).");
        for (JarDuplicates jarDuplicates : dups) {
            System.out.println(); //NOSONAR
            System.out.println( //NOSONAR
                    jarDuplicates.getGreatest().getBaseName() + ":");
            for (JarFile jarFile : jarDuplicates.getJarFiles()) {
                if (jarDuplicates.areEquivalent()) {
                    System.out.print(" * ");  //NOSONAR
                } else {
                    System.out.print("   ");  //NOSONAR
                }
                System.out.println(jarFile.toFile() + " ["  //NOSONAR
                        + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT
                                .format(jarFile.getLastModified()) + "]");
            }
        }
    }
}
