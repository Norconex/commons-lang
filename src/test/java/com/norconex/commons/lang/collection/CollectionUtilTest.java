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
package com.norconex.commons.lang.collection;

import static com.norconex.commons.lang.collection.CollectionUtil.adaptedList;
import static com.norconex.commons.lang.collection.CollectionUtil.adaptedSet;
import static com.norconex.commons.lang.collection.CollectionUtil.toArray;
import static com.norconex.commons.lang.collection.CollectionUtil.toTypeList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.ibm.icu.math.BigDecimal;
import com.norconex.commons.lang.map.MapUtil;

class CollectionUtilTest {

    @Test
    void testAdaptedList() {
        assertThat(adaptedList(null)).isEmpty();
        assertThat(adaptedList("   ")).isEmpty();
        assertThat(adaptedList(Arrays.asList("1", "2"))).hasSize(2);
        assertThat(adaptedList(new String[] {"1", "2", "3"})).hasSize(3);
        assertThat(adaptedList(BigDecimal.valueOf(4))).hasSize(1);
    }

    @Test
    void testAdaptedSet() {
        assertThat(adaptedSet(null)).isEmpty();
        assertThat(adaptedSet("   ")).isEmpty();
        assertThat(adaptedSet(Arrays.asList("1", "2"))).hasSize(2);
        assertThat(adaptedSet(new String[] {"1", "2", "3"})).hasSize(3);
        assertThat(adaptedSet(BigDecimal.valueOf(4))).hasSize(1);
    }

    @Test
    void testToArray() {
        assertThat(toArray(null, String.class)).isEqualTo(new String[] {});
        List<String> list = Arrays.asList("1", "2");
        assertThrows(NullPointerException.class, () -> toArray(list, null));
        assertThat(toArray(Arrays.asList("1", "2"), String.class))
            .isEqualTo(new String[] {"1", "2"});
    }

    @Test
    void testSetAllCollection() {
        List<String> target;

        // null target
        target = null;
        CollectionUtil.setAll(target, Arrays.asList("1", "2"));
        assertThat(target).isNull();

        // empty target
        target = new ArrayList<>();
        CollectionUtil.setAll(target, Arrays.asList("1", "2"));
        assertThat(target).containsExactly("1", "2");

        // source and target have the same entries
        target = new ArrayList<>(Arrays.asList("1", "2"));
        var source = new ArrayList<>(Arrays.asList("1", "2"));
        CollectionUtil.setAll(target, source);
        assertThat(target).containsExactly("1", "2");

        // source adds some to target
        target = new ArrayList<>(Arrays.asList("1", "2"));
        CollectionUtil.setAll(target, Arrays.asList("2", "3"));
        assertThat(target).containsExactly("2", "3");

        // source is null
        target = new ArrayList<>(Arrays.asList("1", "2"));
        CollectionUtil.setAll(target, (Collection<String>) null);
        assertThat(target).isEmpty();
    }

    @Test
    void testSetAllArray() {
        List<String> target;

        // null target
        target = null;
        CollectionUtil.setAll(target, "1", "2");
        assertThat(target).isNull();

        // empty target
        target = new ArrayList<>();
        CollectionUtil.setAll(target, "1", "2");
        assertThat(target).containsExactly("1", "2");

        // source and target are the same entries
        target = new ArrayList<>(Arrays.asList("1", "2"));
        var source = new String[] {"1", "2"};
        CollectionUtil.setAll(target, source);
        assertThat(target).containsExactly("1", "2");

        // source adds some to target
        target = new ArrayList<>(Arrays.asList("1", "2"));
        CollectionUtil.setAll(target, "2", "3");
        assertThat(target).containsExactly("2", "3");

        // source is null
        target = new ArrayList<>(Arrays.asList("1", "2"));
        CollectionUtil.setAll(target, (String[]) null);
        assertThat(target).isEmpty();
    }

    @Test
    void testSetAllMap() {
        Map<String, String> target;

        // null target
        target = null;
        CollectionUtil.setAll(target, MapUtil.toMap("k1", "v1", "k2", "v2"));
        assertThat(target).isNull();

        // empty target
        target = new TreeMap<>();
        CollectionUtil.setAll(target, MapUtil.toMap("k1", "v1", "k2", "v2"));
        assertThat(target).containsExactlyEntriesOf(
                MapUtil.toMap("k1", "v1", "k2", "v2"));

        // source and target are the same entries
        target = MapUtil.toMap("k1", "v1", "k2", "v2");
        CollectionUtil.setAll(target, MapUtil.toMap("k1", "v1", "k2", "v2"));
        assertThat(target).containsExactlyEntriesOf(
                MapUtil.toMap("k1", "v1", "k2", "v2"));

        // source and target are different
        target = MapUtil.toMap("k1", "v1", "k2", "v2");
        CollectionUtil.setAll(target, MapUtil.toMap("k2", "v2", "k3", "v3"));
        assertThat(target).containsExactlyEntriesOf(
                MapUtil.toMap("k2", "v2", "k3", "v3"));

        // source is null
        target = MapUtil.toMap("k1", "v1", "k2", "v2");
        CollectionUtil.setAll(target, (Map<String, String>) null);
        assertThat(target).isEmpty();
    }

