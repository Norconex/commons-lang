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

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>Convenient way of matching values and/or fields (key)
 * in a given {@link Properties}.
 * </p>
 * <h3>Null handling:</h3>
 * <p>
 * Since {@link Properties} does not store <code>null</code> values,
 * a <code>null</code> pattern for the field or value matcher will
 * instead match anything.  If {@link Properties} itself is <code>null</code>,
 * it is considered non-matching.
 * </p>
 * @author Pascal Essiembre
 * @since 1.8.0
 * @see TextMatcher
 */
public final class PropertyMatcher implements Predicate<Properties> {
    private final TextMatcher valueMatcher = new TextMatcher();
    private final TextMatcher fieldMatcher = new TextMatcher();
    /**
     * Constructor.
     * @param field properties key
     * @param regex regular expression
     * @param caseSensitive <code>true</code> if case sensitive
     * @deprecated Since 2.0.0 use
     *             {@link #PropertyMatcher(TextMatcher, TextMatcher)}.
     */
    @Deprecated
    public PropertyMatcher(
            String field, String regex, boolean caseSensitive) {
        this(TextMatcher.basic(field),
                new TextMatcher(Method.REGEX).setIgnoreCase(!caseSensitive));
    }

    /**
     * A property matcher matching empty or <code>null</code> elements.
     * @param field properties field name to match as-is
     * @deprecated Since 2.0.0 use
     *             {@link #PropertyMatcher(TextMatcher, TextMatcher)}.
     */
    @Deprecated
    public PropertyMatcher(String field) {
        this(TextMatcher.basic(field), null);
    }

    /**
     * A property matcher matching empty or <code>null</code> elements.
     * @param fieldMatcher field match instructions
     * @since 2.0.0
     */
    public PropertyMatcher(TextMatcher fieldMatcher) {
        this(fieldMatcher, null);
    }
    /**
     * Constructor.
     * @param fieldMatcher field match instructions
     * @param valueMatcher value match instructions
     * @since 2.0.0
     */
    public PropertyMatcher(TextMatcher fieldMatcher, TextMatcher valueMatcher) {
        super();
        this.fieldMatcher.copyFrom(fieldMatcher);
        this.valueMatcher.copyFrom(valueMatcher);
    }

    /**
     * Gets the value matcher (copy).
     * @return value matcher
     * @since 2.0.0
     */
    public TextMatcher getValueMatcher() {
        return new TextMatcher(valueMatcher);
    }
    /**
     * Gets the field matcher (copy).
     * @return field matcher
     * @since 2.0.0
     */
    public TextMatcher getFieldMatcher() {
        return new TextMatcher(fieldMatcher);
    }

    /**
     * Gets the expression.
     * @return the expression.
     * @deprecated Since 2.0.0 use {@link #getValueMatcher()}.
     */
    @Deprecated
    public String getRegex() {
        return valueMatcher.getPattern();
    }
    /**
     * Gets whether this valueMatcher is case sensitive.
     * @return the expression.
     * @deprecated Since 2.0.0 use {@link #getValueMatcher()}.
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return !valueMatcher.isIgnoreCase();
    }
    /**
     * Gets the field name being matched.
     * @return the field name
     * @deprecated Since 2.0.0 use {@link #getFieldMatcher()}.
     */
    @Deprecated
    public String getKey() {
        return fieldMatcher.getPattern();
    }

    /**
     * Tests whether this property valueMatcher matches at least one value for
     * the key in the given {@link Properties}. To get all matches instead,
     * use {@link #match(Properties)} instead.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the valueMatcher's expression
     * @see #match(Properties)
     */
    public boolean matches(Properties properties) {
        if (properties == null) {
            return false;
        }

        boolean keyFound = false;
        for (Entry<String, List<String>> en : properties.entrySet()) {
            String field = en.getKey();

            // reject if field pattern is NOT null and does not match
            if (fieldMatcher.getPattern() != null
                    && !fieldMatcher.matches(field)) {
                continue;
            }

            keyFound = true;

            // matches value pattern if not null
            if (valueMatcher.getPattern() != null) {
                List<String> values = en.getValue();
                for (String value : values) {
                    if (valueMatcher.matches(value)) {
                        return true;
                    }
                }
            }
        }

        // if no key was found, we treat as equivalent to having
        // been there with a null value.
        if (!keyFound && valueMatcher.getPattern() == null) {
            return true;
        }

        return false;
    }

    /**
     * For compatibility with {@link Predicate}.  Same as invoking
     * {@link #matches(Properties)}.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the list of valueMatcher regular expressions
     */
    @Override
    public boolean test(Properties properties) {
        return matches(properties);
    }

    /**
     * Returns matching field/values in the given
     * {@link Properties}. To simply get whether there is a match instead,
     * use the predicate {@link #test(Properties)} method instead.
     * @param properties the properties to look for a match
     * @return a list of matching values, or an empty list
     * @see #test(Properties)
     * @since 2.0.0
     */
    public Properties match(Properties properties) {
        Properties matches = new Properties();
        if (properties == null) {
            return matches;
        }

        for (Entry<String, List<String>> en : properties.entrySet()) {
            String field = en.getKey();

            // reject if field pattern is NOT null and does not match
            if (fieldMatcher.getPattern() != null
                    && !fieldMatcher.matches(field)) {
                continue;
            }

            // matches if value pattern is null or matches
            List<String> values = en.getValue();
            for (String value : values) {
                if (valueMatcher.getPattern() == null
                        || valueMatcher.matches(value)) {
                    matches.add(field, value);
                }
            }
        }
        return matches;
    }

    /**
     * Replaces all matching values for the key in the given
     * {@link Properties} with the given replacement.
     * @param properties the properties to look for a match and replace
     * @param replacement text replacement
     * @return original fields/values that were replaced (or empty).
     * @since 2.0.0
     */
    public Properties replace(Properties properties, String replacement) {
        Properties replacedValues = new Properties();
        if (properties == null) {
            return replacedValues;
        }

        Properties newValues = new Properties();
        for (Entry<String, List<String>> en : properties.entrySet()) {
            String field = en.getKey();

            // skip if field pattern is NOT null and does not match
            if (fieldMatcher.getPattern() != null
                    && !fieldMatcher.matches(field)) {
                continue;
            }

            // matches if value pattern is null or matches
            List<String> values = en.getValue();
            for (String value : values) {
                if (valueMatcher.getPattern() == null
                        || valueMatcher.matches(value)) {
                    String newValue = valueMatcher.replace(value, replacement);
                    if (!Objects.equals(value, newValue)) {
                        replacedValues.add(field, value);
                    }
                    newValues.add(field, newValue);
                } else {
                    newValues.add(field, value);
                }
            }
        }
        if (!replacedValues.isEmpty()) {
            for (Entry<String, List<String>> en : properties.entrySet()) {
                properties.setList(en.getKey(), en.getValue());
            }
        }
        return replacedValues;
    }

    public static PropertyMatcher loadFromXML(XML xml) {
        TextMatcher fieldMatcher = new TextMatcher();
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
        TextMatcher valueMatcher = new TextMatcher();
        valueMatcher.loadFromXML(xml.getXML("valueMatcher"));
        return new PropertyMatcher(fieldMatcher, valueMatcher);
    }
    public static void saveToXML(XML xml, PropertyMatcher matcher) {
        matcher.getFieldMatcher().saveToXML(xml.addElement("fieldMatcher"));
        matcher.getValueMatcher().saveToXML(xml.addElement("valueMatcher"));
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