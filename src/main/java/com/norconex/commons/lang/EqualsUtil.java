/* Copyright 2010-2017 Norconex Inc.
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
package com.norconex.commons.lang;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Convenience methods related to object equality.
 * @author Pascal Essiembre
 */
public final class EqualsUtil {

    private EqualsUtil() {
        super();
    }

    /**
     * Whether a source object equals ANY of the target objects.
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if any of the target objects are equal
     */
    public static boolean equalsAny(Object source, Object... targets) {
        if (targets == null) {
            return source == null;
        }
        for (Object object : targets) {
            if (Objects.equals(source, object)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Whether a source string equals ANY of the target string.
     * @param source string being tested for equality with target strings
     * @param targets one or more strings to be tested with source string 
     *                for equality
     * @return <code>true</code> if any of the target strings are equal
     */
    public static boolean equalsAnyIgnoreCase(
            CharSequence source, CharSequence... targets) {
        if (targets == null) {
            return source == null;
        }
        for (CharSequence target : targets) {
            if (StringUtils.equalsIgnoreCase(source, target)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Whether a source object equals ALL of the target objects
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if all of the target objects are equal
     */
    public static boolean equalsAll(Object source, Object... targets) {
        if (targets == null) {
            return source == null;
        }
        for (Object object : targets) {
            if (!Objects.equals(source, object)) {
                return false;
            }
        }
        return true;
    }
    /**
     * Whether a source string equals ALL of the target string.
     * @param source string being tested for equality with target strings
     * @param targets one or more strings to be tested with source string 
     *                for equality
     * @return <code>true</code> if all of the target strings are equal
     */
    public static boolean equalsAllIgnoreCase(
            CharSequence source, CharSequence... targets) {
        if (targets == null) {
            return source == null;
        }
        for (CharSequence target : targets) {
            if (!StringUtils.equalsIgnoreCase(source, target)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a source object equals NONE of the target objects
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if none of the target objects are equal
     */
    public static boolean equalsNone(Object source, Object... targets) {
        return !equalsAny(source, targets);
    }
    /**
     * Whether a source string equals NONE of the target string.
     * @param source string being tested for equality with target strings
     * @param targets one or more strings to be tested with source string 
     *                for equality
     * @return <code>true</code> if none of the target strings are equal
     */
    public static boolean equalsNoneIgnoreCase(
            String source, String... targets) {
        return !equalsAnyIgnoreCase(source, targets);
    }
    
    /**
     * Compares that two maps are equals, regardless of entry orders.
     * It does so by making sure the two maps are the same size
     * and the first map contains all entries of the other one. 
     * Map elements should implement <code>equals</code>.
     * @param map1 first map 
     * @param map2 second map
     * @return <code>true</code> if the two maps are equal
     * @since 1.14.0
     */
    public static boolean equalsMap(Map<?, ?> map1, Map<?,?> map2) {
        if (map1 == null && map2 == null) {
            return true;
        }
        if (map1 == null || map2 == null) {
            return false;
        }
        if (map1.size() != map2.size()) {
            return false;
        }
        return map1.entrySet().containsAll(map2.entrySet());
    }
}