    @Test
    void testAsListOrEmpty() {
        assertThat(CollectionUtil.asListOrEmpty("1", "2")).hasSize(2);
        assertThat(CollectionUtil.asListOrEmpty((String[]) null)).isEmpty();
    }

    @Test
    void testAsListOrNull() {
        assertThat(CollectionUtil.asListOrNull("1", "2")).hasSize(2);
        assertThat(CollectionUtil.asListOrNull((String[]) null)).isNull();
    }

    @Test
    void testToStringListObjectArray() {
        assertThat(CollectionUtil.toStringList((Object[]) null)).isEmpty();
        assertThat(CollectionUtil.toStringList(
                11,
                Duration.ofSeconds(22),
                Locale.CANADA_FRENCH,
                StandardCharsets.UTF_8,
                true))
            .containsExactly("11", "22000", "fr_CA", "UTF-8", "true");
    }

    @Test
    void testToStringListCollection() {
        assertThat(CollectionUtil.toStringList(
                (Collection<Object>) null)).isEmpty();
        assertThat(CollectionUtil.toStringList(Arrays.asList(
                11,
                Duration.ofSeconds(22),
                Locale.CANADA_FRENCH,
                StandardCharsets.UTF_8,
                true)))
            .containsExactly("11", "22000", "fr_CA", "UTF-8", "true");
    }

    @Test
    void testToTypeList() {
        assertThat(toTypeList((List<String>) null, s -> "blah")).isEmpty();
        assertThat(toTypeList(Arrays.asList("1000", "2000", "3000"),
                s -> Duration.ofMillis(Long.parseLong(s))))
            .containsExactly(
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(3));
    }

    @Test
    void testUnmodifiableList() {
        List<String> list1 = CollectionUtil.unmodifiableList((String[]) null);
        assertThat(list1).isEmpty();
        assertThrows(UnsupportedOperationException.class, () -> list1.add("1"));

        List<String> list2 = CollectionUtil.unmodifiableList("1", "2");
        assertThat(list2).hasSize(2);
        assertThrows(UnsupportedOperationException.class, () -> list2.add("3"));
    }

    @Test
    void testUnmodifiableSet() {
        Set<String> set1 = CollectionUtil.unmodifiableSet((String[]) null);
        assertThat(set1).isEmpty();
        assertThrows(UnsupportedOperationException.class, () -> set1.add("1"));

        Set<String> set2 = CollectionUtil.unmodifiableSet("1", "2");
        assertThat(set2).hasSize(2);
        assertThrows(UnsupportedOperationException.class, () -> set2.add("3"));
    }

    @Test
    void testRemoveNulls() {
        List<String> list =
                new ArrayList<>(Arrays.asList("1", null, "2", null));
        CollectionUtil.removeNulls(list);
        assertThat(list).containsExactly("1", "2");

        assertDoesNotThrow(() -> CollectionUtil.removeNulls(null));
    }

    @Test
    void testRemoveBlanks() {
        List<String> list = new ArrayList<>(Arrays.asList("1", " ", "2", " "));
        CollectionUtil.removeBlanks(list);
        assertThat(list).containsExactly("1", "2");

        assertDoesNotThrow(() -> CollectionUtil.removeBlanks(null));
    }

    @Test
    void testRemoveEmpties() {
        List<String> list = new ArrayList<>(Arrays.asList("1", "", "2", ""));
        CollectionUtil.removeEmpties(list);
        assertThat(list).containsExactly("1", "2");

        assertDoesNotThrow(() -> CollectionUtil.removeEmpties(null));
    }

    @Test
    void testReplaceAll() {
        List<String> list = new ArrayList<>(Arrays.asList("1", "2", "2", "3"));
        CollectionUtil.replaceAll(list, "2", "4");
        assertThat(list).containsExactly("1", "4", "4", "3");

        assertDoesNotThrow(() -> CollectionUtil.replaceAll(null, "1", "2"));
    }

    @Test
    void testNullsToEmpties() {
        List<String> list = new ArrayList<>(
                Arrays.asList("1", "", null, "  ", null));
        CollectionUtil.nullsToEmpties(list);
        assertThat(list).containsExactly("1", "", "", "  ", "");

        assertDoesNotThrow(() -> CollectionUtil.nullsToEmpties(null));
    }

    @Test
    void testEmptiesToNulls() {
        List<String> list = new ArrayList<>(
                Arrays.asList("1", "", null, "  ", ""));
        CollectionUtil.emptiesToNulls(list);
        assertThat(list).containsExactly("1", null, null, "  ", null);

        assertDoesNotThrow(() -> CollectionUtil.emptiesToNulls(null));
    }

    @Test
    void testBlanksToNulls() {
        List<String> list = new ArrayList<>(
                Arrays.asList("1", "", null, "  ", "     "));
        CollectionUtil.blanksToNulls(list);
        assertThat(list).containsExactly("1", null, null, null, null);

        assertDoesNotThrow(() -> CollectionUtil.blanksToNulls(null));
    }

}
