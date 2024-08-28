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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.stream.Collectors;

class JarTestUtil {

    static void assertDirOnlyHasFiles(
            Path dir, String... fileNames) throws IOException {
        assertThat(Files.list(dir)
                .map(p -> p.getFileName().toString()
                        .replaceFirst("^(.*\\.bak)-\\d+$", "$1"))
                .collect(Collectors.toList()))
                        .containsExactlyInAnyOrder(fileNames);
    }

    static void assertDirContainsFiles(
            Path dir, String... fileNames) throws IOException {
        assertThat(Files.list(dir)
                .map(p -> p.getFileName().toString()
                        .replaceFirst("^(.*\\.bak)-\\d+$", "$1"))
                .collect(Collectors.toList()))
                        .contains(fileNames);
    }

    static void createSourceTargetJars(
            Path sourceDir, Path targetDir, String fileName)
            throws IOException {
        createSourceTargetJars(
                sourceDir.resolve(fileName), targetDir.resolve(fileName));
    }

    static void createSourceTargetJars(
            Path sourceFile, Path targetFile) throws IOException {
        FileTime lastModified = null;
        if (sourceFile != null) {
            lastModified = Files.getLastModifiedTime(
                    Files.createFile(sourceFile));
        }
        if (targetFile != null) {
            Path file = Files.createFile(targetFile);
            if (lastModified != null) {
                Files.setLastModifiedTime(file, lastModified);
            }
        }
    }

    static FileTime createJar(Path file) throws IOException {
        return Files.getLastModifiedTime(Files.createFile(file));
    }

    static void createJar(Path file, FileTime time) throws IOException {
        Files.createFile(file);
        if (time != null) {
            Files.setLastModifiedTime(file, time);
        }
    }
}
