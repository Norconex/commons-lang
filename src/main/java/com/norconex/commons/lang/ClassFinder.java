/* Copyright 2010-2022 Norconex Inc.
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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for finding names of classes implementing an interface or class
 * in directories or JAR files.
 *
 */
@Slf4j
public final class ClassFinder {

    private static WeakReference<Cache> refCache;

    private ClassFinder() {
    }

    /**
     * Finds the names of all subtypes of the super class,
     * scanning the roots of this class classpath.
     * This method is null-safe.  If no classes are found,
     * an empty list will be returned.
     * @param superClass the class from which to find subtypes
     * @return list of class names
     * @param <T> super type
     * @since 1.4.0
     */
    public static <T> List<Class<? extends T>> findSubTypes(
            Class<T> superClass) {
        return findSubTypes(superClass, null);
    }

    /**
     * Finds the names of all subtypes of the super class,
     * scanning the roots of this class classpath.
     * This method is null-safe.  If no classes are found,
     * an empty list will be returned.
     * @param superClass the class from which to find subtypes
     * @param predicate filter to keep classes testing <code>true</code>
     * @return list of class names
     * @param <T> super type
     * @since 2.0.0
     */
    public static <T> List<Class<? extends T>> findSubTypes(
            Class<T> superClass, Predicate<String> predicate) {
        return findSubTypes(ClassFinder.class.getClassLoader(),
                cache().classes, superClass, predicate);
    }

    /**
     * Finds the names of all subtypes of the super class in list
     * of {@link File} supplied.
     * This method is null-safe.  If no classes are found,
     * an empty list will be returned.
     * @param files directories and/or JARs to scan for classes
     * @param superClass the class from which to find subtypes
     * @return list of class names
     * @param <T> super type
     * @since 1.4.0
     */
    public static <T> List<Class<? extends T>> findSubTypes(
            List<File> files, Class<T> superClass) {
        return findSubTypes(files, superClass, null);
    }

    /**
     * Finds the names of all subtypes of the super class in list
     * of {@link File} supplied.
     * This method is null-safe.  If no classes are found,
     * an empty list will be returned.
     * @param files directories and/or JARs to scan for classes
     * @param superClass the class from which to find subtypes
     * @param accept filter to keep classes testing <code>true</code>
     * @return list of class names
     * @param <T> super type
     * @since 2.0.0
     */
    public static <T> List<Class<? extends T>> findSubTypes(
            List<File> files, Class<T> superClass, Predicate<String> accept) {
        List<Class<? extends T>> classes = new ArrayList<>();
        if (superClass == null || files == null) {
            return classes;
        }
        for (File file : files) {
            classes.addAll(findSubTypes(file, superClass, accept));
        }
        return classes;
    }

    /**
     * Finds the names of all subtypes of the super class for the
     * supplied {@link File}.
     * This method is null-safe.  If no classes are found,
     * an empty list will be returned.
     * If the file is null or does not exists, or if it is not a JAR or
     * directory, an empty string list will be returned.
     * @param file directory or JAR to scan for classes
     * @param superClass the class from which to find subtypes
     * @return list of class names
     * @param <T> super type
     * @since 1.4.0
     */
    public static <T> List<Class<? extends T>> findSubTypes(
            File file, Class<T> superClass) {
        return findSubTypes(file, superClass, null);
    }

