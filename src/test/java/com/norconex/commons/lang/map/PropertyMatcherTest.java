/* Copyright 2022 Norconex Inc.
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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;

class PropertyMatcherTest {

    private Properties sampleProps;

    @BeforeEach
    void beforeEach() {
        sampleProps = new Properties(MapUtil.toMap(
            "a", asList("1", "2", "3"),
            "b", asList("4", "5", "6"),
            "abc", asList("7", "8", "9")
        ));
    }

    @Test
    void testReplace() {
        PropertyMatcher pm = new PropertyMatcher(
                TextMatcher.regex("a|b"),
                TextMatcher.regex("5|9"));

        Properties replacedOrig = pm.replace(null, "0");
        assertThat(replacedOrig).isEmpty();
        assertThat(sampleProps).isEqualTo(new Properties(MapUtil.toMap(
                "a", asList("1", "2", "3"),
                "b", asList("4", "5", "6"),
                "abc", asList("7", "8", "9")
        )));

        replacedOrig = pm.replace(sampleProps, "X");
        assertThat(replacedOrig).isEqualTo(
                new Properties(MapUtil.toMap("b", asList("5"))));
        assertThat(sampleProps).isEqualTo(new Properties(MapUtil.toMap(
                "a", asList("1", "2", "3"),
                "b", asList("4", "X", "6"),
                "abc", asList("7", "8", "9")
        )));

        pm = new PropertyMatcher(null, TextMatcher.regex("2|3|4"));
        replacedOrig = pm.replace(sampleProps, "Y");
        assertThat(replacedOrig).isEqualTo(new Properties(MapUtil.toMap(
                "a", asList("2", "3"),
                "b", asList("4")
        )));
        assertThat(sampleProps).isEqualTo(new Properties(MapUtil.toMap(
                "a", asList("1", "Y", "Y"),
                "b", asList("Y", "X", "6"),
                "abc", asList("7", "8", "9"))
        ));

        pm = new PropertyMatcher(TextMatcher.basic("abc"));
        replacedOrig = pm.replace(sampleProps, "Z");
        assertThat(replacedOrig).isEqualTo(new Properties(MapUtil.toMap(
                "abc", asList("7", "8", "9"))));
        assertThat(sampleProps).isEqualTo(new Properties(MapUtil.toMap(
                "a", asList("1", "Y", "Y"),
                "b", asList("Y", "X", "6"),
                "abc", asList("Z", "Z", "Z")
        )));

        pm = new PropertyMatcher(null, null);
        replacedOrig = pm.replace(sampleProps, "0");
        assertThat(replacedOrig).isEqualTo(new Properties(MapUtil.toMap(
                "a", asList("1", "Y", "Y"),
                "b", asList("Y", "X", "6"),
                "abc", asList("Z", "Z", "Z")
        )));
        assertThat(sampleProps).isEqualTo(new Properties(MapUtil.toMap(
                "a", asList("0", "0", "0"),
                "b", asList("0", "0", "0"),
                "abc", asList("0", "0", "0"))
        ));
    }

    @Test
    void testMatches() {
        PropertyMatcher pm = new PropertyMatcher(
                TextMatcher.regex("a|b"),
                TextMatcher.regex("5|9"));
        assertThat(pm.matches(sampleProps)).isTrue();
        assertThat(pm.match(sampleProps)).isEqualTo(
                MapUtil.toMap("b", Arrays.asList("5")));

        pm = new PropertyMatcher(TextMatcher.regex("d"));
        assertThat(pm.test(sampleProps)).isFalse();
        assertThat(pm.matches(null)).isFalse();
        assertThat(pm.match(null)).isEmpty();
    }

    @Test
    void testWithNullMatchers() {
        PropertyMatcher pm = new PropertyMatcher(null, null);
        assertThat(pm.matches(sampleProps)).isTrue();
        assertThat(pm.match(sampleProps)).isEqualTo(sampleProps);
        assertThat(pm.getFieldMatcher()).isNull();
        assertThat(pm.getValueMatcher()).isNull();

        pm = new PropertyMatcher(null, TextMatcher.regex("3|4"));
        assertThat(pm.matches(sampleProps)).isTrue();
        assertThat(pm.match(sampleProps)).isEqualTo(
            new Properties(MapUtil.toMap(
                    "a", asList("3"),
                    "b", asList("4"))
        ));
        assertThat(pm.getFieldMatcher()).isNull();
        assertThat(pm.getValueMatcher()).isNotNull();

        pm = new PropertyMatcher(TextMatcher.regex("a|b"));
        assertThat(pm.matches(sampleProps)).isTrue();
        assertThat(pm.match(sampleProps)).isEqualTo(
            new Properties(MapUtil.toMap(
                    "a", asList("1", "2", "3"),
                    "b", asList("4", "5", "6"))
        ));
        assertThat(pm.getFieldMatcher()).isNotNull();
        assertThat(pm.getValueMatcher()).isNull();
    }

    @Test
    void testXmlSaveLoad() {
        PropertyMatcher pmBefore = new PropertyMatcher(
                TextMatcher.regex("a|b"),
                TextMatcher.regex("5|9"));
        XML xml = new XML("xml");
        PropertyMatcher.saveToXML(xml, pmBefore);
        PropertyMatcher pmAfter = PropertyMatcher.loadFromXML(xml);
        assertThat(pmAfter).isEqualTo(pmBefore);

        pmBefore = new PropertyMatcher(null, TextMatcher.regex("5|9"));
        xml = new XML("xml");
        PropertyMatcher.saveToXML(xml, pmBefore);
        pmAfter = PropertyMatcher.loadFromXML(xml);
        assertThat(pmAfter).isEqualTo(pmBefore);

        pmBefore = new PropertyMatcher(TextMatcher.regex("a|b"), null);
        xml = new XML("xml");
        PropertyMatcher.saveToXML(xml, pmBefore);
        pmAfter = PropertyMatcher.loadFromXML(xml);
        assertThat(pmAfter).isEqualTo(pmBefore);

        pmBefore = new PropertyMatcher(null, null);
        xml = new XML("xml");
        PropertyMatcher.saveToXML(xml, pmBefore);
        pmAfter = PropertyMatcher.loadFromXML(xml);
        assertThat(pmAfter).isEqualTo(pmBefore);

        assertThat(PropertyMatcher.loadFromXML(null)).isNull();
        assertDoesNotThrow(() -> PropertyMatcher.saveToXML(null, null));
    }
}
