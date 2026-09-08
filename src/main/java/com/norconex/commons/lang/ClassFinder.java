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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
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
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Utility class for finding names of classes implementing an interface or class
 * in directories or JAR files.
 * </p>
 *
 * <h2>Class index</h2>
 * <p>
 * Enumerating every entry of every JAR on a large classpath is slow, and it is
 * wasted work for the many JARs that hold nothing of interest. A JAR can
 * therefore ship a <b>class index</b>: a resource named
 * {@value #INDEX_RESOURCE} listing the fully qualified name of each of its
 * classes, one per line (blank lines and lines starting with
 * <code>#</code> are ignored). When a JAR carries one, its index is read
 * instead of the archive being opened and walked.
 * </p>
 *
 * <h2>Scan modes</h2>
 * <p>
 * The system property {@value #PROPERTY_SCAN_MODE} selects how the classpath
 * is treated when scanning the roots of this class' classpath:
 * </p>
 * <ul>
 *   <li>
 *     <code>all</code> (default) &mdash; every classpath entry is read: index
 *     first where one exists, otherwise the JAR or directory is walked. Same
 *     coverage as previous versions.
 *   </li>
 *   <li>
 *     <code>indexed</code> &mdash; JARs without an index are skipped.
 *     Directories are still walked, so working from compiled output in an IDE
 *     or from <code>target/classes</code> behaves normally. Use this to avoid
 *     paying for third-party JARs, and put any JAR that must still be scanned
 *     in the extension directory below.
 *   </li>
 * </ul>
 *
 * <h2>Extension directory</h2>
 * <p>
 * The directory named by {@value #PROPERTY_EXT_DIR} (default
 * <code>{@value #DEFAULT_EXT_DIR}</code>, relative to the working directory)
 * is always scanned when it exists, whatever the scan mode, and whether or not
 * it is on the classpath. It is the place to drop JARs holding classes that
 * have to be discovered by scanning.
 * </p>
 */
@Slf4j
public final class ClassFinder {

    /**
     * Resource path of a JAR's class index, listing its fully qualified class
     * names one per line.
     */
    public static final String INDEX_RESOURCE =
            "META-INF/norconex/classes.idx";

    /**
     * System property selecting the classpath scan mode: <code>all</code>
     * (default) or <code>indexed</code>.
     */
    public static final String PROPERTY_SCAN_MODE =
            "norconex.classfinder.scanMode";

    /**
     * System property naming the always-scanned extension directory.
     */
    public static final String PROPERTY_EXT_DIR =
            "norconex.classfinder.extDir";

    /** Default extension directory, relative to the working directory. */
    public static final String DEFAULT_EXT_DIR = "ext";

    /** Scan mode reading every classpath entry. */
    public static final String SCAN_MODE_ALL = "all";

    /** Scan mode skipping JARs that carry no class index. */
    public static final String SCAN_MODE_INDEXED = "indexed";

    // Safety limits when scanning JARs to avoid zip-bomb / resource exhaustion
    private static final int MAX_JAR_ENTRIES = 10_000;
    private static final long MAX_JAR_SIZE = 200L * 1024L * 1024L; // 200MB
    private static final int MAX_ENTRY_NAME_LENGTH = 2_048;

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
            cache = new Cache(scanClasspath());
            refCache = new WeakReference<>(cache);
        } else {
            cache = refCache.get();
        }
        return cache;
    }

    /**
     * Discards the cached classpath class names, so the next lookup rebuilds
     * it. The cache is otherwise held weakly and rebuilt only when collected.
     * @since 3.1.0
     */
    public static synchronized void clearCache() {
        refCache = null;
    }

    private static Set<String> scanClasspath() {
        return scanSources(
                ClassFinder.class.getClassLoader(),
                Arrays.stream(SystemUtils.JAVA_CLASS_PATH.split(
                        File.pathSeparator))
                        .distinct()
                        .map(File::new)
                        .toList(),
                new File(StringUtils.defaultIfBlank(
                        System.getProperty(PROPERTY_EXT_DIR),
                        DEFAULT_EXT_DIR)),
                SCAN_MODE_INDEXED.equalsIgnoreCase(StringUtils.trimToEmpty(
                        System.getProperty(PROPERTY_SCAN_MODE))));
    }

    /**
     * Collects the class names offered by the supplied sources. Kept
     * package-private, and free of system-property and classpath lookups, so
     * the scanning rules can be exercised in isolation.
     * @param loader class loader whose class indexes are read
     * @param classpathEntries JARs and directories forming the classpath
     * @param extDir always-scanned extension directory, need not exist
     * @param indexedOnly skip classpath JARs carrying no class index
     * @return discovered fully qualified class names
     */
    static Set<String> scanSources(
            ClassLoader loader,
            List<File> classpathEntries,
            File extDir,
            boolean indexedOnly) {

        Set<String> classes = new HashSet<>();

        // Indexed JARs first: reading a listing beats opening the archive.
        // Remember which files they were so they are not then walked as well.
        var indexedFiles = readClassIndexes(loader, classes);

        for (File file : classpathEntries) {
            if (indexedFiles.contains(canonical(file))) {
                continue;
            }
            // Skipping unindexed JARs is the point of "indexed" mode.
            // Directories stay in so compiled output still resolves.
            if (indexedOnly && file.isFile()) {
                LOG.debug("Skipping unindexed JAR in \"{}\" scan mode: {}",
                        SCAN_MODE_INDEXED, file);
                continue;
            }
            classes.addAll(listClasses(file));
        }

        classes.addAll(scanExtensionDir(extDir));
        return classes;
    }

    /**
     * Reads every class index reachable from the loader, adding the names
     * each one lists.
     * @param loader class loader to look index resources up on
     * @param classes set to add discovered class names to
     * @return canonical paths of the files whose index was read
     */
    private static Set<String> readClassIndexes(
            ClassLoader loader, Set<String> classes) {
        Set<String> indexedFiles = new HashSet<>();
        try {
            var urls = loader.getResources(INDEX_RESOURCE);
            while (urls.hasMoreElements()) {
                var url = urls.nextElement();
                var count = 0;
                // Caching is off deliberately: for a jar: URL the JVM would
                // otherwise hold the archive open for the life of the
                // process, and the index is read once anyway.
                var conn = url.openConnection();
                conn.setUseCaches(false);
                try (var reader = new BufferedReader(new InputStreamReader(
                        conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        var name = normalizeIndexEntry(line);
                        if (name != null) {
                            classes.add(name);
                            count++;
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("Could not read class index: {}", url, e);
                    continue;
                }
                var file = indexSourceFile(url);
                if (file != null) {
                    indexedFiles.add(file);
                }
                LOG.debug("Read {} class names from index: {}", count, url);
            }
        } catch (IOException e) {
            LOG.warn("Could not look up class indexes on the classpath.", e);
        }
        return indexedFiles;
    }

    /**
     * Turns one index line into a class name, or <code>null</code> for a
     * blank line or a comment. Entries may be written either as class names
     * (<code>com.example.Foo</code>) or as archive paths
     * (<code>com/example/Foo.class</code>), since emitting the raw path is
     * what a build tool can do most easily.
     */
    private static String normalizeIndexEntry(String line) {
        var name = StringUtils.trimToEmpty(line);
        if (name.isEmpty() || name.startsWith("#")) {
            return null;
        }
        name = name.replace('/', '.').replace('\\', '.');
        name = Strings.CS.removeEnd(name, ".class");
        name = StringUtils.strip(name, ".");
        return name.isEmpty() ? null : name;
    }

    /**
     * Resolves the canonical path of the archive or directory an index came
     * from, so that entry is not scanned again. Returns <code>null</code> when
     * the source cannot be established, which only means the entry may also
     * be walked.
     */
    private static String indexSourceFile(URL url) {
        try {
            var spec = url.toString();
            if (spec.startsWith("jar:")) {
                var sep = spec.indexOf("!/");
                if (sep < 0) {
                    return null;
                }
                return canonical(new File(
                        URI.create(spec.substring(4, sep))));
            }
            if ("file".equals(url.getProtocol())) {
                // A plain directory on the classpath: the index sits inside
                // it, so step back up out of META-INF/norconex.
                var indexFile = new File(url.toURI());
                var root = indexFile.getParentFile() // norconex
                        .getParentFile() // META-INF
                        .getParentFile(); // classpath root
                return canonical(root);
            }
        } catch (Exception e) {
            LOG.debug("Could not resolve the source of class index: {}",
                    url, e);
        }
        return null;
    }

    private static Set<String> scanExtensionDir(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return Collections.emptySet();
        }
        Set<String> classes = new HashSet<>();
        // Classes may sit loose in the directory as well as in JARs.
        classes.addAll(listClassesInDirectory(dir));
        var jars = dir.listFiles(
                f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                classes.addAll(listClassesFromJar(jar));
            }
        }
        LOG.debug("Found {} classes in extension directory: {}",
                classes.size(), dir);
        return classes;
    }

    private static String canonical(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
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
        // Classpaths routinely carry entries that are neither, which is
        // normal and holds no classes either way.
        LOG.debug("Classpath entry is not a JAR and not a directory: {}", file);
        return Collections.emptySet();
    }

    private static Set<String> listClassesInDirectory(File dir) {
        Set<String> classes = new HashSet<>();
        var dirPath = dir.getAbsolutePath();
        var classFiles = FileUtils.listFiles(
                dir, new String[] { "class" }, true);
        for (File classFile : classFiles) {
            var filePath = classFile.getAbsolutePath();
            var className = Strings.CS.removeStart(filePath, dirPath);
            className = resolveClassName(/*loader, */ className);
            if (className != null) {
                classes.add(className);
            }
        }
        return classes;
    }

    private static Set<String> listClassesFromJar(File jarFile) {
        Set<String> classes = new HashSet<>();
        // Basic safety checks before opening/iterating the archive.
        // Hitting these limits is an expected outcome for a few large
        // third-party archives, not a misconfiguration, so it is logged at
        // debug: a JAR holding types meant to be discovered by scanning
        // belongs in the extension directory or should ship a class index.
        if (jarFile.length() > MAX_JAR_SIZE) {
            LOG.debug("Skipping large JAR file: {} ({} bytes)", jarFile,
                    jarFile.length());
            return classes;
        }
        try (var jar = new JarFile(jarFile)) {
            var declaredEntries = jar.size();
            if (declaredEntries > MAX_JAR_ENTRIES) {
                LOG.debug("Skipping JAR with too many entries: {} ({} entries)",
                        jarFile, declaredEntries);
                return classes;
            }
            var entries = jar.entries();
            var processed = 0;
            while (entries.hasMoreElements()) {
                if (++processed > MAX_JAR_ENTRIES) {
                    LOG.warn("Too many entries while reading JAR: {} - "
                            + "stopping after {}", jarFile, processed);
                    break;
                }
                var entry = entries.nextElement();
                var entryName = entry.getName();
                // Basic sanity checks to avoid path traversal or extremely
                // long names that could be used in attacks.
                if (entryName == null
                        || entryName.length() > MAX_ENTRY_NAME_LENGTH) {
                    LOG.debug("Skipping suspicious entry name in {}: {}",
                            jarFile, entryName);
                    continue;
                }
                if (entryName.startsWith("/")
                        || entryName.contains("..")
                        || entryName.contains(":\\")) {
                    LOG.debug("Skipping suspicious entry path in {}: {}",
                            jarFile, entryName);
                    continue;
                }
                var className = resolveClassName(entryName);
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
        className = Strings.CS.removeStart(className, ".");
        return Strings.CS.removeEnd(className, ".class");
    }

    private static class Cache {
        private final Set<String> classes;

        public Cache(Set<String> classes) {
            this.classes = Collections.unmodifiableSet(classes);
        }
    }
}
