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

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.ArrayUtils;

import com.norconex.commons.lang.ClassFinder;

import lombok.NonNull;

/**
 * Abstract implementation of {@link PolymorphicTypeProvider} offering
 * convenience methods to facilitate registration.
 * @since 3.0.0
 */
public abstract class BasePolymorphicTypeProvider
        implements PolymorphicTypeProvider {

    @Override
    public final MultiValuedMap<Class<?>, Class<?>> getPolymorphicTypes() {
        var registry = new Registry();
        register(registry);
        return registry.get();
    }

    protected abstract void register(Registry registry);

    /**
     * Helper for registering polymorphic types and sub-types.
     */
    public static class Registry {
        private final MultiValuedMap<Class<?>, Class<?>> map =
                MultiMapUtils.newListValuedHashMap();

        /**
         * Adds a super type and all supplied sub-types.
         * @param superType polymorphic type
         * @param subType required polymorphic sub-type
         * @param subTypes optional additional sub-types
         * @return this, for chaining
         */
        public Registry add(
                @NonNull Class<?> superType,
                @NonNull Class<?> subType,
                Class<?>... subTypes) {
            map.putAll(superType, List.of(ArrayUtils.add(subTypes, subType)));
            return this;
        }

        /**
         * Adds a super type and all sub-types discovered from scanning
         * for classes with a qualified name starting with the same package
         * as the super class.
         * Scanning is performed using the super type class loader.
         * @param superType polymorphic type to add and discover its sub-types
         * @return this, for chaining
         */
        public Registry addFromScan(
                @NonNull Class<?> superType) {
            map.putAll(superType, ClassFinder.findSubTypes(
                    superType,
                    nm -> nm.startsWith(superType.getPackageName())));
            return this;
        }

        /**
         * Adds a super type and all sub-types discovered from scanning
         * for classes with a qualified name starting with the supplied
         * base package.
         * Scanning is performed using the super type class loader.
         * @param superType polymorphic type to add and discover its sub-types
         * @param basePackage the fully qualified class names prefix to match
         * @return this, for chaining
         */
        public Registry addFromScan(
                @NonNull Class<?> superType,
                @NonNull String basePackage) {
            return addFromScan(superType, nm -> nm.startsWith(basePackage));
        }

        /**
         * Adds a super type and all sub-types discovered from scanning
         * for classes with a qualified name matching the supplied predicate.
         * Scanning is performed using the super type class loader.
         * @param superType polymorphic type to add and discover its sub-types
         * @param filter predicate returning <code>true</code> on
         *     matching sub-types
         * @return this, for chaining
         */
        public Registry addFromScan(
                @NonNull Class<?> superType,
                @NonNull Predicate<String> filter) {
            map.putAll(superType, ClassFinder.findSubTypes(superType, filter));
            return this;
        }

        private MultiValuedMap<Class<?>, Class<?>> get() {
            return map;
        }
    }
}
