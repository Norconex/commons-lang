/* Copyright 2023 Norconex Inc.
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
package com.norconex.commons.lang.xml.mock;

import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.xml.Xml;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public final class MockHost {
    private String name;
    private int port = -1;

    public MockHost(String name, int port) {
        this.name = name;
        this.port = port;
    }

    public boolean isSet() {
        return StringUtils.isNotBlank(name);
    }

    public MockHost withName(String name) {
        return new MockHost(name, getPort());
    }

    public MockHost withPort(int port) {
        return new MockHost(getName(), port);
    }

    @Override
    public String toString() {
        return name + ":" + port;
    }

    public static MockHost loadFromXML(
            Xml xml, MockHost defaultHost) {
        if (xml == null) {
            return defaultHost;
        }
        var name = xml.getString("name");
        if (StringUtils.isNotBlank(name)) {
            return new MockHost(name, xml.getInteger("port"));
        }
        return defaultHost;
    }

    public static void saveToXML(Xml xml, MockHost host) {
        if (host != null) {
            xml.addElement("name", host.getName());
            xml.addElement("port", host.getPort());
        }
    }
}