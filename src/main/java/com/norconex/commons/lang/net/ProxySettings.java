/* Copyright 2018-2023 Norconex Inc.
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
import java.net.Proxy;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.security.Credentials;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Convenience class for implementation requiring proxy settings.
 * </p>
 * @since 1.14.0
 */
@Slf4j
@JsonAutoDetect(
    fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE
)
@Data
@Accessors(chain = true)
public class ProxySettings implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Proxy host (name and port), or <code>null</code> when not set. */
    private Host host;
    /**
     * Proxy scheme (default is "http").
     * @since 2.0.0
     */
    private String scheme;
    /**
     * Proxy credentials, when required or applicable. Never <code>null</code>.
     * @since 2.0.0
     */
    private final Credentials credentials = new Credentials();
    /**
     * Proxy authentication realm, when required or applicable.
     * @since 2.0.0
     */
    private String realm;

    public ProxySettings() {
    }

    public ProxySettings(String name, int port) {
        this(new Host(name, port));
    }

    public ProxySettings(Host host) {
        this.host = host;
    }

    /**
     * Sets the proxy credentials.
     * @param credentials proxy credentials
     * @return this
     * @since 2.0.0
     */
    public ProxySettings setCredentials(Credentials credentials) {
        this.credentials.copyFrom(credentials);
        return this;
    }

    /**
     * Wether this proxy is configured. That is, if the host is not
     * <code>null</code> and also configured.
     * @return <code>true</code> if set
     * @see Host#isSet()
     */
    public boolean isSet() {
        return host != null && host.isSet();
    }

    /**
     * Copy properties of this instance to the supplied instance.
     * @param another another proxy settings instance
     */
    public void copyTo(ProxySettings another) {
        BeanUtil.copyProperties(another, this);
    }

    /**
     * Copy properties of the supplied instance to this instance.
     * @param another another proxy settings instance
     */
    public void copyFrom(ProxySettings another) {
        BeanUtil.copyProperties(this, another);
    }

    /**
     * Converts this proxy settings to a {@link Proxy}. The scheme is used
     * to establish the proxy type.  If {@link #isSet()} returns false,
     * this method returns <code>null</code>.
     * @return proxy or <code>null</code>
     * @since 3.0.0
     */
    public Proxy toProxy() {
        if (!isSet()) {
            return null;
        }
        var type = Proxy.Type.HTTP;
        if (StringUtils.startsWithIgnoreCase(scheme, "socks")) {
            type = Proxy.Type.SOCKS;
        } else if (StringUtils.isNotBlank(scheme)
                && !StringUtils.startsWithIgnoreCase(scheme, "http")) {
            LOG.warn("Unsupported proxy scheme: '{}'. Defaulting to HTTP.",
                    scheme);
        }
        return new Proxy(type, host.toInetSocketAddress());
    }
}
