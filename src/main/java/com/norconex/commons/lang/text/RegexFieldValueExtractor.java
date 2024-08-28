/* Copyright 2017-2023 Norconex Inc.
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
package com.norconex.commons.lang.text;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertySetter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Simplify extraction of field/value pairs (or "key/value") from text using
 * regular expression.
 * Match groups can be used to identify the fields and values.
 * Field matching is optional and can be set explicitly instead. If
 * both a "toField" and a "fieldGroup" are provided, the toField act as a
 * default when no fields could be obtained from matching.
 * At least one of "toField" or "fieldGroup" must be specified. If fieldGroup
 * is specified without a "toField" and finds no matches, the matching
 * of the value is ignored.
 * If no value group is provided, it assumes the entire regex match is the
 * value.
 * If more than one value is extracted for a given toField, they will be
 * available as a list.
 * </p>
 * <p>
 * When initialized with a "pattern" only instead of passing or configuring
 * a {@link Regex} instance, a default one will be created, assuming
 * case insensitivity and dots matching any character.
 * </p>
 *
 * {@nx.xml.usage #attributes
 *     toField="(toField name)"
 *     fieldGroup="(toField name match group index)"
 *     valueGroup="(value match group index)"
 *     {@nx.include com.norconex.commons.lang.map.PropertySetter#attributes}
 *     {@nx.include com.norconex.commons.lang.text.Regex#attributes}
 * }
 *
 * <p>
 * The above are configurable attributes consuming classes can expect.
 * The actual regular expression is expected to be the tag content.
 * Many of the available attributes on XML configuration represent the
 * regular expression flags as defined in {@link Pattern}.
 * </p>
 *
 * {@nx.xml.example
 * <sampleConfig fieldGroup="1" valueGroup="2">
 *   (DocNo):(\d+)
 * </sampleConfig>
 * }
 * <p>
 * The above is configured to extract "DocNo" as the toField, and the following
 * numeric characters will make up the value.
 * </p>
 *
 * @since 2.0.0 (moved from Norconex Importer RegexKeyValueExtractor)
 */
@SuppressWarnings("javadoc")
@EqualsAndHashCode
@ToString
@Slf4j
public class RegexFieldValueExtractor {

    public static final RegexFieldValueExtractor[] EMPTY_ARRAY = {};

    private Regex regex;
    private String toField;
    private int fieldGroup = -1;
    private int valueGroup = -1;
    private PropertySetter onSet;

    public RegexFieldValueExtractor() {
        regex = defaultRegex(null);
    }

    public RegexFieldValueExtractor(String pattern) {
        this(defaultRegex(pattern), null);
    }

    public RegexFieldValueExtractor(String pattern, String field) {
        this(defaultRegex(pattern), field, -1);
    }

    public RegexFieldValueExtractor(
            String pattern, String field, int valueGroup) {
        this(defaultRegex(pattern), field, valueGroup);
    }

    public RegexFieldValueExtractor(
            String pattern, int fieldGroup, int valueGroup) {
        this(defaultRegex(pattern), fieldGroup, valueGroup);
    }

    public RegexFieldValueExtractor(Regex regex) {
        this(regex, null);
    }

    public RegexFieldValueExtractor(Regex regex, String field) {
        this(regex, field, -1);
    }

    public RegexFieldValueExtractor(Regex regex, String field, int valueGroup) {
        this.regex = regex;
        toField = field;
        this.valueGroup = valueGroup;
    }

    public RegexFieldValueExtractor(
            Regex regex, int fieldGroup, int valueGroup) {
        this.regex = regex;
        this.fieldGroup = fieldGroup;
        this.valueGroup = valueGroup;
    }

    public Regex getRegex() {
        return regex;
    }

    public RegexFieldValueExtractor setRegex(Regex regex) {
        Objects.requireNonNull(regex, "'regex' must not be null.");
        this.regex = regex;
        return this;
    }

    public int getFieldGroup() {
        return fieldGroup;
    }

    public RegexFieldValueExtractor setFieldGroup(int fieldGroup) {
        this.fieldGroup = fieldGroup;
        return this;
    }

