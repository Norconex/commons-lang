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

import org.apache.commons.collections4.MultiValuedMap;

import com.norconex.commons.lang.bean.BeanMapper;

/**
 * Java service provider interface (SPI) for automatically
 * registering types and sub-types to the {@link BeanMapper}
 * @since 3.0.0
 */
public interface PolymorphicTypeProvider {

    /**
     * Gets polymorphic types and their sub-types for auto-registration
     * by {@link BeanMapper} (or for custom use).-
     * @return a map keyed by polymorphic types and valued by their subtypes
     */
    MultiValuedMap<Class<?>, Class<?>> getPolymorphicTypes();
}
