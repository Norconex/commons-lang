/* Copyright 2010-2022 Norconex Inc.
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
package com.norconex.commons.lang.map;

import java.util.Map;

/**
 * Listener for {@link Map} changes.
 * @since 1.4
 * @see ObservableMap
 * @see MapChangeSupport
 *
 * @param <K> the type of keys maintained by the map we are observing
 * @param <V> the type of mapped values
 * @deprecated Use {@link MapChangeListener} instead.
 */
@Deprecated(since="3.0.0")
@FunctionalInterface
public interface IMapChangeListener<K, V> //NOSONAR
        extends MapChangeListener<K, V> {
}
