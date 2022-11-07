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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;

class MapChangeSupportTest {

    @Test
    void testMapChangeSupport() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "aaa");

        MutableObject<String> key = new MutableObject<>();
        MutableObject<String> newValue = new MutableObject<>();
        MutableObject<String> oldValue = new MutableObject<>();

        IMapChangeListener<String, String> listener =
                event -> {
            key.setValue(event.getKey());
            newValue.setValue(event.getNewValue());
            oldValue.setValue(event.getOldValue());
        };

        MapChangeSupport<String, String> mcs = new MapChangeSupport<>(map);
        mcs.addMapChangeListener(listener);
        mcs.addMapChangeListener(null); // should have no effect on size
        mcs.fireMapChange("a", "aa", "aaa");

        assertThat(key.getValue()).isEqualTo("a");
        assertThat(oldValue.getValue()).isEqualTo("aa");
        assertThat(newValue.getValue()).isEqualTo("aaa");

        assertThat(mcs.getMapChangeListeners().size()).isOne();
        assertThat(mcs.getMapChangeListeners()).isNotEmpty();

        mcs.removeMapChangeListener(null);
        assertThat(mcs.getMapChangeListeners()).isNotEmpty();

        mcs.removeMapChangeListener(listener);
        assertThat(mcs.getMapChangeListeners()).isEmpty();

        mcs.addMapChangeListener(listener);
        assertThat(mcs.getMapChangeListeners()).isNotEmpty();
        mcs.clear();
        assertThat(mcs.isEmpty()).isTrue();
    }
}
