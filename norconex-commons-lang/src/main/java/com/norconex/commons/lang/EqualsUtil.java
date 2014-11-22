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
package com.norconex.commons.lang;

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
            String source, String... targets) {
        if (targets == null) {
            return source == null;
        }
        for (String target : targets) {
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
            String source, String... targets) {
        if (targets == null) {
            return source == null;
        }
        for (String target : targets) {
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
}
