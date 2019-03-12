/* Copyright 2018-2019 Norconex Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Map-related utility methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class MapUtil {

    private MapUtil() {
        super();
    }

    /**
     * Converts a array of values to a map, alternating between key and values.
     * @param <K> may key type
     * @param <V> may value type
     * @param values to convert
     * @return the new map
     */
    public static <K, V> Map<K, V> toMap(Object... values) {
        Map<K, V> map = new HashMap<>();
        toMap(map, values);
        return map;
    }
    /**
     * Populates an existing map with the array of values,
     * alternating between key and values.
     * @param <K> may key type
     * @param <V> may value type
     * @param map the map to fill with values
     * @param values to convert
     */
    @SuppressWarnings("unchecked")
    public static <K, V> void toMap(Map<K, V> map, Object... values) {
        Objects.requireNonNull(map, "'map' must not be null.");
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Must supply an even number of values.");
        }
        for (int i = 0; i < values.length; i = i + 2) {
            map.put((K) values[i], (V) values[i + 1]);
        }
    }
}
