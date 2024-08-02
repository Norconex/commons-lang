/* Copyright 2021-2023 Norconex Inc.
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
package com.norconex.commons.lang.xml.flow.impl;

import java.util.Optional;
import java.util.function.Predicate;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.XMLConfigurable;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A simple XML condition matching {@link Properties} keys and values.
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
public class PropertyMatcherCondition
        implements Predicate<Properties>, XMLConfigurable {

    private PropertyMatcher matcher;

    @Override
    public boolean test(Properties props) {
        return matcher.matches(props);
    }

    @Override
    public void loadFromXML(XML xml) {
        if (xml == null || xml.isEmpty()) {
            return;
        }
        TextMatcher fieldMatcher = null;
        var fieldMatcherXML = xml.getXML("fieldMatcher");
        if (fieldMatcherXML != null && !fieldMatcherXML.isEmpty()) {
            fieldMatcher = new TextMatcher();
            loadMatcherFromXML(fieldMatcherXML, fieldMatcher);
        }
        TextMatcher valueMatcher = null;
        var valueMatcherXML = xml.getXML("valueMatcher");
        if (valueMatcherXML != null && !valueMatcherXML.isEmpty()) {
            valueMatcher = new TextMatcher();
            loadMatcherFromXML(valueMatcherXML, valueMatcher);
        }
        matcher = new PropertyMatcher(fieldMatcher, valueMatcher);
    }
    @Override
    public void saveToXML(XML xml) {
        if (xml == null || matcher == null) {
            return;
        }
        var fmXml = xml.addElement("fieldMatcher");
        Optional.ofNullable(matcher.getFieldMatcher())
                .ifPresent(m -> saveMatcherToXML(fmXml, m));
        var vmXml = xml.addElement("valueMatcher");
        Optional.ofNullable(matcher.getValueMatcher())
                .ifPresent(m -> saveMatcherToXML(vmXml, m));
    }

    private void loadMatcherFromXML(XML xml, TextMatcher matcher) {
        if (xml == null) {
            return;
        }
        matcher.setMethod(xml.getEnum(
                "@method", Method.class, matcher.getMethod()));
        matcher.setIgnoreCase(xml.getBoolean(
                "@ignoreCase", matcher.isIgnoreCase()));
        matcher.setIgnoreDiacritic(xml.getBoolean(
                "@ignoreDiacritic", matcher.isIgnoreDiacritic()));
        matcher.setReplaceAll(xml.getBoolean(
                "@replaceAll", matcher.isReplaceAll()));
        matcher.setPartial(xml.getBoolean("@partial", matcher.isPartial()));
        matcher.setTrim(xml.getBoolean("@trim", matcher.isTrim()));
        matcher.setMatchEmpty(xml.getBoolean(
                "@matchEmpty", matcher.isMatchEmpty()));
        matcher.setPattern(xml.getString("."));
    }
    private void saveMatcherToXML(XML xml, TextMatcher matcher) {
        if (xml == null) {
            return;
        }
        xml.setAttribute("method", matcher.getMethod());
        xml.setAttribute("ignoreCase", matcher.isIgnoreCase());
        xml.setAttribute("ignoreDiacritic", matcher.isIgnoreDiacritic());
        xml.setAttribute("replaceAll", matcher.isReplaceAll());
        xml.setAttribute("partial", matcher.isPartial());
        xml.setAttribute("trim", matcher.isTrim());
        xml.setAttribute("matchEmpty", matcher.isMatchEmpty());
        xml.setTextContent(matcher.getPattern());
    }







}
