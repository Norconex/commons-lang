/* Copyright 2020-2022 Norconex Inc.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.TimeIdGenerator;

class ExtendedReferenceInsertionEventHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void testPrecedence() throws IOException {
        var baseVarName = "precedenceTest";

        // precedence order
        var valueSystemProperty = "valueSystemProperty";
        var valuePropertiesFile = "valuePropertiesFile";
        var valueVariablesFile = "valueVariablesFile";

        String varName;
        String before;
        String after;

        // Test with: System property + Prop. file + Var. file
        varName = baseVarName + 1;
        System.setProperty(varName, valueSystemProperty);
        before = "${" + varName + "}";
        after = resolveVariable(before,
                varName + "=" + valuePropertiesFile,
                varName + "=" + valueVariablesFile);
        Assertions.assertEquals(valueSystemProperty, after);
        System.clearProperty(varName);

        // Test with: Prop. file + Var. file
        varName = baseVarName + 2;
        before = "${" + varName + "}";
        after = resolveVariable(before,
                varName + "=" + valuePropertiesFile,
                varName + "=" + valueVariablesFile);
        System.clearProperty(varName);
    }

    @Test
    void testWithSystemProperty() throws IOException {

        var expected = "propertyValue";
        System.setProperty("propertyTest", expected);

        var before = "${PROPERTY_TEST}";
        var after = resolveVariable(before);

        Assertions.assertEquals(expected, after);
        System.clearProperty("propertyTest");
    }

    @Test
    void testWithDefaultValueInReference() throws IOException {

        var expectedDefault = "defaultValue";
        var expectedResolved = "resolvedValue";
        var before = "${testVar|'" + expectedDefault + "'}";

        // test getting default value (variable not set)
        var afterDefault = resolveVariable(before);
        Assertions.assertEquals(expectedDefault, afterDefault);

        // test getting property value (variable set)
        System.setProperty("testVar", expectedResolved);
        var afterResolved = resolveVariable(before);
        Assertions.assertEquals(expectedResolved, afterResolved);
        System.clearProperty("testVar");
    }

    // Disabling since the JAVA_HOME is system dependent so could generate
    // a false-fail.
    @Disabled
    @Test
    void testWithEnvironmentVariable() throws IOException {
        var before = "${javaHome}";
        var after = resolveVariable(before);
        Assertions.assertNotEquals(before, after);
        Assertions.assertTrue(after.contains("java"));
    }

    private String resolveVariable(String configFileContent)
            throws IOException {
        return resolveVariable(configFileContent, null, null);
    }

    private String resolveVariable(String configFileContent,
            String propertiesFileContent, String variablesFileContent)
            throws IOException {
        var fileBaseName = "text-" + TimeIdGenerator.next();
        var testFile = writeFile(fileBaseName + ".txt", configFileContent);
        writeFile(fileBaseName + ".properties", propertiesFileContent);
        writeFile(fileBaseName + ".variables", variablesFileContent);
        return ConfigurationLoader.builder().build().toString(testFile);
    }

    private Path writeFile(String fileName, String content) throws IOException {
        if (StringUtils.isNotBlank(content)) {
            var file = tempDir.resolve(fileName);
            FileUtils.write(file.toFile(), content, UTF_8);
            return file;
        }
        return null;
    }
}
