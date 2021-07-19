/* Copyright 2018-2020 Norconex Inc.
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
import java.util.Iterator;
import java.util.function.Predicate;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Holds a collection of {@link PropertyMatcher} to perform tests/replace on
 * all of them at once.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
//TODO check if nulls are inserted and do not add if null?
public class PropertyMatchers extends ArrayList<PropertyMatcher>
        implements Predicate<Properties> {

    private static final long serialVersionUID = 1L;

    public PropertyMatchers() {
        super();
    }

    /**
     * Adds one or more property matchers.
     * @param matchers property matchers
     */
    public void addAll(PropertyMatcher... matchers) {
        addAll(Arrays.asList(matchers));
    }

    /**
     * Removes all matchers for a given field.
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
     * match any of the property matchers.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the list of matcher regular expressions
     */
    public boolean matches(Properties properties) {
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
     * Returns properties that were replaced (or empty).
     * @param properties the properties to look for a match and replace
     * @param replacement text replacement
     * @return properties that were replaced
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
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
