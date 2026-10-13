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
package com.norconex.commons.lang.bean.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.bean.stubs.ClearableDefaults;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * The factory has to survive being used the way Jackson uses factories, and
 * this library must not bring back a copy of one of Jackson's classes under
 * Jackson's own name.
 */
class NullKeepingSerializerFactoryTest {

    /**
     * This library used to replace Jackson's PropertyBuilder by shipping a
     * class of the same name. That only worked when this library came before
     * Jackson on the class path, which an application packaged as one jar does
     * not guarantee, and it dropped explicit nulls without any error when it
     * did not. Nothing of Jackson's may be redefined here again.
     */
    @Test
    void thisLibraryDoesNotShipAClassOfJacksonsUnderItsName()
            throws IOException {
        var copies = Collections.list(getClass().getClassLoader().getResources(
                "tools/jackson/databind/ser/PropertyBuilder.class"));

        assertThat(copies).as("copies of Jackson's PropertyBuilder: %s", copies)
                .hasSize(1);
        assertThat(copies.get(0).toString()).contains("jackson-databind");
    }

    @Test
    void nothingInTheBuildOutputIsInJacksonsPackages() throws IOException {
        var classes = Path.of("target", "classes");
        if (!Files.isDirectory(classes)) {
            return; // not run from a build tree
        }
        try (var tree = Files.walk(classes)) {
            var inJackson = tree
                    .map(p -> classes.relativize(p).toString().replace('\\',
                            '/'))
                    .filter(p -> p.startsWith("tools/jackson/databind/")
                            && p.endsWith(".class"))
                    .collect(Collectors.toList());
            assertThat(inJackson).isEmpty();
        }
    }

    @Test
    void derivingAFactoryFromItKeepsItsType() {
        var derived = NullKeepingSerializerFactory.INSTANCE.withConfig(
                new tools.jackson.databind.cfg.SerializerFactoryConfig());

        assertThat(derived).isInstanceOf(NullKeepingSerializerFactory.class);
    }

    @Test
    void usingTheSameConfigGivesTheSameFactory() {
        var f = new Exposed();

        // what a mapper with no extra serializers does
        assertThat(f.withConfig(f.config())).isSameAs(f);
    }

    /** Lets a test see the configuration the factory was built with. */
    static class Exposed extends NullKeepingSerializerFactory {
        private static final long serialVersionUID = 1L;

        Exposed() {
            super(null);
        }

        tools.jackson.databind.cfg.SerializerFactoryConfig config() {
            return _factoryConfig;
        }
    }

    /**
     * A module that registers serializers derives a new factory from the one
     * installed. If that returned a plain Jackson factory, the property builder
     * would quietly be lost for any application with a module of its own.
     */
    @Test
    void aModuleAddedLaterNeitherBreaksNorLosesTheNullHandling() {
        var module = new SimpleModule().addSerializer(Marker.class,
                new ValueSerializer<Marker>() {
                    @Override
                    public void serialize(Marker value, JsonGenerator gen,
                            SerializationContext ctxt) {
                        gen.writeString("marked");
                    }
                });
        var mapper = BeanMapper.builder()
                .mapperBuilderCustomizer(b -> b.addModule(module))
                .build();
        var bean = new ClearableDefaults().setText(null);

        var writer = new StringWriter();
        mapper.write(bean, writer, Format.JSON);
        var json = writer.toString();

        assertThat(json).containsPattern("\"text\"\\s*:\\s*null");
        assertThat(json).doesNotContain("\"primitive\"");
        // and the module's own serializer is in use
        var marked = new StringWriter();
        mapper.write(new Marker(), marked, Format.JSON);
        assertThat(marked.toString()).contains("marked");
    }

    static class Marker {
    }
}
