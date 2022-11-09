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
package com.norconex.commons.lang.net;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;

class HostTest {

    @Test
    void testHost() {
        Host host = new Host("example.com", 80);

        assertThat(host.getName()).isEqualTo("example.com");
        assertThat(host.getPort()).isEqualTo(80);
        assertThat(host.isSet()).isTrue();
        assertThat(host.withName(null).isSet()).isFalse();
        assertThat(host.withPort(45)).isEqualTo(new Host("example.com", 45));
        assertThat(host).hasToString("example.com:80");
    }

    @Test
    void testSaveLoadXML() {
        Host host = new Host("example.com", 123);
        XML xml = new XML("test");
        Host.saveToXML(xml, host);

        assertThat(Host.loadFromXML(xml, null)).isEqualTo(host);
        assertThat(Host.loadFromXML(null, host)).isEqualTo(host);

        host = new Host(null, 456);
        xml = new XML("test");
        Host.saveToXML(xml, host);
        assertThat(Host.loadFromXML(xml, host)).isEqualTo(host);
    }
}
