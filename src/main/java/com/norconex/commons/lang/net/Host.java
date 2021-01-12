/* Copyright 2020 Norconex Inc.
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

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.xml.XML;

/**
 * Holds a host name and port.
 *
 * {@nx.xml.usage
 * <name>(host name)</name>
 * <port>(host port)</port>
 * }
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class Host implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int port;

    public Host(String name, int port) {
        super();
        this.name = name;
        this.port = port;
    }
    public String getName() {
        return name;
    }
    public int getPort() {
        return port;
    }

    public boolean isSet() {
        return StringUtils.isNotBlank(name);
    }

    public Host withName(String name) {
        return new Host(name, getPort());
    }
    public Host withPort(int port) {
        return new Host(getName(), port);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return name + ":" + port;
    }

    /**
     * Gets a host from an existing XML.
     * @param xml the XML to get the host from
     * @param defaultHost default host if it does not exist in XML
     * @return host
     */
    public static Host loadFromXML(XML xml, Host defaultHost) {
        if (xml == null) {
            return defaultHost;
        }
        String name = xml.getString("name");
        if (StringUtils.isNotBlank(name)) {
            return new Host(name, xml.getInteger("port"));
        }
        return defaultHost;
    }

    /**
     * Adds a host to an existing XML.
     * @param xml the XML to add the host to
     * @param host the host
     */
    public static void saveToXML(XML xml, Host host) {
        if (host != null) {
            xml.addElement("name", host.getName());
            xml.addElement("port", host.getPort());
        }
    }
}
