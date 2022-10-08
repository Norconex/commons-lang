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
package com.norconex.commons.lang.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

final class VariablesFileResolver {

    private static final String EXTENSION_PROPERTIES = ".properties";
    private static final String EXTENSION_VARIABLES = ".variables";

    /**
     * Constructor.
     */
    private VariablesFileResolver() {}

    static Map<String, String> resolve(String fullDir, String baseFileName) {
        return resolve(getVariablesFile(fullDir, baseFileName));
    }
    static Map<String, String> resolve(Path varFile) {
        if (varFile == null) {
            return Collections.emptyMap();
        }
        try {
            if (varFile.toString().endsWith(EXTENSION_PROPERTIES)) {
                return fromProperties(varFile);
            }
            // Defaults to .variables format
            return fromVariables(varFile);
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Cannot load variables from file: " + varFile, e);
        }
    }


    private static Map<String, String> fromProperties(Path varFile)
            throws IOException {
        Map<String, String> map = new HashMap<>();
        var props = new Properties();
        try (Reader r = Files.newBufferedReader(varFile)) {
            props.load(r);
        }
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
        return map;
    }
    private static Map<String, String> fromVariables(Path varFile)
            throws IOException {
        Map<String, String> map = new HashMap<>();
        for (String line : Files.readAllLines(varFile)) {
            if (line.contains("=")) {
                var key = StringUtils.substringBefore(line, "=").trim();
                var value = StringUtils.substringAfter(line, "=").trim();
                map.put(key, value);
            }
        }
        return map;
    }

    private static Path getVariablesFile(String fullpath, String baseName) {
        var varFile = Paths.get(fullpath + baseName + EXTENSION_PROPERTIES);
        if (fileExists(varFile)) {
            return varFile;
        }
        varFile = Paths.get(fullpath + baseName + EXTENSION_VARIABLES);
        if (fileExists(varFile)) {
            return varFile;
        }
        return null;
    }

    private static boolean fileExists(Path file) {
        return file != null && Files.isRegularFile(file);
    }
}
