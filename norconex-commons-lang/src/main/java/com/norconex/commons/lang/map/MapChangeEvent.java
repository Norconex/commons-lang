/* Copyright 2010-2014 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
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
