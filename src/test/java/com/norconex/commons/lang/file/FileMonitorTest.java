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
package com.norconex.commons.lang.file;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.Sleeper;

@Disabled
class FileMonitorTest {

    @TempDir
    Path tempDir;

    @Test
    void testFileMonitor() throws IOException {
        StringBuilder b = new StringBuilder();
        FileChangeListener listener = f -> {
            try {
                b.append(FileUtils.readFileToString(f, UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        File file = tempDir.resolve("file.txt").toFile();
        FileUtils.touch(file);
        FileMonitor.getInstance().addFileChangeListener(
                listener,  file.getAbsolutePath(), 50L);

        FileUtils.writeStringToFile(file, "one", UTF_8);
        Sleeper.sleepMillis(100);
        FileUtils.writeStringToFile(file, "two", UTF_8);
        Sleeper.sleepMillis(100);
        FileUtils.writeStringToFile(file, "three", UTF_8);
        Sleeper.sleepMillis(500);

        FileMonitor.getInstance().removeFileChangeListener(
                listener, file.getAbsolutePath());

        assertThat(b).contains("onetwothree");
    }
}
