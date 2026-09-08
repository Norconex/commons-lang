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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.convert.CharsetConverter;
import com.norconex.commons.lang.convert.DurationConverter;
import com.norconex.commons.lang.convert.Converter;
import com.norconex.commons.lang.convert.LocaleConverter;

/**
 * Class-scanning tests
 */
class ClassFinderTest {

    @Test
    void testFindSubTypesClass() {
        List<Class<? extends Converter>> types =
                ClassFinder.findSubTypes(Converter.class);
        assertThat(types).contains(
                DurationConverter.class,
                CharsetConverter.class,
                LocaleConverter.class);
    }

    @Test
    void testSubTypesClassPredicate() {
        String toFind = ".DurationConverter";
        List<Class<? extends Converter>> types = ClassFinder.findSubTypes(
                Converter.class, s -> s.endsWith(toFind));
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isEqualTo(DurationConverter.class);
    }

    @Test
    void testFindSubTypesListClass() {
        List<Class<? extends Converter>> types = ClassFinder.findSubTypes(
                Arrays.asList(new File("target/classes")), Converter.class);
        assertThat(types).contains(
                DurationConverter.class,
                CharsetConverter.class,
                LocaleConverter.class);
    }

    @Test
    void testFindSubTypesListClassPredicate() {
        String toFind = ".DurationConverter";
        List<Class<? extends Converter>> types = ClassFinder.findSubTypes(
                Arrays.asList(new File("target/classes")),
                Converter.class,
                s -> s.endsWith(toFind));
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isEqualTo(DurationConverter.class);

        assertThat(ClassFinder.findSubTypes(
                (List<File>) null, Converter.class)).isEmpty();
        assertThat(ClassFinder.findSubTypes(
                Arrays.asList(new File("target/classes")), null)).isEmpty();
    }

    @Test
    void testFindSubTypesFileClass() {
        List<Class<? extends Converter>> types = ClassFinder.findSubTypes(
                new File("target/classes"), Converter.class);
        assertThat(types).contains(
                DurationConverter.class,
                CharsetConverter.class,
                LocaleConverter.class);
    }

    @Test
    void testFindSubTypesFileClassPredicate() {
        String toFind = ".DurationConverter";
        List<Class<? extends Converter>> types = ClassFinder.findSubTypes(
                new File("target/classes"),
                Converter.class,
                s -> s.endsWith(toFind));
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isEqualTo(DurationConverter.class);

        assertThat(ClassFinder.findSubTypes(
                (File) null, Converter.class)).isEmpty();
        assertThat(ClassFinder.findSubTypes(
                new File("target/classes"), null)).isEmpty();
    }

    // --- Class index -----------------------------------------------------

    // A JAR shipping an index has its names read from that listing, and the
    // archive itself is then left alone.
    @Test
    void testIndexIsReadInsteadOfScanningTheJar(@TempDir Path tempDir)
            throws Exception {
        var jar = jarWithIndex(tempDir.resolve("indexed.jar"),
                List.of("com.example.FromIndex", "com.example.AlsoFromIndex"),
                Map.of("com/example/OnlyInTheArchive.class", "irrelevant"));

        Set<String> names;
        try (var loader = loaderFor(jar)) {
            names = ClassFinder.scanSources(loader, List.of(jar), null, false);
        }

        assertThat(names).contains(
                "com.example.FromIndex", "com.example.AlsoFromIndex");
        // The index is authoritative for that JAR, so a class present in the
        // archive but absent from the index is not reported.
        assertThat(names).doesNotContain("com.example.OnlyInTheArchive");
    }

    // Blank lines and comments are there so a generated index stays readable.
    @Test
    void testIndexIgnoresBlankLinesAndComments(@TempDir Path tempDir)
            throws Exception {
        var jar = tempDir.resolve("commented.jar").toFile();
        writeJar(jar, Map.of(
                ClassFinder.INDEX_RESOURCE,
                """
                # generated, do not edit

                com.example.Kept

                #com.example.Commented
                """));

        Set<String> names;
        try (var loader = loaderFor(jar)) {
            names = ClassFinder.scanSources(loader, List.of(jar), null, false);
        }

        assertThat(names).containsExactly("com.example.Kept");
    }

