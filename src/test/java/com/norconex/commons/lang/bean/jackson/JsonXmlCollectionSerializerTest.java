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

import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.Format;

import lombok.Data;

// Test that collections are written properly, with proper null/empty handling.
class JsonXmlCollectionSerializerTest {

    @Test
    void testCollectionSerialize() {
        var testClass = new TestClass();
        testClass.filledList = List.of("one", "two", "three");
        testClass.regularProp1 = "prop1";
        testClass.emptyList = List.of();
        testClass.regularProp2 = "prop2";
        testClass.nullList = null;
        testClass.regularProp3 = "prop3";

        var sw = new StringWriter();
        //        BeanMapper.builder().indent(true).build().write(testClass, sw, Format.XML);
        BeanMapper.DEFAULT.write(testClass, sw, Format.XML);
        System.err.println(sw.toString());
    }

    @Data
    static class TestClass {
        // those are defaults, so not rendered
        private List<String> filledList = List.of("replaceme");
        private String regularProp1 = "replaceme";
        private List<String> emptyList = List.of("replaceme");
        private String regularProp2 = "replaceme";
        private List<String> nullList = List.of("replaceme");
        private String regularProp3 = "replaceme";
    }
}
