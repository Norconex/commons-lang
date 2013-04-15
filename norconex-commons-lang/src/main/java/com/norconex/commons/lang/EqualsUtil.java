/* Copyright 2010-2013 Norconex Inc.
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
package com.norconex.commons.lang;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Convenience methods related to object equality.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public final class EqualsUtil {

    private EqualsUtil() {
        super();
    }

    /**
     * Whether a source object equals ANY of the target objects
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if any of the target objects is equal
     */
    public static boolean equalsAny(Object source, Object... targets) {
        if (targets == null) {
            return source == null;
        }
        for (Object object : targets) {
            if (ObjectUtils.equals(source, object)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Whether a source object equals ALL of the target objects
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if all of the target objects is equal
     */
    public static boolean equalsAll(Object source, Object... targets) {
        if (targets == null) {
            return source == null;
        }
        for (Object object : targets) {
            if (!ObjectUtils.equals(source, object)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Whether a source object equals NONE of the target objects
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if none of the target objects is equal
     */
    public static boolean equalsNone(Object source, Object... targets) {
        return !equalsAny(source, targets);
    }
}
