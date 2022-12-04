/* Copyright 2018-2022 Norconex Inc.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.norconex.commons.lang.collection.CollectionUtil;

/**
 * Holds a collection of {@link PropertyMatcher} to perform multiple matches
 * or replacements at once.
 * @since 2.0.0
 */
public class PropertyMatchers extends ArrayList<PropertyMatcher>
        implements Predicate<Properties> {

    private static final long serialVersionUID = 1L;

    /**
     * Adds one or more property matchers. A <code>null</code> array
     * as well as <code>null</code> array elements are not added.
     * @param matchers property matchers
     * @return <code>true</code> if this list chanced as a result of the call
     */
    public boolean addAll(PropertyMatcher... matchers) {
        if (matchers == null) {
            return false;
        }
        return addAll(Arrays.asList(matchers));
    }

    @Override
    public boolean addAll(Collection<? extends PropertyMatcher> matchers) {
        if (matchers == null) {
            return false;
        }
        List<PropertyMatcher> nullFree = new ArrayList<>(matchers);
        CollectionUtil.removeNulls(nullFree);
        return super.addAll(nullFree);
    }

    /**
     * Removes all matchers matching the given property field.
     * @param field the field to remove matchers on
     * @return how many matchers were removed
     */
    public synchronized int remove(String field) {
        Iterator<PropertyMatcher> it = iterator();
        int count = 0;
        while (it.hasNext()) {
            PropertyMatcher m = it.next();
            if (m.getFieldMatcher().matches(field)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * Returns <code>true</code> if any of the properties key and values
     * match any of the property matchers (only one property matcher needs
     * to match). Returns <code>false</code>
     * if the supplied argument is <code>null</code>.
     * @param properties the properties to look for a match
     * @return <code>true</code> if one property matcher matches.
     */
    public boolean matches(Properties properties) {
        if (properties == null) {
            return false;
        }
        for (PropertyMatcher matcher : this) {
            if (matcher.test(properties)) {
                return true;
            }
        }
        return false;
    }

    /**
     * For compatibility with {@link Predicate}.  Same as invoking
     * {@link #matches(Properties)}.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the list of matcher regular expressions
     */
    @Override
    public boolean test(Properties properties) {
        return matches(properties);
    }

    /**
     * Returns a new instance that is a subset of the given properties,
     * containing only keys and matching values.
     * Returns an empty <code>Properties</code> instance
     * if the supplied argument is <code>null</code>.
     * @param properties the properties to look for a match
     * @return a list of matching values, or an empty list
     * @see #test(Properties)
     */
    public Properties match(Properties properties) {
        Properties props = new Properties();
        if (properties == null) {
            return props;
        }
        for (PropertyMatcher matcher : this) {
            Properties matches = matcher.match(properties);
            if (!matches.isEmpty()) {
                props.putAll(matches);
            }
        }
        return props;
    }
    /**
     * Replaces all matching values of the matching keys in the given
     * {@link Properties} with the given replacement.
     * Returns properties that were replaced (or empty).
     * Returns an empty <code>Properties</code> instance
     * if the supplied argument is <code>null</code>.
     * @param properties the properties to look for a match and replace
     * @param replacement text replacement
     * @return properties that were replaced with their original values
     */
    public Properties replace(Properties properties, String replacement) {
        Properties props = new Properties();
        if (properties == null) {
            return props;
        }
        for (PropertyMatcher matcher : this) {
            Properties replaced = matcher.replace(properties, replacement);
            if (!replaced.isEmpty()) {
                props.putAll(replaced);
            }
        }
        return props;
    }
}
