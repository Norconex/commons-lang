/* Copyright 2026 Norconex Inc.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;

public class HostTest {

    @Test
    public void testLoadFromXML_nameOnly_noPort() {
        // A <host> with a name but no <port> child must not throw, and
        // should resolve to "any port" rather than NPE-ing on unboxing.
        var xml = new XML("<host><name>example.com</name></host>");
        var host = assertDoesNotThrow(
                () -> Host.loadFromXML(xml, null));
        assertEquals("example.com", host.getName());
        assertEquals(-1, host.getPort());
    }

    @Test
    public void testLoadFromXML_nameAndPort() {
        var xml = new XML("<host><name>example.com</name><port>443</port></host>");
        var host = Host.loadFromXML(xml, null);
        assertEquals("example.com", host.getName());
        assertEquals(443, host.getPort());
    }

    @Test
    public void testLoadFromXML_empty_returnsDefault() {
        var xml = new XML("<host/>");
        assertNull(Host.loadFromXML(xml, null));
    }

    @Test
    public void testLoadFromXML_null_returnsDefault() {
        var defaultHost = new Host("default.example.com", 80);
        assertEquals(defaultHost, Host.loadFromXML(null, defaultHost));
    }
}
