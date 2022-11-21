/* Copyright 2021-2022 Norconex Inc.
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

import java.util.function.Predicate;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A simple XML condition matching {@link Properties} keys and values.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
public class PropertyMatcherCondition
        implements Predicate<Properties>, IXMLConfigurable {

    private PropertyMatcher matcher;

    @Override
    public boolean test(Properties props) {
        return matcher.matches(props);
    }

    @Override
    public void loadFromXML(XML xml) {
        matcher = PropertyMatcher.loadFromXML(xml);
    }
    @Override
    public void saveToXML(XML xml) {
        PropertyMatcher.saveToXML(xml, matcher);
    }
}
