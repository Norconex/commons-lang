/* Copyright 2018 Norconex Inc.
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Holds a collection of {@link PropertyMatcher}.  When evaluated, only
 * one of them needs to match.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
//TODO check if nulls are inserted and do not add if null
public class PropertyMatchers extends ArrayList<PropertyMatcher>
        implements Predicate<Properties> {

    private static final long serialVersionUID = 1L;

    public PropertyMatchers() {
        super();
    }

    /**
     * Creates and adds a {@link PropertyMatcher} for each regular expression
     * supplied.
     * @param property the property (e.g., key, field, ...) to match.
     * @param caseSensitive whether regular expression should be case sensitive
     * @param regexes one or more regular expressions for the given field
     */
    public void add(
            String property, boolean caseSensitive, String... regexes) {
        for (String regex : regexes) {
          add(new PropertyMatcher(property, regex, false));
        }
    }

    /**
     * Adds one or more property matchers.
     * @param matchers property matchers
     */
    public void addAll(PropertyMatcher... matchers) {
        addAll(Arrays.asList(matchers));
    }

    /**
     * Removes all matchers on a given field.
     * @param field the field to remove matchers on
     * @return how many matchers were removed
     */
    public synchronized int remove(String field) {
        Iterator<PropertyMatcher> it = iterator();
        int count = 0;
        while (it.hasNext()) {
            PropertyMatcher r = it.next();
            if (r.isCaseSensitive() && r.getKey().equals(field)
                    || !r.isCaseSensitive()
                            && r.getKey().equalsIgnoreCase(field)) {
                it.remove();
                count++;
            }
        }
        return count;
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
     * Returns <code>true</code> if any of the properties key and values
     * match any of the property matchers.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the list of matcher regular expressions
     */
    public boolean matches(Properties properties) {
        for (PropertyMatcher matcher : this) {
            if (matcher.matches(properties)) {
                return true;
            }
        }
        return false;
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
        return new ReflectionToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
