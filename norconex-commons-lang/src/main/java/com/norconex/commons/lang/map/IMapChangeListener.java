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

import java.util.Map;

/**
 * Listener for {@link Map} changes.
 * @author Pascal Essiembre
 * @since 1.4
 *
 * @param <K> the type of keys maintained by the map we are observing
 * @param <V> the type of mapped values
 */
public interface IMapChangeListener<K,V> {

    /**
     * The observed map has changed.
     * @param event change event
     */
    void mapChanged(MapChangeEvent<K, V> event);
}
