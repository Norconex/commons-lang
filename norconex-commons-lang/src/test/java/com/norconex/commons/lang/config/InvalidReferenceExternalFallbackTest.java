/* Copyright 2020 Norconex Inc.
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
package com.norconex.commons.lang.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InvalidReferenceExternalFallbackTest {

    @TempDir
    public Path tempDir;

    @Test
    public void testPropertiesFallbackResolution() throws IOException {
        String expected = "propertyValue";

        System.setProperty("propertyTest", expected);

        String source = "${PROPERTY_TEST}";
        Path testFile = tempDir.resolve("testProp.txt");
        FileUtils.write(testFile.toFile(), source, StandardCharsets.UTF_8);

        String target = new ConfigurationLoader().loadString(testFile);

        Assertions.assertEquals(expected, target);

        System.clearProperty("propertyTest");
    }


    // Disabling since the JAVA_HOME is system dependent so could generate
    // a false-fail.
    @Disabled
    @Test
    public void testEnvFallbackResolution() throws IOException {
        String source = "${javaHome}";
        Path testFile = tempDir.resolve("testEnv.txt");
        FileUtils.write(testFile.toFile(), source, StandardCharsets.UTF_8);

        String target = new ConfigurationLoader().loadString(testFile);

        Assertions.assertNotEquals(source, target);
        Assertions.assertTrue(target.contains("java"));
    }
}