    public int getValueGroup() {
        return valueGroup;
    }

    public RegexFieldValueExtractor setValueGroup(int valueGroup) {
        this.valueGroup = valueGroup;
        return this;
    }

    public String getToField() {
        return toField;
    }

    public RegexFieldValueExtractor setToField(String field) {
        toField = field;
        return this;
    }

    /**
     * Gets the property setter to use when a value is set.
     * @return property setter
     * @since 3.0.0
     */
    public PropertySetter getOnSet() {
        return onSet;
    }

    /**
     * Sets the property setter to use when a value is set.
     * @param onSet property setter
     * @return this instance
     * @since 3.0.0
     */
    public RegexFieldValueExtractor setOnSet(PropertySetter onSet) {
        this.onSet = onSet;
        return this;
    }

    public void extractFieldValues(Properties dest, CharSequence text) {
        if (StringUtils.isBlank(toField) && !hasFieldGroup()) {
            throw new IllegalArgumentException(
                    "At least one of 'toField' or 'fieldGroup' expected.");
        }

        var extractedFieldValues = new Properties();
        var m = matcher(text);
        while (m.find()) {
            var k = extractField(m);
            var v = extractValue(m);
            if (StringUtils.isBlank(k)) {
                LOG.debug("No toField for value: {}", v);
            } else if (v == null) {
                LOG.debug("Null value for toField: {}", k);
            } else {
                extractedFieldValues.add(k, v);
            }
        }
        for (Entry<String, List<String>> en : extractedFieldValues.entrySet()) {
            PropertySetter.orAppend(onSet).apply(
                    dest, en.getKey(), en.getValue());
        }
    }

    public Properties extractFieldValues(CharSequence text) {
        var dest = new Properties();
        extractFieldValues(dest, text);
        return dest;
    }

    public static void extractFieldValues(Properties dest,
            CharSequence text, List<RegexFieldValueExtractor> extractors) {
        if (extractors == null) {
            return;
        }
        for (RegexFieldValueExtractor extractor : extractors) {
            extractor.extractFieldValues(dest, text);
        }
    }

    public static Properties extractFieldValues(
            CharSequence text, List<RegexFieldValueExtractor> extractors) {
        var dest = new Properties();
        extractFieldValues(dest, text, extractors);
        return dest;
    }

    public static void extractFieldValues(Properties dest,
            CharSequence text, RegexFieldValueExtractor... extractors) {
        if (ArrayUtils.isEmpty(extractors)) {
            return;
        }
        extractFieldValues(dest, text, Arrays.asList(extractors));
    }

    public static Properties extractFieldValues(
            CharSequence text, RegexFieldValueExtractor... extractors) {
        var dest = new Properties();
        extractFieldValues(dest, text, extractors);
        return dest;
    }

    private static Regex defaultRegex(String pattern) {
        return new Regex(pattern).dotAll().ignoreCase();
    }

    private Matcher matcher(CharSequence text) {
        return regex.matcher(text);
    }

    private String extractField(Matcher m) {
        var f = StringUtils.isNotBlank(getToField()) ? getToField() : "";
        if (hasFieldGroup()) {
            if (m.groupCount() < getFieldGroup()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("""
                        No match group {} for toField in regex "{}"\
                        for match value "{}". Defaulting to toField:\s\
                        "{}".""",
                            getFieldGroup(), getRegex(), m.group(), toField);
                }
            } else {
                f = m.group(getFieldGroup());
            }
        }
        return f;
    }

    private String extractValue(Matcher m) {
        if (hasValueGroup()) {
            if (m.groupCount() >= getValueGroup()) {
                return m.group(getValueGroup());
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("""
                    No match group {}\s\
                    for value in regex "{}" \
                    for match value "{}".\s\
                    Defaulting to entire match.""",
                        getValueGroup(), getRegex(), m.group());
            }
        }
        return m.group();
    }

    private boolean hasFieldGroup() {
        return getFieldGroup() > -1;
    }

    private boolean hasValueGroup() {
        return getValueGroup() > -1;
    }
}
