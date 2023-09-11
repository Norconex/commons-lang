/* Copyright 2022-2023 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.SystemUtil;
import com.norconex.commons.lang.security.Credentials;

import lombok.Data;

class ConfigurationLoaderTest {

    private static final String CFG_BASE_PATH = "src/test/resources/config/";

    private ConfigurationLoader configLoader;

    @BeforeEach
    void beforeEach() {
        configLoader = ConfigurationLoader.builder().build();
    }

    @Test
    void testLoadXMLPath() {
        var xml = configLoader.toXml(cfgPath("xml.xml"));
        assertThat(xml.getString("username")).isEqualTo("joe");
        assertThat(xml.getString("password")).isEqualTo("whatever");
    }

    // when explicit passing a class, should load into that class.
    @Test
    void testLoadFromXMLPathClass() throws IOException {
        var creds = configLoader.toObject(
                cfgPath("xml.xml"), TestConfig.class);
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");
    }

    @Test
    void testLoadWithClassAttribute() throws IOException {
        var testConfig = configLoader.toObject(
                cfgPath("xml-with-creds.xml"),
                TestConfigWithCreds.class);
        assertThat(testConfig.getCredentials().getUsername())
            .isEqualTo("joe");
        assertThat(testConfig.getCredentials().getPassword())
            .isEqualTo("whatever");
    }

    @Test
    void testLoadFromXMLPathObject() throws IOException {
        var creds = new TestConfig();
        configLoader.toObject(cfgPath("xml.xml"), creds);
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");
    }

    @Test
    void testLoadString() throws Exception {
        var loader = ConfigurationLoader.builder()
                .variablesFile(cfgPath("string.vars.txt"))
                .build();
        var str = SystemUtil.callWithProperty("VAR_E", "beans", () ->
            loader.toString(cfgPath("string.cfg")));
        // "varB" should not be resolved as it comes from an #include
        // directive (as opposed to parse)
        assertThat(StringUtils.remove(str, '\r')).isEqualTo(
                """
                	Config with coffee in it.
                	It includes ${varB},\s
                	as well as milk, sugar and beans.""");

        // null path
        assertThrows(ConfigurationException.class,
                () -> loader.toString(null));

        // blank include
        var blankIncl = cfgPath("blank-include.cfg");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(blankIncl));

        // invalid path
        assertThat(loader.toString(cfgPath("doesntExist"))).isNull();
    }

    private Path cfgPath(String path) {
        return Path.of(CFG_BASE_PATH + path);
    }

    @Data
    static class TestConfig {
        private String username;
        private String password;
    }
    @Data
    static class TestConfigWithCreds {
        private Credentials credentials;
    }
}
