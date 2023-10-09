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
package com.norconex.commons.lang.bean.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;

/**
 * Finds and load all registered {@link PolymorphicTypeProvider}.
 * @since 3.0.0
 */
public final class PolymorphicTypeLoader {

    private PolymorphicTypeLoader() {}

    public static List<PolymorphicTypeProvider> providers() {
        List<PolymorphicTypeProvider> providers = new ArrayList<>();
        ServiceLoader<PolymorphicTypeProvider> loader =
                ServiceLoader.load(PolymorphicTypeProvider.class);
        loader.forEach(providers::add);
        return providers;
    }

    public static MultiValuedMap<Class<?>, Class<?>> polymorphicTypes() {
        MultiValuedMap<Class<?>, Class<?>> map =
                MultiMapUtils.newListValuedHashMap();
        providers().forEach(p -> map.putAll(p.getPolymorphicTypes()));
        return map;
    }
}
