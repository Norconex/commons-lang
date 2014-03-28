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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final List<IMapChangeListener<K,V>> listeners= new ArrayList<>();
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
}
