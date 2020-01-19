/* Copyright 2015-2020 Norconex Inc.
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>Convenient way of matching values for a key
 * in a given {@link Properties}.
 * </p>
 * <p>
 * A <code>null</code> or empty expression will try to match an
 * empty key value.
 * </p>
 * @author Pascal Essiembre
 * @since 1.8.0
 */
public final class PropertyMatcher implements Predicate<Properties> {
    private final TextMatcher matcher;
    private final String key;
    /**
     * Constructor.
     * @param key properties key
     * @param regex regular expression
     * @param caseSensitive <code>true</code> if case sensitive
     * @deprecated Since 2.0.0 use
     *             {@link #PropertyMatcher(String, TextMatcher)}.
     */
    @Deprecated
    public PropertyMatcher(
            String key, String regex, boolean caseSensitive) {
        this(key, new TextMatcher(Method.REGEX).setIgnoreCase(!caseSensitive));
    }

    /**
     * A property matcher matching empty or <code>null</code> elements.
     * @param key properties key
     */
    public PropertyMatcher(String key) {
        this(key, null);
    }
    /**
     * Constructor.
     * @param key properties key
     * @param matcher match instructions
     * @since 2.0.0
     */
    public PropertyMatcher(String key, TextMatcher matcher) {
        super();
        this.matcher = new TextMatcher(matcher);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
    public TextMatcher getTextMatcher() {
        return matcher;
    }

    /**
     * Gets the expression.
     * @return the expression.
     * @deprecated Since 2.0.0 use {@link #getTextMatcher()}.
     */
    @Deprecated
    public String getRegex() {
        return matcher.getPattern();
    }
    /**
     * Gets whether this matcher is case sensitive.
     * @return the expression.
     * @deprecated Since 2.0.0 use {@link #getTextMatcher()}.
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return !matcher.isIgnoreCase();
    }

    /**
     * Tests whether this property matcher matches at least one value for
     * the key in the given {@link Properties}. To get all matches instead,
     * use {@link #match(Properties)} instead.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the matcher's expression
     * @see #match(Properties)
     */
    public boolean matches(Properties properties) {
        if (properties == null) {
            return false;
        }
        TextMatcher m = new TextMatcher(matcher);
        Collection<String> values =  properties.getStrings(key);
        for (String value : values) {
            if (!matcher.hasPattern() && StringUtils.isBlank(value)) {
                return true;
            }
            m.setText(StringUtils.trimToEmpty(value));
            if (matcher.matches()) {
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
     * Returns a list of matching values for the key in the given
     * {@link Properties}. To simply get whether there is a match instead,
     * use the predicate {@link #test(Properties)} method instead.
     * @param properties the properties to look for a match
     * @return a list of matching values, or an empty list
     * @see #test(Properties)
     * @since 2.0.0
     */
    public List<String> match(Properties properties) {
        List<String> matches = new ArrayList<>();
        if (properties == null) {
            return matches;
        }
        TextMatcher m = new TextMatcher(matcher);
        Collection<String> values =  properties.getStrings(key);
        for (String value : values) {
            if (!matcher.hasPattern() && StringUtils.isBlank(value)) {
                matches.add(value);
            }
            m.setText(StringUtils.trimToEmpty(value));
            if (matcher.matches()) {
                matches.add(value);
            }
        }
        return matches;
    }

    /**
     * Replaces all matching values for the key in the given
     * {@link Properties} with the given replacement.
     * @param properties the properties to look for a match and replace
     * @return a list of original values that were replaced (or empty).
     * @since 2.0.0
     */
    public List<String> replace(Properties properties) {
        List<String> replacedValues = new ArrayList<>();
        if (properties == null) {
            return replacedValues;
        }
        TextMatcher m = new TextMatcher(matcher);
        Collection<String> values =  properties.getStrings(key);
        List<String> newValues = new ArrayList<>();
        for (String value : values) {
            m.setText(StringUtils.trimToEmpty(value));
            if (!matcher.hasPattern() && StringUtils.isBlank(m.getText())
                    || matcher.matches()) {
                String newValue = matcher.replace();
                if (!Objects.equals(value, newValue)) {
                    replacedValues.add(value);
                }
                newValues.add(newValue);
            } else {
                newValues.add(value);
            }
        }
        if (!replacedValues.isEmpty()) {
            properties.setList(key, newValues);
        }
        return replacedValues;
    }

    public static PropertyMatcher loadFromXML(XML xml) {
        TextMatcher m = new TextMatcher();
        m.loadFromXML(xml);
        return new PropertyMatcher(xml.getString("@field"), m);
    }
    public static void saveToXML(XML xml, PropertyMatcher matcher) {
        xml.setAttribute("field", matcher.key);
        matcher.getTextMatcher().saveToXML(xml);
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
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}