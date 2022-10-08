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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.ErrorHandler;

import com.norconex.commons.lang.SystemUtil;
import com.norconex.commons.lang.security.Credentials;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

import lombok.Data;

class ConfigurationLoaderTest {

    private static final String CFG_BASE_PATH = "src/test/resources/config/";

    private ConfigurationLoader configLoader;

    @BeforeEach
    void beforeEach() {
        configLoader = new ConfigurationLoader();
    }

    @Test
    void testLoadXMLPath() {
        XML xml = configLoader.loadXML(cfgPath("xml.xml"));
        assertThat(xml.getString("username")).isEqualTo("joe");
        assertThat(xml.getString("password")).isEqualTo("whatever");
    }

    @Test
    void testLoadXMLPathErrorHandler() {
        XML xml = configLoader.loadXML(cfgPath("xml.xml"), null);
        assertThat(xml.getString("username")).isEqualTo("joe");
        assertThat(xml.getString("password")).isEqualTo("whatever");

        assertThat(configLoader.loadXML(null, null)).isNull();
    }

    @Test
    void testLoadFromXMLPath() {
        Credentials creds = configLoader.loadFromXML(cfgPath("xml.xml"));
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");
    }

    @Test
    void testLoadFromXMLPathErrorHandler() {
        Credentials creds = configLoader.loadFromXML(
                cfgPath("xml.xml"), (ErrorHandler) null);
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");

        assertThat((Object) configLoader.loadFromXML(
                null, (ErrorHandler) null)).isNull();
    }

    @Test
    void testLoadFromXMLPathClass() {
        var creds = configLoader.loadFromXML(
                cfgPath("xml.xml"), TestConfig.class);
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");
    }

    @Test
    void testLoadFromXMLPathClassErrorHandler() {
        var creds = configLoader.loadFromXML(
                cfgPath("xml.xml"), TestConfig.class, null);
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");

        assertThat(configLoader.loadFromXML(
                null, (Class<?>) null, null)).isNull();
    }

    @Test
    void testLoadFromXMLPathObject() {
        TestConfig creds = new TestConfig();
        configLoader.loadFromXML(cfgPath("xml.xml"), creds);
        assertThat(creds.getUsername()).isEqualTo("joe");
        assertThat(creds.getPassword()).isEqualTo("whatever");
    }

    @Test
    void testLoadString() throws Exception {
        configLoader.setVariablesFile(cfgPath("string.vars.txt"));
        var str = SystemUtil.callWithProperty("VAR_E", "beans", () ->
            configLoader.loadString(cfgPath("string.cfg")));
        // "varB" should not be resolved as it comes from an #include
        // directive (as opposed to parse)
        assertThat(str).isEqualTo(
                "Config with coffee in it.\n"
                + "It includes ${varB}, \n"
                + "as well as milk, sugar and beans.");
        
        // null path
        assertThrows(ConfigurationException.class, 
                () -> configLoader.loadString(null));

        // blank include
        Path blankIncl = cfgPath("blank-include.cfg");
        assertThrows(ConfigurationException.class, 
                () -> configLoader.loadString(blankIncl));

        // invalid path
        assertThat(configLoader.loadString(cfgPath("doesntExist"))).isNull();
    }

    private Path cfgPath(String path) {
        return Path.of(CFG_BASE_PATH + path);
    }

    @Data
    static class TestConfig implements IXMLConfigurable {
        private String username;
        private String password;
        @Override
        public void loadFromXML(XML xml) {
            username = xml.getString("username");
            password = xml.getString("password");
        }
        @Override
        public void saveToXML(XML xml) {
            //NOOP
        }
    }
}
