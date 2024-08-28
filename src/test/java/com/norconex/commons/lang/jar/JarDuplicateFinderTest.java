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
package com.norconex.commons.lang.jar;

import static com.norconex.commons.lang.jar.JarTestUtil.createJar;
import static com.norconex.commons.lang.jar.JarTestUtil.createSourceTargetJars;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.mock.Mocker;

class JarDuplicateFinderTest {

    @TempDir
    private Path tempDir;

    private Path sourceDir;
    private Path targetDir;

    @BeforeEach
    void beforeEach() throws IOException {
        sourceDir = Files.createDirectories(tempDir.resolve("sourceDir"));
        targetDir = Files.createDirectories(tempDir.resolve("targetDir"));

        createSourceTargetJars(sourceDir, targetDir, "noversion.jar");
        createSourceTargetJars(sourceDir, targetDir, "a-1.0.0.jar");
        createJar(sourceDir.resolve("b-1.0.0.jar"));
        createJar(sourceDir.resolve("b-1.1.0.jar"));
        createJar(targetDir.resolve("b-1.0.3.jar"));
        createJar(targetDir.resolve("b-1.0.4.jar"));
        createJar(sourceDir.resolve("c-1.0.0.jar"));
        createJar(targetDir.resolve("d-1.0.0.jar"));
    }

    @Test
    void testFindJarDuplicates() {
        JarDuplicates[] expected = {
                new JarDuplicates(
                        new JarFile(
                                sourceDir.resolve("noversion.jar").toFile()),
                        new JarFile(
                                targetDir.resolve("noversion.jar").toFile())),
                new JarDuplicates(
                        new JarFile(sourceDir.resolve("a-1.0.0.jar").toFile()),
                        new JarFile(targetDir.resolve("a-1.0.0.jar").toFile())),
                new JarDuplicates(
                        new JarFile(sourceDir.resolve("b-1.1.0.jar").toFile()),
                        new JarFile(targetDir.resolve("b-1.0.4.jar").toFile()),
                        new JarFile(targetDir.resolve("b-1.0.3.jar").toFile()),
                        new JarFile(sourceDir.resolve("b-1.0.0.jar").toFile()))
        };

        assertThat(JarDuplicateFinder.findJarDuplicates(
                Arrays.asList(sourceDir.toFile(), targetDir.toFile())))
                        .hasSize(3).containsExactlyInAnyOrder(expected);

        assertThat(JarDuplicateFinder.findJarDuplicates(
                sourceDir.toString(), targetDir.toString()))
                        .hasSize(3).containsExactlyInAnyOrder(expected);
    }

    @Test
    void testFindJarDuplicatesOf() {
        assertThat(JarDuplicateFinder.findJarDuplicatesOf(
                sourceDir.resolve("noversion.jar").toFile(),
                Arrays.asList(targetDir.toFile())))
                        .containsExactly(
                                new JarFile(targetDir.resolve("noversion.jar")
                                        .toFile()));

        assertThat(JarDuplicateFinder.findJarDuplicatesOf(
                sourceDir.resolve("b-1.1.0.jar").toFile(),
                Arrays.asList(targetDir.toFile())))
                        .containsExactly(
                                new JarFile(targetDir.resolve("b-1.0.4.jar")
                                        .toFile()),
                                new JarFile(targetDir.resolve("b-1.0.3.jar")
                                        .toFile()));
    }

    @Test
    void testMain() throws IOException {
        String out = Mocker.mockStdInOutErr(() -> JarDuplicateFinder.main(
                new String[] { sourceDir.toString(), targetDir.toString() }));
        assertThat(out)
                .contains(
                        "a:",
                        "b:",
                        "noversion:")
                .matches("(?s).*targetDir.a-1\\.0\\.0\\.jar.*")
                .matches("(?s).*sourceDir.a-1\\.0\\.0\\.jar.*")
                .matches("(?s).*sourceDir.b-1\\.1\\.0\\.jar.*")
                .matches("(?s).*targetDir.b-1\\.0\\.4\\.jar.*")
                .matches("(?s).*targetDir.b-1\\.0\\.3\\.jar.*")
                .matches("(?s).*sourceDir.b-1\\.0\\.0\\.jar.*")
                .matches("(?s).*targetDir.noversion\\.jar.*")
                .matches("(?s).*sourceDir.noversion\\.jar.*");
    }
}
