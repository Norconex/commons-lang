/* Copyright 2020-2023 Norconex Inc.
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;

/**
 * Holds a host name and port.
 *
 * {@nx.xml.usage
 * <name>(host name)</name>
 * <port>(host port)</port>
 * }
 * @since 2.0.0
 */
@EqualsAndHashCode
@JsonAutoDetect(
        fieldVisibility=Visibility.ANY,
        getterVisibility=Visibility.NONE,
        isGetterVisibility=Visibility.NONE)
public final class Host implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int port = -1;

    @JsonCreator
    public Host(
            @JsonProperty("name") String name,
            @JsonProperty("port") int port) {
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
    public String toString() {
        return name + ":" + port;
    }
}
