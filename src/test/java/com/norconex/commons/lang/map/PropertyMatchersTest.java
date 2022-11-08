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

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.text.TextMatcher;

class PropertyMatchersTest {

    @Test
    void testPropertyMatchers() {

        Properties sampleProps = new Properties(MapUtil.toMap(
            "a", asList("1", "2", "3"),
            "b", asList("4", "5", "6"),
            "abc", asList("7", "8", "9")
        ));

        PropertyMatcher pm1 = new PropertyMatcher(
                TextMatcher.regex("a|b"),
                TextMatcher.regex("5|9"));
        PropertyMatcher pm2 = new PropertyMatcher(TextMatcher.basic("abc"));
        PropertyMatchers pms = new PropertyMatchers();
        pms.addAll(pm1, pm2);

        assertThat(pms.matches(sampleProps)).isTrue();
        assertThat(pms.test(sampleProps)).isTrue();
        assertThat(pms.match(sampleProps)).isEqualTo(
            new Properties(MapUtil.toMap(
                "b", asList("5"),
                "abc", asList("7", "8", "9")
            ))
        );

        assertThat(pms.addAll((PropertyMatcher[]) null)).isFalse();
        assertThat(pms.addAll((Collection<PropertyMatcher>) null)).isFalse();
        assertThat(pms.matches(null)).isFalse();
        assertThat(pms.match(null)).isEmpty();

        assertThat(pms.remove("abc")).isOne();
        assertThat(pms.size()).isOne();

        assertThat(pms.matches(new Properties())).isFalse();

        assertThat(pms.replace(sampleProps, "0")).isEqualTo(
                new Properties(MapUtil.toMap("b", asList("5"))));
        assertThat(sampleProps).isEqualTo(
            new Properties(MapUtil.toMap(
                "a", asList("1", "2", "3"),
                "b", asList("4", "0", "6"),
                "abc", asList("7", "8", "9")
            ))
        );

        assertThat(pms.replace(null, "0")).isEmpty();
    }
}
