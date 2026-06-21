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
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.bean.jackson.JsonXmlCollection;
import com.norconex.commons.lang.bean.jackson.JsonXmlMap;
import com.norconex.commons.lang.config.Configurable;

import lombok.Data;

/**
 * Verifies that XML collection/map handling on an unwrapped {@link Configurable}
 * config works WITHOUT requiring {@code @JsonXmlCollection}/{@code @JsonXmlMap}
 * (auto-registered module behavior), while annotated and method-based
 * properties keep working (no regression).
 */
class UnwrappedConfigAutoCollectionTest {

    @Data
    public static class Thing {
        private String name;
    }

    @Data
    public static class HolderConfig {
        // --- UNannotated: must be auto-handled by the registered module ---
        private final List<String> tags = new ArrayList<>();
        private final List<Thing> things = new ArrayList<>();
        private final Set<String> codes = new LinkedHashSet<>();
        private final Map<String, String> attrs = new LinkedHashMap<>();

        // --- Annotated: regression guard (must keep working) ---
        @JsonXmlCollection(entryName = "op")
        private final List<Thing> ops = new ArrayList<>();
        @JsonXmlMap(entryName = "param")
        private final Map<String, String> params = new LinkedHashMap<>();

        // --- Method-based collection property (like HdfsFetcherConfig) ---
        private final List<String> codePaths = new ArrayList<>();

        public HolderConfig setTags(List<String> v) {
            reset(tags, v);
            return this;
        }

        public HolderConfig setThings(List<Thing> v) {
            reset(things, v);
            return this;
        }

        public HolderConfig setCodes(Set<String> v) {
            codes.clear();
            codes.addAll(v);
            return this;
        }

        public HolderConfig setAttrs(Map<String, String> v) {
            attrs.clear();
            attrs.putAll(v);
            return this;
        }

        public HolderConfig setOps(List<Thing> v) {
            reset(ops, v);
            return this;
        }

        public HolderConfig setParams(Map<String, String> v) {
            params.clear();
            params.putAll(v);
            return this;
        }

        @JsonProperty("codePaths")
        List<String> getCodePathsAsStr() {
            return new ArrayList<>(codePaths);
        }

        @JsonProperty("codePaths")
        HolderConfig setCodePathsAsStr(List<String> v) {
            reset(codePaths, v);
            return this;
        }

        private static <T> void reset(List<T> target, List<T> v) {
            target.clear();
            if (v != null) {
                target.addAll(v);
            }
        }
    }

    @Data
    public static class Holder implements Configurable<HolderConfig> {
        private final HolderConfig configuration = new HolderConfig();
    }

    private static Thing thing(String n) {
        var t = new Thing();
        t.setName(n);
        return t;
    }

    private HolderConfig readXml(String body) {
        var h = new Holder();
        BeanMapper.DEFAULT.read(
                h, new StringReader("<Holder>" + body + "</Holder>"),
                Format.XML);
        return h.getConfiguration();
    }

    // ---- UNannotated collections (the design intent) ----

    @Test
    void unannotated_listString_multi() {
        assertThat(readXml("<tags><tag>a</tag><tag>b</tag></tags>").getTags())
                .containsExactly("a", "b");
    }

    @Test
    void unannotated_listString_single() {
        assertThat(readXml("<tags><tag>solo</tag></tags>").getTags())
                .containsExactly("solo");
    }

    @Test
    void unannotated_listBean() {
        assertThat(readXml(
                "<things><thing><name>x</name></thing>"
                        + "<thing><name>y</name></thing></things>")
                                .getThings())
                                        .extracting(Thing::getName)
                                        .containsExactly("x", "y");
    }

    @Test
    void unannotated_set() {
        assertThat(readXml("<codes><code>a</code><code>b</code></codes>")
                .getCodes()).containsExactly("a", "b");
    }

    // ---- Annotated: must keep working ----

    @Test
    void annotated_collection() {
        assertThat(readXml(
                "<ops><op><name>o1</name></op><op><name>o2</name></op></ops>")
                        .getOps())
                                .extracting(Thing::getName)
                                .containsExactly("o1", "o2");
    }

    // ---- Round-trip across XML/JSON/YAML (covers maps + everything) ----

    @Test
    void roundTrip_allFormats() {
        var h = new Holder();
        h.getConfiguration()
                .setTags(List.of("a", "b"))
                .setThings(List.of(thing("x"), thing("y")))
                .setCodes(new LinkedHashSet<>(List.of("c1", "c2")))
                .setAttrs(new LinkedHashMap<>(Map.of("k", "v")))
                .setOps(List.of(thing("o1")))
                .setParams(new LinkedHashMap<>(Map.of("p", "1")))
                .setCodePathsAsStr(List.of("/a", "/b"));
        assertThatNoException()
                .isThrownBy(() -> BeanMapper.DEFAULT.assertWriteRead(h));
    }
}
