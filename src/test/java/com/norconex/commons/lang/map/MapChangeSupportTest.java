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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class MapChangeSupportTest {

    @Test
    void testMapChangeSupport() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "aaa");

        AtomicReference<String> key = new AtomicReference<>();
        AtomicReference<String> newValue = new AtomicReference<>();
        AtomicReference<String> oldValue = new AtomicReference<>();

        @SuppressWarnings("deprecation")
        IMapChangeListener<String, String> listener = //NOSONAR
                event -> {
                    key.set(event.getKey());
                    newValue.set(event.getNewValue());
                    oldValue.set(event.getOldValue());
                };

        MapChangeSupport<String, String> mcs = new MapChangeSupport<>(map);
        mcs.addMapChangeListener(listener);
        mcs.addMapChangeListener(null); // should have no effect on size
        mcs.fireMapChange("a", "aa", "aaa");

        assertThat(key.get()).isEqualTo("a");
        assertThat(oldValue.get()).isEqualTo("aa");
        assertThat(newValue.get()).isEqualTo("aaa");

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
