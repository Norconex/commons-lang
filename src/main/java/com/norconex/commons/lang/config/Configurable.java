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
package com.norconex.commons.lang.config;

/**
 * The implementing class can be configured via the object returned
 * by {@link #getConfiguration()}.
 * Provides a consistent way to obtain a file configuration dynamically.
 * Could be useful for object mapping, for instance.
 * @param <T> The configuration class type
 * @since 3.0.0
 */
public interface Configurable<T> {
    /**
     * Gets the configuration for a configurable object.
     * @return configuration
     */
    T getConfiguration();
}
