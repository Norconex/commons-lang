/* Copyright 2023 Norconex Inc.
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.Format;

import lombok.Data;
import lombok.experimental.Accessors;

class JsonXmlCollectionTest {

    private static CollectionHolder ch;

    @BeforeAll
    static void beforeAll() {
        ch = new CollectionHolder();
        ch.some = "thing";
        ch.defaultEntryNames.addAll(List.of("1", "2", "3"));
        ch.specifiedEntryNames = new HashSet<>(List.of("4", "5", "6"));
        ch.defaultType = new ArrayList<>(List.of("7", "8", "9"));
        ch.complexType.addAll(List.of(
                new SomeEntry().setPropa("aaa1").setPropb("bbb2").setPropc(3),
                new SomeEntry().setPropa("aaa4").setPropb("bbb5").setPropc(6),
                new SomeEntry().setPropa("aaa7").setPropb("bbb8").setPropc(9)));
    }

    @Test
    void testWriteRead() {
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(ch));
    }

    @Test
    void testXmlTagNames() {
        var out = new StringWriter();
        BeanMapper.DEFAULT.write(ch, out, Format.XML);
        var xml = out.toString();

        assertThat(xml).containsIgnoringWhitespaces(
                "<defaultEntryNames><defaultEntryName>",
                "<specifiedEntryNames><child>",
                "<defaultType><entry>");
    }

    @Test
    void testNullCollection()
            throws StreamWriteException, DatabindException, IOException {
        assertThatNoException().isThrownBy(() -> {
            var out = new StringWriter();
            var obj = new ObjectWithNullList();
            obj.nullList = null;
            BeanMapper.DEFAULT.write(obj, out, Format.XML);
            BeanMapper.DEFAULT.write(obj, out, Format.JSON);
            BeanMapper.DEFAULT.write(obj, out, Format.YAML);
        });
    }

    @Data
    static class CollectionHolder {

        private String some;

        private final List<String> defaultEntryNames = new LinkedList<>();

        @JsonXmlCollection(entryName = "child")
        private Set<String> specifiedEntryNames;

        private Collection<String> defaultType;

        private final List<SomeEntry> complexType = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    static class SomeEntry {
        private String propa;
        private String propb;
        private int propc;
    }

    @Data
    @JsonInclude(value = Include.ALWAYS, content = Include.ALWAYS)
    static class ObjectWithNullList {
        private List<String> nullList = null;
    }
}
