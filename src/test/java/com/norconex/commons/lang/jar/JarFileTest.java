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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JarFileTest {

    @TempDir
    private Path tempDir;

    private JarFile file1;
    private JarFile file2;

    @BeforeEach
    void beforeEach() throws IOException {
        file1 = new JarFile(
                Files.createFile(tempDir.resolve("a-1.0.0.jar")).toFile());
        file2 = new JarFile(
                Files.createFile(tempDir.resolve("a-2.0.0.jar")).toFile());
    }

    @Test
    void testJarFile() throws IOException {
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> new JarFile(tempDir.resolve("doesntExist.jar").toFile()));

        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> new JarFile(Files.createFile(
                        tempDir.resolve("b-1.0.0.bad")).toFile()));
    }

    @Test
    void testToFile() {
        assertThat(file1.toFile()).isEqualTo(
                tempDir.resolve("a-1.0.0.jar").toFile());
    }

    @SuppressWarnings("deprecation")
    @Test
    void testGetPath() {
        assertThat(file1.getPath()).isEqualTo(
                tempDir.resolve("a-1.0.0.jar").toFile());
    }

    @Test
    void testGetFullName() {
        assertThat(file1.getFullName()).isEqualTo("a-1.0.0.jar");
    }

    @Test
    void testGetBaseName() {
        assertThat(file1.getBaseName()).isEqualTo("a");
    }

    @Test
    void testGetVersion() {
        assertThat(file1.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void testGetLastModified() {
        assertThat(file1.getLastModified()).isBeforeOrEqualTo(new Date());
    }

    @Test
    void testIsVersionGreaterThan() {
        assertThat(file1.isVersionGreaterThan(file2)).isFalse();
    }

    @Test
    void testToString() {
        assertThat(file1.toString()).hasToString(
                tempDir.resolve("a-1.0.0.jar").toFile().toString());
    }

    @SuppressWarnings("deprecation")
    @Test
    void testIsSameVersion() {
        assertThat(file1.isSameVersion(file2)).isFalse();
        assertThat(file1.isSameVersion(null)).isFalse();
    }

    @Test
    void testIsDuplicateOf() {
        assertThat(file1.isDuplicateOf(file2)).isTrue();
        assertThat(file1.isDuplicateOf(null)).isFalse();
    }

    @SuppressWarnings("deprecation")
    @Test
    void testIsSameVersionAndTime() {
        assertThat(file1.isSameVersionAndTime(file2)).isFalse();
    }

    @Test
    void testIsEquivalentTo() {
        assertThat(file1.isEquivalentTo(file2)).isFalse();
    }

    @Test
    void testIsGreaterThan() {
        assertThat(file1.isGreaterThan(file2)).isFalse();
    }

    @Test
    void testIsGreaterOrEquivalentTo() {
        assertThat(file1.isGreaterOrEquivalentTo(file2)).isFalse();
    }

    @Test
    void testIsLowerThan() {
        assertThat(file1.isLowerThan(file2)).isTrue();
    }

    @Test
    void testIsLowerOrEquivalentTo() {
        assertThat(file1.isLowerOrEquivalentTo(file2)).isTrue();
    }

    @Test
    void testCompareTo() {
        assertThat(file1).isLessThan(file2);
        assertThat(file2).isGreaterThan(file1);
        assertThat(file1).isGreaterThan(null);
        assertThat(file1).isEqualByComparingTo(file1);
    }

    @Test
    void testToJarFiles() {
        assertThat(JarFile.toJarFiles(
                tempDir.resolve("a-1.0.0.jar"),
                tempDir.resolve("a-2.0.0.jar")))
            .containsExactly(file1, file2);

        assertThat(JarFile.toJarFiles(Arrays.asList(
                tempDir.resolve("a-1.0.0.jar"),
                tempDir.resolve("a-2.0.0.jar"))))
            .containsExactly(file1, file2);

        assertThat(JarFile.toJarFiles(
                tempDir.resolve("a-1.0.0.jar").toFile(),
                tempDir.resolve("a-2.0.0.jar").toFile()))
            .containsExactly(file1, file2);
    }
}
