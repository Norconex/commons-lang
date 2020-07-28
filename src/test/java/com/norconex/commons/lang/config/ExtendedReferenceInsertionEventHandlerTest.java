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

import com.norconex.commons.lang.TimeIdGenerator;

public class ExtendedReferenceInsertionEventHandlerTest {

    @TempDir
    public Path tempDir;

    @Test
    public void testWithSystemProperty() throws IOException {

        String expected = "propertyValue";
        System.setProperty("propertyTest", expected);

        String before = "${PROPERTY_TEST}";
        String after = resolveVariable(before);

        Assertions.assertEquals(expected, after);
        System.clearProperty("propertyTest");
    }

    @Test
    public void testWithDefaultValueInReference() throws IOException {

        String expectedDefault = "defaultValue";
        String expectedResolved = "resolvedValue";
        String before = "${testVar|'" + expectedDefault + "'}";

        // test getting default value (variable not set)
        String afterDefault = resolveVariable(before);
        Assertions.assertEquals(expectedDefault, afterDefault);

        // test getting property value (variable set)
        System.setProperty("testVar", expectedResolved);
        String afterResolved = resolveVariable(before);
        Assertions.assertEquals(expectedResolved, afterResolved);
        System.clearProperty("testVar");
    }

    // Disabling since the JAVA_HOME is system dependent so could generate
    // a false-fail.
    @Disabled
    @Test
    public void testWithEnvironmentVariable() throws IOException {
        String before = "${javaHome}";
        String after = resolveVariable(before);
        Assertions.assertNotEquals(before, after);
        Assertions.assertTrue(after.contains("java"));
    }

    private String resolveVariable(String configFileContent)
            throws IOException {
        Path testFile = tempDir.resolve(
                "text-" + TimeIdGenerator.next() + ".txt");
        FileUtils.write(testFile.toFile(),
                configFileContent, StandardCharsets.UTF_8);
        return new ConfigurationLoader().loadString(testFile);
    }
}
