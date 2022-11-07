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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JarDuplicatesTest {

    @TempDir
    private Path tempDir;

    private JarFile file1_0;
    private JarFile file1_2;
    private JarFile file2_0;

    @BeforeEach
    void beforeEach() throws IOException {
        file1_0 = new JarFile(
                Files.createFile(tempDir.resolve("a-1.0.0.jar")).toFile());
        file1_2 = new JarFile(
                Files.createFile(tempDir.resolve("a-1.2.0.jar")).toFile());
        file2_0 = new JarFile(
                Files.createFile(tempDir.resolve("a-2.0.0.jar")).toFile());
    }


    @SuppressWarnings("deprecation")
    @Test
    void testJarDuplicates() {
        List<JarFile> expected = Arrays.asList(file2_0, file1_2, file1_0);

        assertThat(new JarDuplicates(file1_0, file2_0, file1_2).getJarFiles())
            .containsExactlyElementsOf(expected);

        assertThat(new JarDuplicates(
                Arrays.asList(file1_0, file1_2, file2_0)).getJarFiles())
            .containsExactlyElementsOf(expected);

        assertThrows(IllegalArgumentException.class,
                () -> new JarDuplicates(file1_0));

        JarDuplicates jarDups = new JarDuplicates(file1_0, file1_2, file2_0);

        assertThat(jarDups.getBaseName()).isEqualTo("a");
        assertThat(jarDups.getLatestVersion()).isEqualTo(jarDups.getGreatest());
        assertThat(jarDups.getGreatest()).isEqualTo(file2_0);
        assertThat(jarDups.getAllButGreatest()).containsExactly(
                file1_2, file1_0);
        assertThat(jarDups.hasVersionConflict()).isEqualTo(
                !jarDups.areEquivalent());
        assertThat(jarDups.areEquivalent()).isFalse();
        assertThat(jarDups.get(tempDir.resolve(
                "a-1.2.0.jar").toFile()).get()).isEqualTo(file1_2);

        assertThat(new JarDuplicates(file1_0, file1_2, file2_0).contains(
                tempDir.resolve("a-1.2.0.jar").toFile())).isTrue();
    }
}
