/* Copyright 2017-2020 Norconex Inc.
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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>
 * Simplify extraction of field/value pairs (or "field/value") from text using
 * regular expression.
 * Match groups can be used to identify the fields and values.
 * Field matching is optional and can be set explicitly instead. If
 * both a field and a field group are provided, the field act as a
 * default when no fields could be obtained from matching.
 * At least one of "field" or "fieldGroup" must be specified. If fieldGroup
 * is specified without a "field" and finds no matches, the matching
 * of the value is ignored.
 * If no value group is provided, it assumes the entire regex match is the
 * value.
 * If more than one value is extracted for a given field, they will be
 * available as a list.
 * </p>
 * <p>
 * When initialized with a "pattern" only instead of passing or configuring
 * a {@link Regex} instance, a default one will be created, assuming
 * case insensitivity and dots matching any character.
 * </p>
 *
 * {@nx.xml.usage #attributes
 *     field="(field name)"
 *     fieldGroup="(field name match group index)"
 *     valueGroup="(value match group index)"
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
 * The above is configured to extract "DocNo" as the field, and the following
 * numeric characters will make up the value.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0 (moved from Norconex Importer RegexKeyValueExtractor)
 */
@SuppressWarnings("javadoc")
public class RegexFieldValueExtractor implements IXMLConfigurable {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegexFieldValueExtractor.class);

    public static final RegexFieldValueExtractor[] EMPTY_ARRAY =
            new RegexFieldValueExtractor[] {};

    private Regex regex;
    private String field;
    private int fieldGroup = -1;
    private int valueGroup = -1;
    private PropertySetter onSet;

    public RegexFieldValueExtractor() {
        super();
        this.regex = defaultRegex(null);
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
        this.field = field;
        this.valueGroup = valueGroup;
    }
    public RegexFieldValueExtractor(
            Regex regex, int fieldGroup, int valueGroup) {
        super();
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
    public String getField() {
        return field;
    }
    public RegexFieldValueExtractor setField(String field) {
        this.field = field;
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
        if (StringUtils.isBlank(field) && !hasFieldGroup()) {
            throw new IllegalArgumentException(
                    "At least one of 'field' or 'fieldGroup' expected.");
        }

        Properties extractedFieldValues = new Properties();
        Matcher m = matcher(text);
        while (m.find()) {
            String k = extractField(m);
            String v = extractValue(m);
            if (StringUtils.isBlank(k)) {
                LOG.debug("No field for value: {}", v);
            } else if (v == null) {
                LOG.debug("Null value for field: {}", k);
            } else {
                extractedFieldValues.add(k, v);
            }
        }
        for (Entry<String,List<String>> en : extractedFieldValues.entrySet()) {
            PropertySetter.orDefault(onSet).apply(
                    dest, en.getKey(), en.getValue());
        }
    }
    public Properties extractFieldValues(CharSequence text) {
        Properties dest = new Properties();
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
        Properties dest = new Properties();
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
        Properties dest = new Properties();
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
        String f = StringUtils.isNotBlank(getField()) ? getField() : "";
        if (hasFieldGroup()) {
            if (m.groupCount() < getFieldGroup()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No match group {} for field in regex \"{}\""
                            + "for match value \"{}\". Defaulting to field: "
                            + "\"{}\".",
                            getFieldGroup(), getRegex(), m.group(), field);
                }
            } else {
                f = m.group(getFieldGroup());
            }
        }
        return f;
    }
    private String extractValue(Matcher m) {
        if (hasValueGroup()) {
            if (m.groupCount() < getValueGroup()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No match group {} "
                            + "for value in regex \"{}\" "
                            + "for match value \"{}\". "
                            + "Defaulting to entire match.",
                            getValueGroup(), getRegex(), m.group());
                }
            } else {
                return m.group(getValueGroup());
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

    @Override
    public void loadFromXML(XML xml) {
        setField(xml.getString("@field", getField()));
        setFieldGroup(xml.getInteger("@fieldGroup", getFieldGroup()));
        setValueGroup(xml.getInteger("@valueGroup", getValueGroup()));
        if (regex == null) {
            regex = new Regex();
        }
        regex.loadFromXML(xml);
        setOnSet(PropertySetter.fromXML(xml, getOnSet()));
    }
    @Override
    public void saveToXML(XML xml) {
        xml.setAttribute("field", getField());
        xml.setAttribute("fieldGroup", getFieldGroup());
        xml.setAttribute("valueGroup", getValueGroup());
        if (regex != null) {
            regex.saveToXML(xml);
        }
        PropertySetter.toXML(xml, onSet);
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
