/* Copyright 2018-2022 Norconex Inc.
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
package com.norconex.commons.lang.bean;

import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.event.Event;
import com.norconex.commons.lang.event.IEventListener;


/**
 * Bean utility methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
class BeanUtilTest2 {

    @Test
    void testToMap() {
        var map = BeanUtil.toMap(new Bean());
        Assertions.assertEquals("potato", map.get("string"));
        Assertions.assertEquals(123, map.get("primitiveInt"));
        Assertions.assertEquals(Integer.valueOf(456), map.get("objectInteger"));
        Assertions.assertEquals(Event.builder(
                "testEvent", "test").build(), map.get("event"));
        Assertions.assertEquals(Arrays.asList(0.5d, 1.0d), map.get("doubles"));
        Assertions.assertEquals(5, map.size());
    }

    @Test
    void testToProperties() {
        var props = BeanUtil.toProperties(new Bean(), "event");
        Assertions.assertEquals("potato", props.getString("string"));
        Assertions.assertEquals("123", props.getString("primitiveInt"));
        Assertions.assertEquals("456", props.getString("objectInteger"));
        Assertions.assertEquals(
                Arrays.asList("0.5", "1.0"), props.getStrings("doubles"));
        Assertions.assertEquals(4, props.size());
    }

    @Test
    void testDoVisit() {
        var root = new Root();
        Assertions.assertEquals(3, BeanUtil.getChildren(root).size());
        Assertions.assertEquals(3,
                BeanUtil.find(root, IEventListener.class).size());
    }

    @Test
    void testDoVisitProperties() {
        var root = new Root();
        var cnt = new MutableInt();
        BeanUtil.visitAllProperties(root, (obj, pd) -> {
            cnt.increment();
        });
        Assertions.assertEquals(5, cnt.intValue());

    }

}
