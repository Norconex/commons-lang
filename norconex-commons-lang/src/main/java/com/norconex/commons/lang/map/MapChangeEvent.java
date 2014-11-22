/* Copyright 2010-2014 Norconex Inc.
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

import java.io.Serializable;
import java.util.Map;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * A event representing a change of values in an {@link Map}.
 * @author Pascal Essiembre
 *
 * @author Pascal Essiembre
 * @since 1.4
 *
 * @param <K> the type of keys maintained by the map we are observing
 * @param <V> the type of mapped values
 */
public class MapChangeEvent<K, V> implements Serializable {

    private static final long serialVersionUID = -5475381612189548521L;

    private final Map<K, V> source;
    private final K key;
    private final V oldValue;
    private final V newValue;

    /**
     * Creates a new event.
     * @param source the source on which the change was applied.
     * @param key the key that has is value changed.
     * @param oldValue the old value
     * @param newValue the new value
     */
    public MapChangeEvent(
            Map<K, V> source, K key, V oldValue, V newValue) {
        super();
        this.source = source;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Gets the source on which the change was applied.
     * @return the source
     */
    public Map<K, V> getSource() {
        return source;
    }
    /**
     * Gets the key that has is value changed.
     * @return the key
     */
    public K getKey() {
        return key;
    }
    /**
     * Gets the old value.
     * @return old value
     */
    public V getOldValue() {
        return oldValue;
    }
    /**
     * Gets the new value.
     * @return new value
     */
    public V getNewValue() {
        return newValue;
    }

    

    @Override
    public String toString() {
        return "MapChangeEvent [source=" + source + ", key=" + key
                + ", oldValue=" + oldValue + ", newValue=" + newValue + "]";
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof MapChangeEvent)) {
            return false;
        }
        MapChangeEvent<?, ?> castOther = (MapChangeEvent<?, ?>) other;
        return new EqualsBuilder().append(source, castOther.source)
                .append(key, castOther.key)
                .append(oldValue, castOther.oldValue)
                .append(newValue, castOther.newValue).isEquals();
    }

    private transient int hashCode;

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(source).append(key)
                    .append(oldValue).append(newValue).toHashCode();
        }
        return hashCode;
    }
}
