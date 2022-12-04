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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;

class ObservableMapTest {

    @Test
    void testObservableMap() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "aa");
        map.put("b", "bb");

        ObservableMap<String, String> obsMap = new ObservableMap<>(map);

        MutableObject<String> key = new MutableObject<>();
        MutableObject<String> newValue = new MutableObject<>();
        MutableObject<String> oldValue = new MutableObject<>();

        @SuppressWarnings("deprecation")
        IMapChangeListener<String, String> listener = //NOSONAR
                event -> {
            key.setValue(event.getKey());
            newValue.setValue(event.getNewValue());
            oldValue.setValue(event.getOldValue());
        };
        obsMap.addMapChangeListener(listener);

        obsMap.put("a", "aaa");

        assertThat(key.getValue()).isEqualTo("a");
        assertThat(oldValue.getValue()).isEqualTo("aa");
        assertThat(newValue.getValue()).isEqualTo("aaa");

        assertThat(obsMap)
            .hasSize(2)
            .isNotEmpty()
            .containsKeys("a", "b")
            .containsValues("aaa", "bb")
            .containsEntry("a", "aaa")
            .hasToString("ObservableMap [map={a=aaa, b=bb}]")
            ;

        assertThat(obsMap.keySet()).containsExactly("a", "b");
        assertThat(obsMap.values()).containsExactly("aaa", "bb");
        assertThat(obsMap.entrySet()).containsExactly(
                ImmutablePair.of("a", "aaa"),
                ImmutablePair.of("b", "bb"));
        assertThat(obsMap.remove("b")).isEqualTo("bb");
        assertThat(obsMap.size()).isOne();
        assertThat(key.getValue()).isEqualTo("b");
        assertThat(oldValue.getValue()).isEqualTo("bb");
        assertThat(newValue.getValue()).isNull();

        obsMap.clear();
        assertThat(obsMap).isEmpty();
        assertThat(key.getValue()).isEqualTo("a");
        assertThat(oldValue.getValue()).isEqualTo("aaa");
        assertThat(newValue.getValue()).isNull();

        obsMap.clearMapChangeListeners();
        obsMap.removeMapChangeListener(null);
        assertThat(obsMap.getMapChangeListeners()).isEmpty();

        obsMap.addMapChangeListener(listener);
        obsMap.putAll(MapUtil.toMap("c", "cc"));
        assertThat(key.getValue()).isEqualTo("c");
        assertThat(oldValue.getValue()).isNull();
        assertThat(newValue.getValue()).isEqualTo("cc");
    }
}
