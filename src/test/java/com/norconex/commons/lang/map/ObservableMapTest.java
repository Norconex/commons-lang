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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;

class ObservableMapTest {

    @Test
    void testObservableMap() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "aa");
        map.put("b", "bb");

        ObservableMap<String, String> obsMap = new ObservableMap<>(map);

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
        obsMap.addMapChangeListener(listener);

        obsMap.put("a", "aaa");

        assertThat(key.get()).isEqualTo("a");
        assertThat(oldValue.get()).isEqualTo("aa");
        assertThat(newValue.get()).isEqualTo("aaa");

        assertThat(obsMap)
                .hasSize(2)
                .isNotEmpty()
                .containsKeys("a", "b")
                .containsValues("aaa", "bb")
                .containsEntry("a", "aaa")
                .hasToString("ObservableMap [map={a=aaa, b=bb}]");

        assertThat(obsMap.keySet()).containsExactly("a", "b");
        assertThat(obsMap.values()).containsExactly("aaa", "bb");
        assertThat(obsMap.entrySet()).containsExactly(
                ImmutablePair.of("a", "aaa"),
                ImmutablePair.of("b", "bb"));
        assertThat(obsMap.remove("b")).isEqualTo("bb");
        assertThat(obsMap.size()).isOne();
        assertThat(key.get()).isEqualTo("b");
        assertThat(oldValue.get()).isEqualTo("bb");
        assertThat(newValue.get()).isNull();

        obsMap.clear();
        assertThat(obsMap).isEmpty();
        assertThat(key.get()).isEqualTo("a");
        assertThat(oldValue.get()).isEqualTo("aaa");
        assertThat(newValue.get()).isNull();

        obsMap.clearMapChangeListeners();
        obsMap.removeMapChangeListener(null);
        assertThat(obsMap.getMapChangeListeners()).isEmpty();

        obsMap.addMapChangeListener(listener);
        obsMap.putAll(MapUtil.toMap("c", "cc"));
        assertThat(key.get()).isEqualTo("c");
        assertThat(oldValue.get()).isNull();
        assertThat(newValue.get()).isEqualTo("cc");
    }
}
