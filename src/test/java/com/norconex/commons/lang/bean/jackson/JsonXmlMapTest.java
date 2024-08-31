/* Copyright 2024 Norconex Inc.
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
package com.norconex.commons.lang.bean.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections4.map.LinkedMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.map.MapUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

class JsonXmlMapTest {

    private static MapHolder ch;

    @BeforeAll
    static void beforeAll() {
        ch = new MapHolder();
        ch.some = "thing";
        ch.defaultEntryNames.putAll(MapUtil.toMap("k1", "v1", "k2", "v2"));
        ch.specifiedEntryNames =
                new LinkedMap<>(MapUtil.toMap("k3", "v3", "k4", "v4"));
        ch.defaultType = new HashMap<>(MapUtil.toMap(5, true, 6, false));
        ch.complexType.putAll(MapUtil.toMap(
                new SomeKey("k7-a", "k7-b"), new SomeValue("v7-a", "b7-b"),
                new SomeKey("k8-a", "k8-b"), new SomeValue("v8-a", "b8-b"),
                new SomeKey("k9-a", "k9-b"), new SomeValue("v9-a", "b9-b")));
    }

    @Test
    void testWriteRead() {
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(ch, Format.XML));
    }

    @Test
    void testXmlTagNames() {
        var out = new StringWriter();
        BeanMapper.DEFAULT.write(ch, out, Format.XML);
        var xml = out.toString();

        assertThat(xml).containsIgnoringWhitespaces("""
        <defaultEntryNames>
          <entry><key>k1</key><value>v1</value></entry>
          <entry><key>k2</key><value>v2</value></entry>
        </defaultEntryNames>
        """);
    }

    @Test
    void testNullMap() {
        assertThatNoException().isThrownBy(() -> {
            var out = new StringWriter();
            var obj = new ObjectWithNullMap();
            obj.nullMap = null;
            BeanMapper.DEFAULT.write(obj, out, Format.XML);
            BeanMapper.DEFAULT.write(obj, out, Format.JSON);
            BeanMapper.DEFAULT.write(obj, out, Format.YAML);
        });
    }

    @Data
    static class MapHolder {

        private String some;

        private final Map<String, String> defaultEntryNames = new TreeMap<>();

        @JsonXmlMap(
            entryName = "child",
            keyName = "childKey",
            valueName = "childValue"
        )
        private Map<String, String> specifiedEntryNames;

        private Map<Integer, Boolean> defaultType;

        private final Map<SomeKey, SomeValue> complexType = new HashMap<>();
    }

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SomeKey {
        private String propa;
        private String propb;
    }

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SomeValue {
        private String propc;
        private String propd;
    }

    @Data
    @JsonInclude(value = Include.ALWAYS, content = Include.ALWAYS)
    static class ObjectWithNullMap {
        private Map<String, String> nullMap = null;
    }
}
