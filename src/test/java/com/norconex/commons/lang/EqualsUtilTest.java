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
package com.norconex.commons.lang;

import static com.norconex.commons.lang.EqualsUtil.equalsAllIgnoreCase;
import static com.norconex.commons.lang.EqualsUtil.equalsAnyIgnoreCase;
import static com.norconex.commons.lang.EqualsUtil.equalsNoneIgnoreCase;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.MapUtil;

class EqualsUtilTest {

    @Test
    void testEqualsAny() {
        assertThat(EqualsUtil.equalsAny("a", "a", "b", "c")).isTrue();
        assertThat(EqualsUtil.equalsAny("d", "a", "b", "c")).isFalse();

        assertThat(EqualsUtil.equalsAny("a", (String) null)).isFalse();
        assertThat(EqualsUtil.equalsAny(null, (String) null)).isTrue();
        assertThat(EqualsUtil.equalsAny("a", (Object[]) null)).isFalse();

        assertThat(EqualsUtil.equalsAny("a")).isFalse();
        assertThat(EqualsUtil.equalsAny((String) null)).isFalse();
    }

    @Test
    void testEqualsAnyIgnoreCase() {
        assertThat(equalsAnyIgnoreCase("a", "a", "b", "c")).isTrue();
        assertThat(equalsAnyIgnoreCase("d", "a", "b", "c")).isFalse();
        assertThat(equalsAnyIgnoreCase("A", "a", "b", "c")).isTrue();

        assertThat(equalsAnyIgnoreCase("a", (String) null)).isFalse();
        assertThat(equalsAnyIgnoreCase(null, (String) null)).isTrue();
        assertThat(equalsAnyIgnoreCase("a", (CharSequence[]) null)).isFalse();

        assertThat(equalsAnyIgnoreCase("a")).isFalse();
        assertThat(equalsAnyIgnoreCase((String) null)).isFalse();
    }

    @Test
    void testEqualsAll() {
        assertThat(EqualsUtil.equalsAll("a", "a", "b", "c")).isFalse();
        assertThat(EqualsUtil.equalsAll("d", "a", "b", "c")).isFalse();
        assertThat(EqualsUtil.equalsAll("a", "a", "a", "a")).isTrue();
        assertThat(EqualsUtil.equalsAll("A", "a", "A", "a")).isFalse();

        assertThat(EqualsUtil.equalsAll("a", (String) null)).isFalse();
        assertThat(EqualsUtil.equalsAll(null, (String) null)).isTrue();
        assertThat(EqualsUtil.equalsAll("a", (Object[]) null)).isFalse();

        assertThat(EqualsUtil.equalsAll("a")).isFalse();
        assertThat(EqualsUtil.equalsAll((String) null)).isFalse();
    }

    @Test
    void testEqualsAllIgnoreCase() {
        assertThat(equalsAllIgnoreCase("a", "a", "b", "c")).isFalse();
        assertThat(equalsAllIgnoreCase("d", "a", "b", "c")).isFalse();
        assertThat(equalsAllIgnoreCase("a", "a", "a", "a")).isTrue();
        assertThat(equalsAllIgnoreCase("A", "a", "A", "a")).isTrue();

        assertThat(equalsAllIgnoreCase("a", (String) null)).isFalse();
        assertThat(equalsAllIgnoreCase(null, (String) null)).isTrue();
        assertThat(equalsAllIgnoreCase("a", (CharSequence[]) null)).isFalse();

        assertThat(equalsAllIgnoreCase("a")).isFalse();
        assertThat(equalsAllIgnoreCase((String) null)).isFalse();
    }

    @Test
    void testEqualsNone() {
        assertThat(EqualsUtil.equalsNone("a", "a", "b", "c")).isFalse();
        assertThat(EqualsUtil.equalsNone("d", "a", "b", "c")).isTrue();
        assertThat(EqualsUtil.equalsNone("a", "a", "a", "a")).isFalse();
        assertThat(EqualsUtil.equalsNone("A", "a", "A", "a")).isFalse();
        assertThat(EqualsUtil.equalsNone("A", "a", "a", "a")).isTrue();

        assertThat(EqualsUtil.equalsNone("a", (String) null)).isTrue();
        assertThat(EqualsUtil.equalsNone(null, (String) null)).isFalse();
        assertThat(EqualsUtil.equalsNone("a", (Object[]) null)).isTrue();

        assertThat(EqualsUtil.equalsNone("a")).isTrue();
        assertThat(EqualsUtil.equalsNone((String) null)).isTrue();
    }

    @Test
    void testEqualsNoneIgnoreCase() {
        assertThat(equalsNoneIgnoreCase("a", "a", "b", "c")).isFalse();
        assertThat(equalsNoneIgnoreCase("d", "a", "b", "c")).isTrue();
        assertThat(equalsNoneIgnoreCase("a", "a", "a", "a")).isFalse();
        assertThat(equalsNoneIgnoreCase("A", "a", "A", "a")).isFalse();
        assertThat(equalsNoneIgnoreCase("A", "a", "a", "a")).isFalse();

        assertThat(equalsNoneIgnoreCase("a", (String) null)).isTrue();
        assertThat(equalsNoneIgnoreCase(null, (String) null)).isFalse();
        assertThat(equalsNoneIgnoreCase("a", (CharSequence[]) null)).isTrue();

        assertThat(equalsNoneIgnoreCase("a")).isTrue();
        assertThat(equalsNoneIgnoreCase((String) null)).isTrue();
    }

    @Test
    void testEqualsMap() {
        assertThat(EqualsUtil.equalsMap(
                MapUtil.toMap("a", "1", "b", "2"),
                MapUtil.toMap("a", "1", "b", "2", "c", "3"))).isFalse();
        assertThat(EqualsUtil.equalsMap(
                MapUtil.toMap("a", "1", "b", "2"),
                MapUtil.toMap("a", "2", "b", "1"))).isFalse();
        assertThat(EqualsUtil.equalsMap(
                MapUtil.toMap("a", "1", "b", "2"),
                MapUtil.toMap("a", "1", "b", "2"))).isTrue();
        assertThat(EqualsUtil.equalsMap(
                MapUtil.toMap("a", "1", "b", "2"),
                MapUtil.toMap("b", "2", "a", "1"))).isTrue();
        assertThat(EqualsUtil.equalsMap(null, null)).isTrue();
    }
}
