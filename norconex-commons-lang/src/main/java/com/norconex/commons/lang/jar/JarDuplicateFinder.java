/* Copyright 2016-2017 Norconex Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.AbstractSetValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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

    private static final Logger LOG = 
            LogManager.getLogger(JarDuplicateFinder.class);
    
    private JarDuplicateFinder() {
        super();
    }

    /**
     * Finds all jar duplicates. This method is null-safe and returns
     * an empty list if no duplicate Jars are found.
     * @param jarPaths paths to either jar files or directories containing jars
     * @return list of jar duplicates
     */
    public static List<JarDuplicates> findJarDuplicates(String... jarPaths) {
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
     * Finds all jar duplicates. This method is null-safe and returns
     * an empty list if no duplicate Jars are found.
     * @param jarPaths paths to either jar files or directories containing jars
     * @return list of jar duplicates
     */
    public static List<JarDuplicates> findJarDuplicates(File... jarPaths) {
        MultiValuedMap<String, JarFile> allJars = new HashTreeValuedHashMap<>();
        for (File path : jarPaths) {
            File[] jars;
            if (path.isDirectory()) {
                jars = path.listFiles(JarFile.FILTER);
            } else if (path.isFile() && JarFile.FILTER.accept(path)) {
                jars = new File[] { path };
            } else {
                jars = new File[] { };
                LOG.warn("Path is not a valid jar file or directory: " + path);
            }
            for (File jar : jars) {
                JarFile jarFile = new JarFile(jar);
                allJars.put(jarFile.getBaseName(), jarFile);
            }
        }

        JarFile[] emptyJarFiles = new JarFile[]{};
        List<JarDuplicates> dups = new ArrayList<>();
        for (String baseName : allJars.keySet()) {
            Collection<JarFile> jarFiles = allJars.get(baseName);
            if (jarFiles.size() > 1) {
                dups.add(new JarDuplicates(jarFiles.toArray(emptyJarFiles)));
            }
        }
        
        return dups;
    }

    public static void main(String[] args) {
        List<JarDuplicates> dups = JarDuplicateFinder.findJarDuplicates(args);
        System.out.println("Found " + dups.size()
                + " Jar(s) having ore more duplicates.");
        for (JarDuplicates jarDuplicates : dups) {
            System.out.println();
            System.out.println(
                    jarDuplicates.getLatestVersion().getBaseName() + ":");
            for (JarFile jarFile : jarDuplicates.getJarFiles()) {
                if (jarDuplicates.hasVersionConflict() 
                        && jarDuplicates.getLatestVersion().equals(jarFile)) {
                    System.out.print(" * ");
                } else {
                    System.out.print("   ");
                }
                System.out.println(jarFile.getPath() + " ["
                        + DateFormatUtils.ISO_DATETIME_FORMAT.format(
                                jarFile.getLastModified()) + "]");
            }
        }
    }
    
    private static class HashTreeValuedHashMap<K, V> 
            extends AbstractSetValuedMap<K, V> {
        public HashTreeValuedHashMap() {
            super(new HashMap<K, TreeSet<V>>());
        }
        @Override
        protected Set<V> createCollection() {
            return new TreeSet<>();
        }
    }
}
