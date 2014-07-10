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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A map implementation that reports changes to added {@link IMapChangeListener}
 * instance.  Changes are triggered when a value change is detected
 * in the following method calls: {@link #put(Object, Object)}, 
 * {@link #putAll(Map)}, {@link #remove(Object)}, and {@link #clear()}.
 * This class will not detect changes made to key or value objects modified
 * outside this class.
 * This class can be used as a decorator to other {@link Map} implementations.
 * 
 * @author Pascal Essiembre
 * @since 1.4
 *
 * @param <K> the type of keys maintained by the map we are observing
 * @param <V> the type of mapped values
 */
public class ObservableMap<K,V> implements Map<K,V>, Serializable {

    private static final long serialVersionUID = 8109864235479497466L;

    private final MapChangeSupport<K,V> mcs = new MapChangeSupport<>(this);    
    private final Map<K, V> map;
    
    public ObservableMap() {
        this(null);
    }
    /**
     * Decorates map argument as an {@code ObservableMap}.
     * @param map the Map to decorate 
     */
    public ObservableMap(Map<K, V> map) {
        if (map == null) {
            this.map = new HashMap<K, V>();
        } else {
            this.map = map;
        }
    }

    /**
     * Adds a map change listener.
     * @param listener change listener
     */
    public void addMapChangeListener(IMapChangeListener<K,V> listener) {
        this.mcs.addMapChangeListener(listener);
    }
    /**
     * Removes a map change listener.
     * @param listener change listener
     */
    public void removeMapChangeListener(IMapChangeListener<K,V> listener) {
        this.mcs.removeMapChangeListener(listener);
    }
    
    @Override
    public int size() {
        return map.size();
    }
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }
    @Override
    public V get(Object key) {
        return map.get(key);
    }
    @Override
    public V put(K key, V value) {
        if (mcs.isEmpty()) {
            return map.put(key, value);
        }
        V oldValue = map.put(key, value);
        mcs.fireMapChange(key, oldValue, value);
        return oldValue;
    }
    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        if (mcs.isEmpty()) {
            return map.remove(key);
        }
        V oldValue = map.remove(key);
        mcs.fireMapChange((K) key, oldValue, null);
        return oldValue;
    }
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (mcs.isEmpty()) {
            map.putAll(m);
            return;
        }
        Iterator<? extends Map.Entry<? extends K, ? extends V>> it = 
                m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<? extends K, ? extends V> entry = 
                    (Map.Entry<? extends K, ? extends V>) it.next();
            put(entry.getKey(), entry.getValue());
        } 
    }
    @Override
    public void clear() {
        if (mcs.isEmpty()) {
            map.clear();
            return;
        }
        Map<K, V> oldMap = new HashMap<>(map);
        map.clear();
        Iterator<? extends Map.Entry<? extends K, 
                ? extends V>> it = oldMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<? extends K, ? extends V> entry = 
                    (Map.Entry<? extends K, ? extends V>) it.next();
            mcs.fireMapChange(entry.getKey(), entry.getValue(), null);
        }
    }
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }
    @Override
    public Collection<V> values() {
        return map.values();
    }
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        result = prime * result + ((mcs == null) ? 0 : mcs.hashCode());
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
        ObservableMap<?,?> other = (ObservableMap<?,?>) obj;
        if (map == null) {
            if (other.map != null) {
                return false;
            }
        } else if (!map.equals(other.map)) {
            return false;
        }
        if (mcs == null) {
            if (other.mcs != null) {
                return false;
            }
        } else if (!mcs.equals(other.mcs)) {
            return false;
        }
        return true;
    }
    @Override
    public String toString() {
        return "ObservableMap [mcs=" + mcs + ", map=" + map
                + "]";
    }

}
