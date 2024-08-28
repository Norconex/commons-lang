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
package com.norconex.commons.lang.bean;

import static com.norconex.commons.lang.bean.stubs.WithDefaultValues.DEFAULT_LIST;
import static com.norconex.commons.lang.bean.stubs.WithDefaultValues.DEFAULT_NUMBER;
import static com.norconex.commons.lang.bean.stubs.WithDefaultValues.DEFAULT_OBJECT;
import static com.norconex.commons.lang.bean.stubs.WithDefaultValues.DEFAULT_TEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.StringReader;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.bean.stubs.AutomobileConfig;
import com.norconex.commons.lang.bean.stubs.WithDefaultValues;
import com.norconex.commons.lang.map.MapUtil;

class DefaultValueTest {

    private final Map<Format, String> absent = MapUtil.toMap(
            Format.XML,
            """
            <config>
              <reference>potato</reference>
            </config>
            """,
            Format.JSON,
            """
            {
              "reference": "potato"
            }
            """,
            Format.YAML,
            """
            reference: potato
            """);

    private final Map<Format, String> empty = MapUtil.toMap(
            Format.XML,
            """
            <config>
              <reference>potato</reference>
              <text></text>
              <number>  </number>
              <object>
              </object>
              <collection>  </collection>
            </config>
            """,
            Format.JSON,
            """
            {
              "reference": "potato",
              "text": "",
              "number": "",
              "object": {},
              "collection": []
            }
            """,
            Format.YAML,
            """
            reference: potato
            text: ""
            number: ""
            object: {}
            collection: []
            """);

    private final Map<Format, String> nulls = MapUtil.toMap(
            Format.XML,
            """
            <config>
              <reference>potato</reference>
              <text/>
              <number/>
              <object/>
              <collection/>
            </config>
            """,
            Format.JSON,
            """
            {
              "reference": "potato",
              "text": null,
              "number": null,
              "object": null,
              "collection": null
            }
            """,
            // omitted, null, or ~
            Format.YAML,
            """
            reference: potato
            text:
            number:
            object: ~
            collection: null
            """);

    private final Map<Format, String> filled = MapUtil.toMap(
            Format.XML,
            """
            <config>
              <reference>potato</reference>
              <text>hello</text>
              <number>13</number>
              <object>
                <model>Camry</model>
              </object>
              <collection>
               <entry>Carrot</entry>
              </collection>
            </config>
            """,
            Format.JSON,
            """
            {
              "reference": "potato",
              "text": "hello",
              "number": 13,
              "object": {
                "model": "Camry"
              },
              "collection": [
                "Carrot"
              ]
            }
            """,
            Format.YAML,
            """
            reference: potato
            text: hello
            number: 13
            object:
              model: Camry
            collection:
              - Carrot
            """);

    @ParameterizedTest
    @EnumSource(Format.class)
    void testAbsent(Format format) {
        // When not present in config, default values must be kept
        var config = absent.get(format);
        var obj = BeanMapper.DEFAULT.read(
                WithDefaultValues.class, new StringReader(config), format);
        assertThat(obj.getReference()).isEqualTo("potato");
        assertThat(obj.getText()).isEqualTo(DEFAULT_TEXT);
        assertThat(obj.getNumber()).isEqualTo(DEFAULT_NUMBER);
        assertThat(obj.getObject()).isEqualTo(DEFAULT_OBJECT);
        assertThat(obj.getCollection())
                .containsExactlyElementsOf(DEFAULT_LIST);
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(obj, format));
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void testEmpty(Format format) throws JsonProcessingException {
        var config = empty.get(format);
        var obj = BeanMapper.DEFAULT.read(
                WithDefaultValues.class, new StringReader(config), format);
        assertThat(obj.getReference()).isEqualTo("potato");
        assertThat(obj.getText()).isEmpty();
        assertThat(obj.getNumber()).isZero();
        // specifying an empty object clears all values and goes to defaults
        assertThat(obj.getObject()).isEqualTo(new AutomobileConfig());
        assertThat(obj.getCollection()).isEmpty();
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(obj, format));
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void testNulls(Format format) {
        var config = nulls.get(format);
        var obj = BeanMapper.DEFAULT.read(
                WithDefaultValues.class, new StringReader(config), format);
        assertThat(obj.getReference()).isEqualTo("potato");
        assertThat(obj.getText()).isNull();
        assertThat(obj.getNumber()).isZero();
        assertThat(obj.getObject()).isNull();
        assertThat(obj.getCollection()).isNull();
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(obj, format));
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void testFilled(Format format) {
        // When present in config ,default values must be overwritten
        var config = filled.get(format);
        var obj = BeanMapper.DEFAULT.read(
                WithDefaultValues.class, new StringReader(config), format);
        assertThat(obj.getReference()).isEqualTo("potato");
        assertThat(obj.getText()).isEqualTo("hello");
        assertThat(obj.getNumber()).isEqualTo(13);
        assertThat(obj.getObject()).isEqualTo(
                new AutomobileConfig().setModel("Camry"));
        assertThat(obj.getCollection()).containsExactly("Carrot");
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(obj, format));
    }
}
