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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.bean.stubs.Automobile;
import com.norconex.commons.lang.bean.stubs.Plane;
import com.norconex.commons.lang.bean.stubs.Transportation;

class BeanMapperPolymorphicTypesTest {

    private static BeanMapper newMapper() {
        return BeanMapper.builder()
                .skipValidation(true)
                .polymorphicServiceLoaderDisabled(true)
                .polymorphicType(Transportation.class,
                        name -> name.startsWith("com.norconex."))
                .build();
    }

    @Test
    void testBuildingMappersDoesNotAlterResolvedTypes() {
        var bm = newMapper();
        var before = new ArrayList<>(
                bm.getPolymorphicTypes().get(Transportation.class));
        assertThat(before).contains(Automobile.class, Plane.class);

        // Build an ObjectMapper for every format (this used to append the
        // sub-types of each type to themselves, every time).
        for (var format : Format.values()) {
            bm.write(new Plane(), new StringWriter(), format);
        }

        assertThat(bm.getPolymorphicTypes().get(Transportation.class))
                .containsExactlyElementsOf(before);
    }

    @Test
    void testResolvedTypesAreUnmodifiable() {
        var bm = newMapper();
        var types = bm.getPolymorphicTypes();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> types.put(Transportation.class, Plane.class));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(types::clear);
        assertThat(bm.getPolymorphicTypes()).isSameAs(types);
    }

    @Test
    void testBuilderRegisteredTypesStillResolved() {
        var bm = BeanMapper.builder()
                .polymorphicServiceLoaderDisabled(true)
                .polymorphicTypeImpl(Transportation.class,
                        java.util.List.of(Plane.class))
                .build();
        assertThat(bm.getPolymorphicTypes().get(Transportation.class))
                .containsExactly(Plane.class);
    }

    @Test
    void testIterateWhileMappersBeingBuilt() throws Exception {
        // Reader iterates the resolved types while other threads trigger
        // the (lazy) creation of the ObjectMappers on the same instance.
        for (var round = 0; round < 50; round++) {
            var bm = newMapper();
            var pool = Executors.newFixedThreadPool(4);
            try {
                var futures = new ArrayList<java.util.concurrent.Future<?>>();
                for (var format : Format.values()) {
                    futures.add(pool.submit(() -> bm.write(
                            new Plane(), new StringWriter(), format)));
                }
                futures.add(pool.submit(() -> {
                    for (var i = 0; i < 200; i++) {
                        assertThat(bm.getPolymorphicTypes().values())
                                .contains(Automobile.class, Plane.class);
                    }
                }));
                for (var f : futures) {
                    f.get(30, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
