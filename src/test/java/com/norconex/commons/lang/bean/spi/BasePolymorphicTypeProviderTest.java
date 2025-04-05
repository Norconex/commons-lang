/* Copyright 2025 Norconex Inc.
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
package com.norconex.commons.lang.bean.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.apache.commons.collections4.MultiValuedMap;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.spi.BasePolymorphicTypeProvider.Registry;

class BasePolymorphicTypeProviderTest {

    @Test
    void testRegister() {
        // scan with just super type
        var polyTypes = register(r -> r.addFromScan(SuperType1.class));
        assertThat(polyTypes.containsKey(SuperType1.class)).isTrue();
        assertThat(polyTypes.values()).containsExactlyInAnyOrder(
                SubType1A.class, SubType1B.class);

        // explicitly add, but just a subset of subtytpes
        polyTypes = register(r -> {
            r.add(SuperType2.class, SubType2A.class);
        });
        assertThat(polyTypes.containsKey(SuperType2.class)).isTrue();
        assertThat(polyTypes.values()).containsExactlyInAnyOrder(
                SubType2A.class);

        // Add with a bad and a good package
        polyTypes = register(r -> {
            r.addFromScan(SuperType1.class, "bad.package");
            r.addFromScan(SuperType2.class, getClass().getPackageName());
        });
        // if no subtype are found, the super type is not registered
        assertThat(polyTypes.containsKey(SuperType1.class)).isFalse();
        assertThat(polyTypes.containsKey(SuperType2.class)).isTrue();
        assertThat(polyTypes.values()).containsExactlyInAnyOrder(
                SubType2A.class, SubType2B.class);

    }

    private MultiValuedMap<Class<?>, Class<?>> register(Consumer<Registry> c) {
        return new BasePolymorphicTypeProvider() {
            @Override
            protected void register(Registry registry) {
                c.accept(registry);
            }
        }.getPolymorphicTypes();
    }

    public interface SuperType1 {

    }

    public static class SubType1A implements SuperType1 {

    }

    public static class SubType1B implements SuperType1 {

    }

    public interface SuperType2 {

    }

    public static class SubType2A implements SuperType2 {

    }

    public static class SubType2B implements SuperType2 {

    }
}
