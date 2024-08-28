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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Here is an example of {@code MapChangeSupport} usage:
 * <pre>
 * public class MyMap extends Map {
 *     private final MapChangeSupport mcs = new MapChangeSupport(this);
 *
 *     public void addMapChangeListener(MapChangeListener listener) {
 *         this.mcs.addMapChangeListener(listener);
 *     }
 *
 *     public void removeMapChangeListener(MapChangeListener listener) {
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
 * @since 1.4
 *
 * @param <K> the type of keys maintained by the map we are observing
 * @param <V> the type of mapped values
 */
@ToString
@EqualsAndHashCode
public class MapChangeSupport<K, V> {

    private final List<MapChangeListener<K, V>> listeners = new ArrayList<>();
    private final Map<K, V> source;

    public MapChangeSupport(Map<K, V> source) {
        this.source = source;
    }

    /**
     * Add a {@link MapChangeListener} to the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The {@link MapChangeListener}    to be added
     */
    public void addMapChangeListener(MapChangeListener<K, V> listener) {
        if (listener == null) {
            return;
        }
        this.listeners.add(listener);
    }

    /**
     * Removes a {@link MapChangeListener} from the listener list.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The {@link MapChangeListener}    to be removed
     */
    public void removeMapChangeListener(MapChangeListener<K, V> listener) {
        if (listener == null) {
            return;
        }
        this.listeners.remove(listener);
    }

    /**
     * Gets an unmodifiable list of listeners.
     * @return listeners
     * @since 3.0.0
     */
    public List<MapChangeListener<K, V>> getMapChangeListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Clears the listeners associated with this instance.
     * @since 3.0.0
     */
    public void clear() {
        listeners.clear();
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
        for (MapChangeListener<K, V> l : listeners) {
            l.mapChanged(event);
        }
    }

    /**
     * Gets whether this instance has no listeners.
     * @return <code>true</code> if there are no listeners
     */
    public boolean isEmpty() {
        return listeners.isEmpty();
    }
}
