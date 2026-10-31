/* Copyright 2026 Norconex Inc.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassIndexProcessorTest {

    @TempDir
    Path tmp;

    @Test
    void indexListsEveryConcreteClassOfAnAnnotatedPackage() throws IOException {
        var out = compile(
                "@com.norconex.commons.lang.ClassIndex\npackage demo;\n",
                "Concrete.java",
                "package demo;\npublic class Concrete implements Runnable {\n"
                        + "  public void run() {\n    class Local {}\n"
                        + "    new Object() {};\n  }\n"
                        + "  public static class Nested {}\n}\n",
                "Base.java", "package demo;\npublic abstract class Base {}\n",
                "Api.java", "package demo;\npublic interface Api {}\n",
                "Kind.java", "package demo;\npublic enum Kind { A, B }\n");

        // Anonymous and local classes are concrete too, so a scan finds them and so must the index.
        assertThat(names(out)).containsExactly(
                "demo.Concrete", "demo.Concrete$1", "demo.Concrete$1Local",
                "demo.Concrete$Nested", "demo.Kind");
    }

    @Test
    void noIndexWhenNoPackageAsksForOne() throws IOException {
        var out = compile(
                "package demo;\n",
                "Concrete.java", "package demo;\npublic class Concrete {}\n");

        assertThat(out.resolve(ClassFinder.INDEX_RESOURCE)).doesNotExist();
    }

    /** Compiles the sources in-process with the processor, and returns the output folder. */
    private Path compile(String packageInfo, String... fileNamesAndSources)
            throws IOException {
        var src = tmp.resolve("src/demo");
        Files.createDirectories(src);
        Files.writeString(src.resolve("package-info.java"), packageInfo);
        for (int i = 0; i < fileNamesAndSources.length; i += 2) {
            Files.writeString(src.resolve(fileNamesAndSources[i]),
                    fileNamesAndSources[i + 1]);
        }
        var out = tmp.resolve("classes");
        Files.createDirectories(out);

        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(
                null, null, StandardCharsets.UTF_8)) {
            List<java.io.File> sources;
            try (var listed = Files.list(src)) {
                sources = listed.map(Path::toFile).toList();
            }
            var units = files.getJavaFileObjectsFromFiles(sources);
            // The annotation has to be on the compile classpath; this is the
            // location of the class that defines it.
            var classpath = Path.of(ClassIndex.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toString();
            var task = compiler.getTask(null, files, null,
                    List.of("-d", out.toString(), "-classpath", classpath),
                    null, units);
            task.setProcessors(List.of(new ClassIndexProcessor()));
            assertThat(task.call()).as("compilation").isTrue();
        } catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
        return out;
    }

    /** The class names listed in the index, without its comment lines. */
    private static List<String> names(Path out) throws IOException {
        return Files.readAllLines(out.resolve(ClassFinder.INDEX_RESOURCE))
                .stream()
                .filter(line -> !line.startsWith("#") && !line.isBlank())
                .toList();
    }
}
