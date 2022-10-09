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
package com.norconex.commons.lang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class ResourceLoaderTest {

    @Test
    void testResourceLoader() throws IOException {
        Class<?> c = getClass();

        // txt
        assertThat(ResourceLoader.getTxtString(null)).isNull();
        assertThat(ResourceLoader.getTxtString(c)).isEqualTo("txt");
        assertThat(ResourceLoader.getTxtReader(null)).isNull();
        assertThat(toString(ResourceLoader.getTxtReader(c))).isEqualTo("txt");
        assertThat(ResourceLoader.getTxtStream(null)).isNull();
        assertThat(toString(ResourceLoader.getTxtStream(c))).isEqualTo("txt");

        // xml
        assertThat(ResourceLoader.getXmlString(null)).isNull();
        assertThat(ResourceLoader.getXmlString(c)).isEqualTo("xml");
        assertThat(ResourceLoader.getXmlReader(null)).isNull();
        assertThat(toString(ResourceLoader.getXmlReader(c))).isEqualTo("xml");
        assertThat(ResourceLoader.getXmlStream(null)).isNull();
        assertThat(toString(ResourceLoader.getXmlStream(c))).isEqualTo("xml");

        // html
        assertThat(ResourceLoader.getHtmlString(null)).isNull();
        assertThat(ResourceLoader.getHtmlString(c)).isEqualTo("html");
        assertThat(ResourceLoader.getHtmlReader(null)).isNull();
        assertThat(toString(ResourceLoader.getHtmlReader(c))).isEqualTo("html");
        assertThat(ResourceLoader.getHtmlStream(null)).isNull();
        assertThat(toString(ResourceLoader.getHtmlStream(c))).isEqualTo("html");

        // json
        assertThat(ResourceLoader.getJsonString(null)).isNull();
        assertThat(ResourceLoader.getJsonString(c)).isEqualTo("json");
        assertThat(ResourceLoader.getJsonReader(null)).isNull();
        assertThat(toString(ResourceLoader.getJsonReader(c))).isEqualTo("json");
        assertThat(ResourceLoader.getJsonStream(null)).isNull();
        assertThat(toString(ResourceLoader.getJsonStream(c))).isEqualTo("json");
    }

    private String toString(Reader r) throws IOException {
        try (r) {
            return IOUtils.toString(r);
        }
    }
    private String toString(InputStream is) throws IOException {
        try (is) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
