/* Copyright 2017-2019 Norconex Inc.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircularRangeTest {

    @Test
    public void testCreationMethods() {
        // these should fail
        try {
            CircularRange.between(1, 5, 1, 6);
            Assertions.fail();
        } catch (Exception e) { }
        try {
            CircularRange.between(1, 5, 0, 5);
            Assertions.fail();
        } catch (Exception e) { }
        try {
            CircularRange.between(1, 5, 0, 6);
            Assertions.fail();
        } catch (Exception e) { }
        try {
            CircularRange.between(5, 1, 1, 5);
            Assertions.fail();
        } catch (Exception e) { }
        try {
            CircularRange.between(5, 1);
            Assertions.fail();
        } catch (Exception e) { }

        // these should succeed
        Assertions.assertNotNull(CircularRange.between(1, 5, 1, 5));
        Assertions.assertNotNull(CircularRange.between(1, 5, 5, 1));
        Assertions.assertNotNull(CircularRange.between(1, 5));
    }

    @Test
    public void testContains() {
        CircularRange<Integer> range = null;

        //Rolling range: e.g. from 10PM to 2AM.
        range = CircularRange.between(0, 23, 22, 2);
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
    public void testContainsRange() {
        CircularRange<Integer> range = null;

        // normal vs normal
        range = CircularRange.between(1, 10, 3, 8);
        Assertions.assertTrue(range.containsRange(range.withRange(3, 8)));
        Assertions.assertTrue(range.containsRange(range.withRange(4, 7)));
        Assertions.assertFalse(range.containsRange(range.withRange(1, 10)));
        Assertions.assertFalse(range.containsRange(range.withRange(2, 8)));
        Assertions.assertFalse(range.containsRange(range.withRange(3, 9)));
        Assertions.assertFalse(range.containsRange(range.withRange(2, 9)));

        // normal vs rolling
        range = CircularRange.between(1, 10, 3, 8);
        Assertions.assertFalse(range.containsRange(range.withRange(8, 3)));
        Assertions.assertFalse(range.containsRange(range.withRange(7, 4)));
        Assertions.assertFalse(range.containsRange(range.withRange(10, 1)));
        Assertions.assertFalse(range.containsRange(range.withRange(8, 2)));
        Assertions.assertFalse(range.containsRange(range.withRange(9, 3)));
        Assertions.assertFalse(range.containsRange(range.withRange(9, 2)));

        // rolling vs rolling
        range = CircularRange.between(1, 10, 8, 3);
        Assertions.assertTrue(range.containsRange(range.withRange(8, 3)));
        Assertions.assertFalse(range.containsRange(range.withRange(7, 4)));
        Assertions.assertTrue(range.containsRange(range.withRange(10, 1)));
        Assertions.assertTrue(range.containsRange(range.withRange(8, 2)));
        Assertions.assertTrue(range.containsRange(range.withRange(9, 3)));
        Assertions.assertTrue(range.containsRange(range.withRange(9, 2)));
        Assertions.assertFalse(range.containsRange(range.withRange(7, 2)));
        Assertions.assertFalse(range.containsRange(range.withRange(9, 4)));

        // rolling vs normal
        range = CircularRange.between(1, 10, 8, 3);
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
    public void testIsOverlappedBy() {
        CircularRange<Integer> range = null;

        // normal vs normal
        range = CircularRange.between(1, 10, 3, 8);
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(3, 8)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(4, 7)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(1, 10)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(2, 8)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(3, 9)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(2, 9)));
        Assertions.assertFalse(range.isOverlappedBy(range.withRange(1, 2)));
        Assertions.assertFalse(range.isOverlappedBy(range.withRange(9, 10)));

        // normal vs rolling
        range = CircularRange.between(1, 10, 3, 8);
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(8, 3)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(7, 4)));
        Assertions.assertFalse(range.isOverlappedBy(range.withRange(10, 1)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(8, 2)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(9, 3)));
        Assertions.assertFalse(range.isOverlappedBy(range.withRange(9, 2)));

        // rolling vs rolling
        range = CircularRange.between(1, 10, 8, 3);
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(8, 3)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(7, 4)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(10, 1)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(8, 2)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(9, 3)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(9, 2)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(7, 2)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(9, 4)));

        // rolling vs normal
        range = CircularRange.between(1, 10, 8, 3);
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(3, 8)));
        Assertions.assertFalse(range.isOverlappedBy(range.withRange(4, 7)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(1, 10)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(2, 8)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(3, 9)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(2, 9)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(9, 10)));
        Assertions.assertTrue(range.isOverlappedBy(range.withRange(1, 2)));
    }
}
