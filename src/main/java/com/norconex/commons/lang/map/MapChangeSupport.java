/* Copyright 2010-2019 Norconex Inc.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Here is an example of {@code MapChangeSupport} usage:
 * <pre>
 * public class MyMap extends Map {
 *     private final MapChangeSupport mcs = new MapChangeSupport(this);
 *
 *     public void addMapChangeListener(IMapChangeListener listener) {
 *         this.mcs.addMapChangeListener(listener);
 *     }
 *
 *     public void removeMapChangeListener(IMapChangeListener listener) {
 *         this.mcs.removeMapChangeListener(listener);
 *     }
 *
 *     public Object put(Object key, Object value) {
 *         Object oldValue = map.put(key, value);
 *         mcs.fireMapChange(key, oldValue, value);
 *         return oldValue;
 *     }
 *
 *     [...]
 * }
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 1.4
 *
 * @param <K> the type of keys maintained by the map we are observing
 * @param <V> the type of mapped values
 */
public class MapChangeSupport<K,V> implements Serializable {

    private static final long serialVersionUID = 3044416439660906663L;
    private final List<IMapChangeListener<K,V>> listeners = new ArrayList<>();
    private final Map<K, V> source;

    public MapChangeSupport(Map<K, V> source) {
        this.source = source;
    }

    /**
     * Add a {@link IMapChangeListener} to the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The {@link IMapChangeListener}    to be added
     */
    public void addMapChangeListener(IMapChangeListener<K, V> listener) {
        if (listener == null) {
            return;
        }
        this.listeners.add(listener);
    }
    /**
     * Removes a {@link IMapChangeListener} from the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The {@link IMapChangeListener}    to be removed
     */
    public void removeMapChangeListener(IMapChangeListener<K,V> listener) {
        if (listener == null) {
            return;
        }
        this.listeners.remove(listener);
    }

    /**
     * Fires a {@link MapChangeEvent} to all change listeners.
     * @param key the key for the value change
     * @param oldValue the old value
     * @param newValue the new value
     */
    public void fireMapChange(K key, V oldValue, V newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        MapChangeEvent<K, V> event =
                new MapChangeEvent<>(source, key, oldValue, newValue);
        for (IMapChangeListener<K, V> l : listeners) {
            l.mapChanged(event);
        }
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
