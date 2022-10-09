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
package com.norconex.commons.lang.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

class EventManagerTest {

    @Test
    void testListenerAccessors() {
        EventManager em = new EventManager();

        // test they added by identity...
        TestEventListener tel1 = new TestEventListener("pepper");
        TestEventListener tel2 = tel1; // <!-- should not be added/counted
        TestEventListener tel3 = new TestEventListener("pepper");

        em.addListener(tel1);
        em.addListeners(Arrays.asList(tel3, tel2, tel1));
        em.addListener(null);
        em.addListeners(null);

        assertThat(em.getListenerCount()).isEqualTo(2);
        assertThat(em.getListeners()).hasSize(2);

        // by identity or null, should not be removed.
        em.removeListener(new TestEventListener("pepper"));
        em.removeListener(null);
        em.removeListeners(null);
        assertThat(em.getListeners()).hasSize(2);

        // only one should match by identity and be removed
        em.removeListeners(Arrays.asList(
                new TestEventListener("pepper"), tel2));
        assertThat(em.getListeners()).hasSize(1);

        // clear
        em.clearListeners();
        assertThat(em.getListeners()).isEmpty();
    }

    @Test
    void testAddListenerFromScan() {
        EventManager em = new EventManager();
        em.addListenersFromScan(new TestObject());
        assertThat(em.getListeners()).hasSize(1);
        assertThat(((TestEventListener) em.getListeners().get(0)).getSalt())
            .isEqualTo("found me");
    }

    @Test
    void testFire() {
        TestEventListener tel = new TestEventListener();

        EventManager em = new EventManager();
        em.addListener(tel);
        em.fire(Event.builder("eventTest", "someSource").build());

        assertThat(tel.getEvent().getName()).isEqualTo("eventTest");
    }

    @Test
    void testFireOnChild() {
        TestEventListener childListener = new TestEventListener("child");
        TestEventListener parentListener = new TestEventListener("parent");

        EventManager parentEM = new EventManager();
        parentEM.addListener(parentListener);

        EventManager childEM = new EventManager(parentEM);
        childEM.addListener(childListener);

        // should be in both child and parent
        childEM.fire(Event.builder("inBoth", "someSource").build());
        assertThat(parentListener.getEvent().getName()).isEqualTo("inBoth");
        assertThat(childListener.getEvent().getName()).isEqualTo("inBoth");
    }

    @Test
    void testFireOnParent() {
        TestEventListener childListener = new TestEventListener("child");
        TestEventListener parentListener = new TestEventListener("parent");

        EventManager parentEM = new EventManager();
        parentEM.addListener(parentListener);

        EventManager childEM = new EventManager();
        childEM.bindParent(parentEM);
        childEM.addListener(childListener);

        parentEM.fire(Event.builder("parentOnly", "someSource").build());
        assertThat(parentListener.getEvent().getName()).isEqualTo("parentOnly");
        assertThat(childListener.getEvent()).isNull();
    }

    @Test
    void testParentBindingError() {
        EventManager parentEM = new EventManager();
        EventManager childEM = new EventManager(parentEM);
        assertDoesNotThrow(() -> childEM.bindParent(parentEM));
        assertThrows(IllegalStateException.class,
                () -> childEM.bindParent(new EventManager()));
    }

    @Data
    static class TestEventListener implements IEventListener<Event> {
        private String salt;
        private Event event;

        public TestEventListener() {}
        public TestEventListener(String salt) {
            this.salt = salt;
        }
        @Override
        public void accept(Event event) {
            this.event = event;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestObject {
        private TestEventListener el = new TestEventListener("found me");
    }
}
