/* Copyright 2015-2022 Norconex Inc.
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
import java.util.Optional;
import java.util.function.Predicate;

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>Convenient way of matching values and/or fields (key)
 * in a given {@link Properties}.
 * </p>
 * @since 1.8.0
 * @see TextMatcher
 */
@ToString
@EqualsAndHashCode
public final class PropertyMatcher implements Predicate<Properties> {
    private final TextMatcher valueMatcher;
    private final TextMatcher fieldMatcher;

    /**
     * A property matcher that will only match present fields, regardless of
     * values.  Use {@link #PropertyMatcher(TextMatcher, TextMatcher)} to
     * match both fields and values.
     * @param fieldMatcher field matcher
     * @since 2.0.0
     */
    public PropertyMatcher(TextMatcher fieldMatcher) {
        this(fieldMatcher, null);
    }
    /**
     * <p>
     * A property matcher matching both fields and values. Only a single value
     * of multi-valued fields need to match to generate a positive match
     * on values.
     * </p>
     * <p>
     * Either or both the field matcher and value matcher can be
     * <code>null</code>. A <code>null</code> matcher is equivalent
     * to matching fields or value, respectively.
     * </p>
     *
     * @param fieldMatcher field matcher
     * @param valueMatcher value matcher
     * @since 2.0.0
     */
    public PropertyMatcher(TextMatcher fieldMatcher, TextMatcher valueMatcher) {
        this.fieldMatcher =
                fieldMatcher == null ? null : new TextMatcher(fieldMatcher);
        this.valueMatcher =
                valueMatcher == null ? null : new TextMatcher(valueMatcher);
    }

    /**
     * Gets the value matcher (copy) or <code>null</code> if the value matcher
     * is <code>null</code>.
     * @return value matcher
     * @since 2.0.0
     */
    public TextMatcher getValueMatcher() {
        return valueMatcher == null ? null : new TextMatcher(valueMatcher);
    }
    /**
     * Gets the field matcher (copy) or <code>null</code> if the field matcher
     * is <code>null</code>.
     * @return field matcher
     * @since 2.0.0
     */
    public TextMatcher getFieldMatcher() {
        return fieldMatcher == null ? null : new TextMatcher(fieldMatcher);
    }

    /**
     * Tests whether this property matcher matches at least one value for
     * the key in the given {@link Properties}. To get all matches instead,
     * use {@link #match(Properties)}.
     * <code>null</code> text field or value matchers are considered to
     * be matching all fields or values, respectively.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the matcher's expression
     * @see #match(Properties)
     */
    public boolean matches(Properties properties) {
        if (properties == null) {
            return false;
        }
        if (fieldMatcher == null && valueMatcher == null) {
            return true;
        }

        for (Entry<String, List<String>> en : properties.entrySet()) {
            var field = en.getKey();

            // reject if field pattern is NOT null and does not match
            if (!matches(fieldMatcher, field)) {
                continue;
            }

            if (valueMatcher == null || valueMatcher.getPattern() == null) {
                return true;
            }

            // matches value pattern if not null
            for (String value : en.getValue()) {
                if (valueMatcher.matches(value)) {
                    return true;
                }
            }
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
     * Returns matching field/values in the given {@link Properties}.
     * For multi-valued fields, only the matching values are returned.
     * <code>null</code> text field or value matchers are considered to
     * be matching all fields or values, respectively.
     * To simply get whether there is a match instead,
     * use the predicate {@link #test(Properties)} method instead.
     * @param properties the properties to look for a match
     * @return a list of matching values, or an empty list
     *     (never <code>null</code>)
     * @see #test(Properties)
     * @since 2.0.0
     */
    public Properties match(Properties properties) {
        var matches = new Properties();
        if (properties == null) {
            return matches;
        }

        if (fieldMatcher == null && valueMatcher == null) {
            matches.loadFromMap(properties);
            return matches;
        }

        for (Entry<String, List<String>> en : properties.entrySet()) {
            var field = en.getKey();

            // reject if field pattern is NOT null and does not match
            if (!matches(fieldMatcher, field)) {
                continue;
            }

            // matches if value pattern is null or matches
            var values = en.getValue();
            for (String value : values) {
                if (matches(valueMatcher, value)) {
                    matches.add(field, value);
                }
            }
        }
        return matches;
    }

    /**
     * Replaces all matching values of the matching keys in the given
     * {@link Properties} with the given replacement.
     * If this property matcher has a <code>null</code> field matcher,
     * it will replace all matching values, for all fields.
     * If it has a <code>null</code> value matcher, it will replace all
     * values of matching fields.  If both are <code>null</code>,
     * all values will be replaced for all keys.
     * @param properties the properties to look for a match and replace
     * @param replacement text replacement
     * @return original fields/values that were replaced (or empty).
     * @since 2.0.0
     */
    public Properties replace(Properties properties, String replacement) {
        var replacedValues = new Properties();
        if (properties == null) {
            return replacedValues;
        }

        var newValues = new Properties();
        for (Entry<String, List<String>> en : properties.entrySet()) {
            var field = en.getKey();

            // skip if field pattern is NOT null and does not match
            if (!matches(fieldMatcher, field)) {
                continue;
            }

            // matches if value pattern is null or matches
            for (String value : en.getValue()) {
                var newValue = replaceValue(value, replacement);
                if (!Objects.equals(value, newValue)) {
                    replacedValues.add(field, value);
                }
                newValues.add(field, newValue);
            }
        }
        for (Entry<String, List<String>> en : newValues.entrySet()) {
            properties.setList(en.getKey(), en.getValue());
        }
        return replacedValues;
    }


    // return new value
    private String replaceValue(String value, String replacement) {
        var newValue = value;
        if (matches(valueMatcher, value)) {
            newValue = valueMatcher == null
                    ? replacement
                    : valueMatcher.replace(value, replacement);
        }
        return newValue;
    }

    private static boolean matches(TextMatcher matcher, String str) {
        return matcher == null || matcher.getPattern() == null
                || matcher.matches(str);
    }

    public static PropertyMatcher loadFromXML(XML xml) {
        if (xml == null || xml.isEmpty()) {
            return null;
        }
        TextMatcher fieldMatcher = null;
        var fieldMatcherXML = xml.getXML("fieldMatcher");
        if (fieldMatcherXML != null && !fieldMatcherXML.isEmpty()) {
            fieldMatcher = new TextMatcher();
            fieldMatcher.loadFromXML(fieldMatcherXML);
        }
        TextMatcher valueMatcher = null;
        var valueMatcherXML = xml.getXML("valueMatcher");
        if (valueMatcherXML != null && !valueMatcherXML.isEmpty()) {
            valueMatcher = new TextMatcher();
            valueMatcher.loadFromXML(valueMatcherXML);
        }
        return new PropertyMatcher(fieldMatcher, valueMatcher);
    }
    public static void saveToXML(XML xml, PropertyMatcher matcher) {
        if (xml == null || matcher == null) {
            return;
        }

        var fmXml = xml.addElement("fieldMatcher");
        Optional.ofNullable(matcher.getFieldMatcher())
                .ifPresent(m -> m.saveToXML(fmXml));
        var vmXml = xml.addElement("valueMatcher");
        Optional.ofNullable(matcher.getValueMatcher())
                .ifPresent(m -> m.saveToXML(vmXml));
    }
}