/* Copyright 2020-2022 Norconex Inc.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple FIFO Map base on {@link LinkedHashMap}. This class is not thread safe
 * (must be synchronized externally).
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see Collections#synchronizedMap(Map)
 * @since 2.0.0
 */
public final class FifoMap<K, V> //NOSONAR we don't consider maxSize for equality
        extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private final int maxSize;

    public FifoMap(int maxSize) {
        this.maxSize = maxSize;
        if (this.maxSize < 1) {
            throw new IllegalArgumentException(
                    "'maxSize' must be greater than zero.");
        }
    }

    public int getMaxSize() {
        return maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > maxSize;
    }
}