    // Entries may be written as class names or as raw archive paths, since
    // emitting the path is what a build tool does most easily.
    @Test
    void testIndexAcceptsPathStyleEntries(@TempDir Path tempDir)
            throws Exception {
        var jar = tempDir.resolve("paths.jar").toFile();
        writeJar(jar, Map.of(
                ClassFinder.INDEX_RESOURCE,
                """
                com/example/UnixPath.class
                com\\example\\WindowsPath.class
                com.example.PlainName
                """));

        Set<String> names;
        try (var loader = loaderFor(jar)) {
            names = ClassFinder.scanSources(loader, List.of(jar), null, false);
        }

        assertThat(names).containsExactlyInAnyOrder(
                "com.example.UnixPath",
                "com.example.WindowsPath",
                "com.example.PlainName");
    }

    // --- Scan modes ------------------------------------------------------

    @Test
    void testIndexedModeSkipsUnindexedJars(@TempDir Path tempDir)
            throws Exception {
        var indexed = jarWithIndex(tempDir.resolve("indexed.jar"),
                List.of("com.example.Indexed"), Map.of());
        var plain = tempDir.resolve("plain.jar").toFile();
        writeJar(plain, Map.of("com/example/Plain.class", "irrelevant"));

        var entries = List.of(indexed, plain);
        Set<String> all;
        Set<String> indexedOnly;
        try (var loader = loaderFor(indexed, plain)) {
            all = ClassFinder.scanSources(loader, entries, null, false);
            indexedOnly = ClassFinder.scanSources(loader, entries, null, true);
        }

        // "all" reads both: the index for one, the archive for the other.
        assertThat(all).contains("com.example.Indexed", "com.example.Plain");

        // "indexed" keeps the indexed JAR and drops the unindexed one.
        assertThat(indexedOnly)
                .contains("com.example.Indexed")
                .doesNotContain("com.example.Plain");
    }

    // Directories are compiled output, so they stay in even in indexed mode
    // or working from an IDE would stop resolving.
    @Test
    void testIndexedModeStillScansDirectories() {
        var names = ClassFinder.scanSources(
                ClassFinder.class.getClassLoader(),
                List.of(new File("target/classes")),
                null,
                true);
        assertThat(names).contains(DurationConverter.class.getName());
    }

    // --- Extension directory ---------------------------------------------

    // The extension directory is scanned whatever the mode, and without
    // being a classpath entry, since it is where user JARs go.
    @Test
    void testExtensionDirIsAlwaysScanned(@TempDir Path tempDir)
            throws Exception {
        var extDir = Files.createDirectory(tempDir.resolve("ext")).toFile();
        var extJar = new File(extDir, "custom.jar");
        writeJar(extJar, Map.of("com/example/Extension.class", "irrelevant"));

        // Nothing on the classpath, indexed mode on: still found.
        assertThat(ClassFinder.scanSources(
                ClassFinder.class.getClassLoader(),
                List.of(), extDir, true))
                        .contains("com.example.Extension");
    }

    @Test
    void testMissingExtensionDirIsHarmless(@TempDir Path tempDir) {
        assertThat(ClassFinder.scanSources(
                ClassFinder.class.getClassLoader(),
                List.of(),
                tempDir.resolve("no-such-dir").toFile(),
                false)).isEmpty();
    }

    @Test
    void testClearCacheForcesARescan() {
        ClassFinder.clearCache();
        assertThat(ClassFinder.findSubTypes(Converter.class))
                .contains(DurationConverter.class);
        ClassFinder.clearCache();
        assertThat(ClassFinder.findSubTypes(Converter.class))
                .contains(DurationConverter.class);
    }

    // --- helpers ---------------------------------------------------------

    private static File jarWithIndex(
            Path jarPath, List<String> indexedNames,
            Map<String, String> otherEntries) throws IOException {
        Map<String, String> entries = new HashMap<>(otherEntries);
        entries.put(ClassFinder.INDEX_RESOURCE,
                String.join("\n", indexedNames));
        var jar = jarPath.toFile();
        writeJar(jar, entries);
        return jar;
    }

    private static void writeJar(File jar, Map<String, String> entries)
            throws IOException {
        try (var out = new JarOutputStream(new FileOutputStream(jar))) {
            for (var entry : entries.entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    private static URLClassLoader loaderFor(File... jars) throws IOException {
        var urls = new URL[jars.length];
        for (var i = 0; i < jars.length; i++) {
            urls[i] = jars[i].toURI().toURL();
        }
        // No parent, so only these JARs answer the index lookup and the test
        // does not see indexes belonging to the real classpath.
        return new URLClassLoader(urls, null);
    }
}