    /**
     * Finds the names of all subtypes of the super class for the
     * supplied {@link File}.
     * This method is null-safe.  If no classes are found,
     * an empty list will be returned.
     * If the file is null or does not exists, or if it is not a JAR or
     * directory, an empty string list will be returned.
     * @param file directory or JAR to scan for classes
     * @param superClass the class from which to find subtypes
     * @param predicate filter to keep classes testing <code>true</code>
     * @return list of class names
     * @param <T> super type
     * @since 2.0.0
     */
    public static <T> List<Class<? extends T>> findSubTypes(
            File file, Class<T> superClass, Predicate<String> predicate) {
        if (superClass == null) {
            return new ArrayList<>();
        }
        if (file == null || !file.exists()) {
            LOG.warn("Trying to find implementing classes from a null or "
                    + "non-existant file: {}", file);
            return new ArrayList<>();
        }

        // since we want those associated with the given file only, we
        // do not use the cache.
        var classNames = listClasses(file);
        var loader = createClassLoader(toURL(file));
        return findSubTypes(loader, classNames, superClass, predicate);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<Class<? extends T>> findSubTypes(
            ClassLoader loader,
            Collection<String> classNames,
            Class<T> superClass,
            Predicate<String> predicate) {

        Objects.requireNonNull(loader, "'loader' must not be null.");
        Objects.requireNonNull(classNames, "'classNames' must not be null.");
        Objects.requireNonNull(superClass, "'superClass' must not be null.");

        List<Class<? extends T>> subTypes = new ArrayList<>();
        for (String className : classNames) {
            if (predicate != null && !predicate.test(className)) {
                continue;
            }
            try {
                Class<?> clazz = loader.loadClass(className);
                // load only concrete implementations
                if (!clazz.isInterface()
                        && !Modifier.isAbstract(clazz.getModifiers())
                        && superClass.isAssignableFrom(clazz)) {
                    subTypes.add((Class<? extends T>) clazz);
                }
            } catch (UnsupportedClassVersionError | ClassNotFoundException e) {
                LOG.error("Invalid class: \"{}\"", className, e);
            } catch (NoClassDefFoundError e) {
                LOG.trace("Invalid class: \"{}\"", className, e);
            }
        }
        return subTypes;
    }

    private static synchronized Cache cache() {
        Cache cache;
        if (refCache == null || refCache.get() == null) {
            Set<String> classes = new HashSet<>();
            for (File file : Arrays.stream(SystemUtils.JAVA_CLASS_PATH.split(
                    File.pathSeparator)).distinct().map(File::new).toList()) {
                var listClasses = listClasses(file);
                if (!listClasses.isEmpty()) {
                    classes.addAll(listClasses);
                }
            }
            cache = new Cache(classes);
            refCache = new WeakReference<>(cache);
        } else {
            cache = refCache.get();
        }
        return cache;
    }

    private static ClassLoader createClassLoader(URL... urls) {
        return new URLClassLoader(urls, ClassFinder.class.getClassLoader());
    }

    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            LOG.error("Invalid classpath URL for file: {}", file, e);
            return null;
        }
    }

    private static Set<String> listClasses(File file) {
        if (file == null || !file.exists()) {
            LOG.warn("Trying to find implementing classes from a null or "
                    + "non-existant file: {}", file);
            return Collections.emptySet();
        }
        if (file.isDirectory()) {
            return listClassesInDirectory(
                    new File(file.getAbsolutePath() + File.separatorChar));
        }
        if (file.getName().endsWith(".jar")) {
            return listClassesFromJar(file);
        }
        LOG.warn("File not a JAR and not a directory.");
        return Collections.emptySet();
    }

    private static Set<String> listClassesInDirectory(File dir) {
        Set<String> classes = new HashSet<>();
        var dirPath = dir.getAbsolutePath();
        var classFiles = FileUtils.listFiles(
                dir, new String[] { "class" }, true);
        for (File classFile : classFiles) {
            var filePath = classFile.getAbsolutePath();
            var className = StringUtils.removeStart(filePath, dirPath);
            className = resolveClassName(/*loader, */ className);
            if (className != null) {
                classes.add(className);
            }
        }
        return classes;
    }

    private static Set<String> listClassesFromJar(File jarFile) {
        Set<String> classes = new HashSet<>();
        try (var jar = new JarFile(jarFile)) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var className = entry.getName();
                className = resolveClassName(className);
                if (className != null) {
                    classes.add(className);
                }
            }
        } catch (IOException e) {
            LOG.error("Could not read JAR: {}", jarFile, e);
        }
        return classes;
    }

    private static String resolveClassName(String rawName) {
        if (!rawName.endsWith(".class")
                // || rawName.contains("$")
                || rawName.endsWith("module-info.class")
                || rawName.startsWith("META-INF")
                || rawName.startsWith("com/sun/")
                || rawName.startsWith("sun/")
                || rawName.startsWith("java/")
                || rawName.startsWith("javax/")) {
            return null;
        }

        var className = rawName;
        className = className.replaceAll("[\\\\/]", ".");
        className = StringUtils.removeStart(className, ".");
        return StringUtils.removeEnd(className, ".class");
    }

    private static class Cache {
        private final Set<String> classes;

        public Cache(Set<String> classes) {
            this.classes = Collections.unmodifiableSet(classes);
        }
    }
}
