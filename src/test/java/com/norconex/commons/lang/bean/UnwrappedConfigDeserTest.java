/* Copyright 2026 Norconex Inc.
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
package com.norconex.commons.lang.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;

import lombok.Data;

/**
 * Diagnoses deserialization gaps for nested/scalar properties, comparing a
 * plain bean against a {@link Configurable} config (exposed by BeanMapper as
 * {@code @JsonUnwrapped}).
 */
class UnwrappedConfigDeserTest {

    @Data
    public static class Cfg {
        private final TextMatcher fieldMatcher = new TextMatcher();
        private String label;
        @JsonAlias("labelAlt")
        private String aliased;

        public Cfg setFieldMatcher(TextMatcher tm) {
            fieldMatcher.copyFrom(tm);
            return this;
        }
    }

    // Plain bean (NOT Configurable) -> properties bound normally.
    @Data
    public static class Plain {
        private final Cfg inner = new Cfg();
    }

    // Configurable bean -> config exposed as @JsonUnwrapped.
    @Data
    public static class Wrapped implements Configurable<Cfg> {
        private final Cfg configuration = new Cfg();
    }

    private Cfg readWrapped(String xml) {
        var w = new Wrapped();
        BeanMapper.DEFAULT.read(w, new StringReader(xml), Format.XML);
        return w.getConfiguration();
    }

    // ---- baseline: plain (non-unwrapped) nested bean binding ----

    @Test
    void plain_textMatcherChildElements() {
        var p = new Plain();
        BeanMapper.DEFAULT.read(p, new StringReader("""
                <Plain><inner>
                  <fieldMatcher>
                    <method>REGEX</method>
                    <pattern>^x.*</pattern>
                  </fieldMatcher>
                </inner></Plain>"""), Format.XML);
        assertThat(p.getInner().getFieldMatcher().getPattern())
                .isEqualTo("^x.*");
        assertThat(p.getInner().getFieldMatcher().getMethod())
                .isEqualTo(Method.REGEX);
    }

    // ---- unwrapped config: the failing cases ----

    @Test
    void unwrapped_textMatcherChildElements_upperEnum() {
        var cfg = readWrapped("""
                <Wrapped>
                  <fieldMatcher>
                    <method>REGEX</method>
                    <pattern>^x.*</pattern>
                  </fieldMatcher>
                </Wrapped>""");
        assertThat(cfg.getFieldMatcher().getPattern()).isEqualTo("^x.*");
        assertThat(cfg.getFieldMatcher().getMethod()).isEqualTo(Method.REGEX);
    }

    @Test
    void unwrapped_textMatcherTextContent() {
        var cfg = readWrapped(
                "<Wrapped><fieldMatcher>docdate</fieldMatcher></Wrapped>");
        assertThat(cfg.getFieldMatcher().getPattern()).isEqualTo("docdate");
    }

    @Test
    void unwrapped_jsonAlias() {
        var cfg = readWrapped("<Wrapped><labelAlt>hello</labelAlt></Wrapped>");
        assertThat(cfg.getAliased()).isEqualTo("hello");
    }

    @Test
    void unwrapped_emptyElementValue() {
        var cfg = readWrapped("<Wrapped><label></label></Wrapped>");
        assertThat(cfg.getLabel()).isEqualTo("");
    }

    // Isolate the re-parse step the FailOnBeanDeserializer performs:
    // update an existing config from a faithful JSON string via the JSON mapper.
    @Test
    void jsonReparse_direct() throws Exception {
        var cfg = new Cfg();
        BeanMapper.DEFAULT.toObjectMapper(BeanMapper.Format.JSON)
                .readerForUpdating(cfg)
                .readValue(
                        "{\"fieldMatcher\":{\"method\":\"REGEX\","
                                + "\"pattern\":\"^x.*\"},\"labelAlt\":\"hi\"}");
        assertThat(cfg.getFieldMatcher().getPattern()).isEqualTo("^x.*");
        assertThat(cfg.getFieldMatcher().getMethod()).isEqualTo(Method.REGEX);
        assertThat(cfg.getAliased()).isEqualTo("hi");
    }
}
