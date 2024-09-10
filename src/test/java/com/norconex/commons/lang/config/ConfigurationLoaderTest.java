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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.norconex.commons.lang.SystemUtil;
import com.norconex.commons.lang.security.Credentials;

import lombok.Data;

class ConfigurationLoaderTest {

    private static final String CFG_BASE_PATH = "src/test/resources/config/vlt/";

    private ConfigurationLoader configLoader;

    @BeforeEach
    void beforeEach() {
        configLoader = ConfigurationLoader.builder().build();
    }

    // when passing an object, should load into that object
    @ParameterizedTest
    @ValueSource(strings = { "xml", "json", "yaml" })
    void testLoadToSimpleObject(String extension) throws IOException {
        var testConfig = new TestConfig();
        configLoader.toObject(cfgPath("object." + extension), testConfig);
        assertThat(testConfig.getUsername()).isEqualTo("joe");
        assertThat(testConfig.getPassword()).isEqualTo("whatever");
    }

    // when passing a class, should instantiate and load into created class.
    @ParameterizedTest
    @ValueSource(strings = { "xml", "json", "yaml" })
    void testLoadToSimpleObjectFromClass(String extension)
            throws IOException {
        var creds = configLoader.toObject(
                cfgPath("object." + extension), TestConfig.class);
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");
    }

    @ParameterizedTest
    @ValueSource(strings = { "xml", "json", "yaml" })
    void testLoadToComplexObjectFromClass(String extension)
            throws IOException {
        var testConfig = configLoader.toObject(
                cfgPath("object-with-creds." + extension),
                TestConfigWithCreds.class);
        assertThat(testConfig.getCredentials().getUsername())
                .isEqualTo("joe");
        assertThat(testConfig.getCredentials().getPassword())
                .isEqualTo("whatever");
    }

    // Loads to com.norconex.commons.lang.xml.XML
    @Test
    void testLoadToXml() {
        var xml = configLoader.toXml(cfgPath("object.xml"));
        assertThat(xml.getString("username")).isEqualTo("joe");
        assertThat(xml.getString("password")).isEqualTo("whatever");
    }

    @Test
    void testLoadToString() throws Exception {
        var loader = ConfigurationLoader.builder()
                .variablesFile(cfgPath("string.vars.txt"))
                .build();
        var str = SystemUtil.callWithProperty("VAR_E", "beans",
                () -> loader.toString(cfgPath("string.cfg")));
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
    @Test
    void testIndentToString() throws Exception {
        var loader = ConfigurationLoader.builder()
                .variablesFile(cfgPath("main_item_template7.vm"))
                .build();
        var str = SystemUtil.callWithProperty("VAR_E", "beans",
                () -> loader.toString(cfgPath("main_template.vm")));
        // "varB" should not be resolved as it comes from an #include
        // directive (as opposed to parse)
        assertThat(StringUtils.remove(str, '\r')).isEqualTo(
                """
                        title: $title
                        date: $date
                        title: template title
                        date: 2024-08-20
                        depth1_include_tst_1:
                          - template2
                          key: template2
                          depth_test
                            title: $title
                            date: $date
                        depth1_parse_tst2:
                          - template2
                          key: template2
                          depth_test
                            title: template title
                            date: 2024-08-20
                        depth_tst2:
                          tst_depth_2_1:
                            - template2
                            key: template2
                            depth_test
                              title: $title
                              date: $date
                            - template2
                            key: template2
                            depth_test
                              title: template title
                              date: 2024-08-20
                        depth_tst_3:
                          depth_tst_3_1:
                            3333: 3333333333
                            3333: 3333333333
                            title: $title
                            date: $date
                            title: template title
                            date: 2024-08-20
                            depth_tst_3_1:
                              tst_depth_3_1_1:
                                3333: 3333333333
                                title: $title
                                date: $date
                                3333: 3333333333
                              tst_depth_3_1_2:
                                title: template title
                                date: 2024-08-20
                                - template2
                                key: template2
                                depth_test
                                  title: template title
                                  date: 2024-08-20
                                tst_depth_3_1_2-1
                                  - 5555: 55555555
                                  5555: 5555555555
                        depth_tst_4_ifelse_and_loop:
                            Name: Alice
                          Age: [10, 30, 50]
                          Feels: very old.
                          Name: Bob
                          Age: [10, 30, 50]
                          Feels: very old.
                          Name: Charlie
                          Age: [10, 30, 50]
                          Feels: very old.    
                            """);

        // null path
        assertThrows(ConfigurationException.class,
                () -> loader.toString(null));


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
