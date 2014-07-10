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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result
                + ((newValue == null) ? 0 : newValue.hashCode());
        result = prime * result
                + ((oldValue == null) ? 0 : oldValue.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MapChangeEvent<?,?> other = (MapChangeEvent<?,?>) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (newValue == null) {
            if (other.newValue != null) {
                return false;
            }
        } else if (!newValue.equals(other.newValue)) {
            return false;
        }
        if (oldValue == null) {
            if (other.oldValue != null) {
                return false;
            }
        } else if (!oldValue.equals(other.oldValue)) {
            return false;
        }
        if (source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!source.equals(other.source)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MapChangeEvent [source=" + source + ", key=" + key
                + ", oldValue=" + oldValue + ", newValue=" + newValue + "]";
    }
}
