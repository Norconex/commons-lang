/* Copyright 2017-2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CircularRangeTest {

    @Test
    void testCreationMethods() {
        Assertions.assertNotNull(CircularRange.between(1, 5, 1, 5));
        Assertions.assertNotNull(CircularRange.between(1, 5, 5, 1));
        Assertions.assertNotNull(CircularRange.between(1, 5));
    }

    @Test
    void testContains() {
        CircularRange<Integer> range = CircularRange.between(0, 23, 22, 2);

        Assertions.assertFalse(range.contains(21));
        Assertions.assertTrue(range.contains(22));
        Assertions.assertTrue(range.contains(23));
        Assertions.assertTrue(range.contains(0));
        Assertions.assertTrue(range.contains(1));
        Assertions.assertTrue(range.contains(2));
        Assertions.assertFalse(range.contains(3));

        //Normal range: e.g. from 2AM to 4AM.
        range = CircularRange.between(0, 23, 2, 4);
        Assertions.assertFalse(range.contains(0));
        Assertions.assertFalse(range.contains(1));
        Assertions.assertTrue(range.contains(2));
        Assertions.assertTrue(range.contains(3));
        Assertions.assertTrue(range.contains(4));
        Assertions.assertFalse(range.contains(5));
        Assertions.assertFalse(range.contains(6));
    }

    @Test
    void testContainsRangeNormalVsNormal() {
        CircularRange<Integer> range = CircularRange.between(1, 10, 3, 8);
        Assertions.assertTrue(range.containsRange(range.withRange(3, 8)));
        Assertions.assertTrue(range.containsRange(range.withRange(4, 7)));
        Assertions.assertFalse(range.containsRange(range.withRange(1, 10)));
        Assertions.assertFalse(range.containsRange(range.withRange(2, 8)));
        Assertions.assertFalse(range.containsRange(range.withRange(3, 9)));
        Assertions.assertFalse(range.containsRange(range.withRange(2, 9)));
    }

    @Test
    void testContainsRangeNormalVsRolling() {
        CircularRange<Integer> range = CircularRange.between(1, 10, 3, 8);
        Assertions.assertFalse(range.containsRange(range.withRange(8, 3)));
        Assertions.assertFalse(range.containsRange(range.withRange(7, 4)));
        Assertions.assertFalse(range.containsRange(range.withRange(10, 1)));
        Assertions.assertFalse(range.containsRange(range.withRange(8, 2)));
        Assertions.assertFalse(range.containsRange(range.withRange(9, 3)));
        Assertions.assertFalse(range.containsRange(range.withRange(9, 2)));
    }

    @Test
    void testContainsRangeRollingVsRolling() {
        CircularRange<Integer> range = CircularRange.between(1, 10, 8, 3);
        Assertions.assertTrue(range.containsRange(range.withRange(8, 3)));
        Assertions.assertFalse(range.containsRange(range.withRange(7, 4)));
        Assertions.assertTrue(range.containsRange(range.withRange(10, 1)));
        Assertions.assertTrue(range.containsRange(range.withRange(8, 2)));
        Assertions.assertTrue(range.containsRange(range.withRange(9, 3)));
        Assertions.assertTrue(range.containsRange(range.withRange(9, 2)));
        Assertions.assertFalse(range.containsRange(range.withRange(7, 2)));
        Assertions.assertFalse(range.containsRange(range.withRange(9, 4)));
    }

    @Test
    void testContainsRangeRollingVsNormal() {
        CircularRange<Integer> range = CircularRange.between(1, 10, 8, 3);
        Assertions.assertFalse(range.containsRange(range.withRange(3, 8)));
        Assertions.assertFalse(range.containsRange(range.withRange(4, 7)));
        Assertions.assertFalse(range.containsRange(range.withRange(1, 10)));
        Assertions.assertFalse(range.containsRange(range.withRange(2, 8)));
        Assertions.assertFalse(range.containsRange(range.withRange(3, 9)));
        Assertions.assertFalse(range.containsRange(range.withRange(2, 9)));
        Assertions.assertTrue(range.containsRange(range.withRange(9, 10)));
        Assertions.assertTrue(range.containsRange(range.withRange(1, 2)));
    }

    @Test
    void testIs() {
        assertThat(CircularRange.is(9).getMinimum()).isEqualTo(9);
        assertThat(CircularRange.is(9).getMaximum()).isEqualTo(9);
        assertThat(CircularRange.is(9).contains(8)).isFalse();
        assertThat(CircularRange.is(9).contains(9)).isTrue();
        assertThat(CircularRange.is(9).contains(10)).isFalse();
    }

    @Test
    void testIsComparator() {
        Comparator<Integer> c = Comparator.reverseOrder();
        assertThat(CircularRange.is(9, c).getMinimum()).isEqualTo(9);
        assertThat(CircularRange.is(9, c).getMaximum()).isEqualTo(9);
        assertThat(CircularRange.is(9, c).contains(8)).isFalse();
        assertThat(CircularRange.is(9, c).contains(9)).isTrue();
        assertThat(CircularRange.is(9, c).contains(10)).isFalse();
    }

    @Test
    void testWithCurcularBoundaries() {
        CircularRange<Integer> r = CircularRange.between(0, 100, 20, 80);
        assertThat(r.withCircularBoundaries(10, 90))
                .isEqualTo(CircularRange.between(10, 90, 20, 80));
    }

    @Test
    void testIsNaturalOrdering() {
        assertThat(CircularRange.between(0, 100,
                Integer::compare)
                .isNaturalOrdering()).isFalse();
        assertThat(CircularRange.between(0, 100)
                .isNaturalOrdering()).isTrue();
    }

    @Test
    void testIsStartedBy() {
        assertThat(CircularRange.between(10, 20).isStartedBy(null)).isFalse();
        assertThat(CircularRange.between(10, 20).isStartedBy(10)).isTrue();
        assertThat(CircularRange.between(10, 20).isStartedBy(20)).isFalse();
        assertThat(CircularRange.between(10, 20).isStartedBy(9)).isFalse();
        assertThat(CircularRange.between(10, 20).isStartedBy(11)).isFalse();
    }

    @Test
    void testIsEndedBy() {
        assertThat(CircularRange.between(10, 20).isEndedBy(null)).isFalse();
        assertThat(CircularRange.between(10, 20).isEndedBy(10)).isFalse();
        assertThat(CircularRange.between(10, 20).isEndedBy(20)).isTrue();
        assertThat(CircularRange.between(10, 20).isEndedBy(19)).isFalse();
        assertThat(CircularRange.between(10, 20).isEndedBy(21)).isFalse();
    }

    @Test
    void testIsOverlappedBy() {
        CircularRange<Integer> c = CircularRange.between(10, 20);
        assertThat(c.isOverlappedBy(null)).isFalse();
        assertThat(c.isOverlappedBy(CircularRange.between(19, 30))).isTrue();
        assertThat(c.isOverlappedBy(CircularRange.between(20, 30))).isTrue();
        assertThat(c.isOverlappedBy(CircularRange.between(21, 30))).isFalse();
        assertThat(c.isOverlappedBy(CircularRange.between(1, 9))).isFalse();
        assertThat(c.isOverlappedBy(CircularRange.between(1, 10))).isTrue();
        assertThat(c.isOverlappedBy(CircularRange.between(1, 11))).isTrue();
        assertThat(c.isOverlappedBy(CircularRange.between(1, 30))).isTrue();
        assertThat(c.isOverlappedBy(CircularRange.between(12, 18))).isTrue();
        assertThat(c.isOverlappedBy(CircularRange.is(15))).isTrue();
        assertThat(c.isOverlappedBy(CircularRange.is(5))).isFalse();
        assertThat(c.isOverlappedBy(CircularRange.is(25))).isFalse();
    }

    @Test
    void testToString() {
        assertThat(CircularRange.between(10, 20, 12, 18))
                .hasToString("[12..18](10..20)");
    }

    @Test
    void testToStringFormat() {
        assertThat(CircularRange.between(10, 20, 12, 18)
                .toString("%3$s-%4$s|%1$s-%2$s"))
                        .isEqualTo("10-20|12-18");
    }

    @Test
    void testNullsAndErrors() {
        assertThat(CircularRange.between(0, 100).contains(null)).isFalse();
        assertThat(CircularRange.between(0, 100).containsRange(null)).isFalse();

        assertThrows(IllegalArgumentException.class,
                () -> CircularRange.between(1, 5, 1, 6));
        assertThrows(IllegalArgumentException.class,
                () -> CircularRange.between(1, 5, 0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> CircularRange.between(1, 5, 0, 6));
        assertThrows(IllegalArgumentException.class,
                () -> CircularRange.between(5, 1, 1, 5));
        assertThrows(IllegalArgumentException.class,
                () -> CircularRange.between(5, 1));

        assertThrows(IllegalArgumentException.class,
                () -> CircularRange.between(null, 10, 3, 8));
        assertThrows(IllegalArgumentException.class,
                () -> CircularRange.between(1, 10, null, 8));
    }
}
