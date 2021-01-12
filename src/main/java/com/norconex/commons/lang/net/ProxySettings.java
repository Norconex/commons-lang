/* Copyright 2018-2020 Norconex Inc.
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.security.Credentials;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>
 * Convenience class for implementation requiring proxy settings.
 * </p>
 *
 * {@nx.include com.norconex.commons.lang.security.Credentials#doc}
 *
 * {@nx.xml.usage
 * <host>
 *   {@nx.include com.norconex.commons.lang.net.Host@nx.xml.usage}
 * </host>
 * <scheme>(Default is "http")</scheme>
 * <realm>(Authentication realm. Default is any.)</realm>
 * <credentials>
 *   {@nx.include com.norconex.commons.lang.security.Credentials@nx.xml.usage}
 * </credentials>
 * }
 * <p>The above can be found under any parent tag. See consuming class
 * documentation for exact usage.</p>
 *
 * @author Pascal Essiembre
 * @since 1.14.0
 */
public class ProxySettings implements IXMLConfigurable, Serializable {

    private static final long serialVersionUID = 1L;

    private Host host;
    private String scheme;
    private final Credentials credentials = new Credentials();
    private String realm;

    public ProxySettings() {
        super();
    }
    public ProxySettings(String name, int port) {
        super();
        this.host = new Host(name, port);
    }
    public ProxySettings(Host host) {
        super();
        this.host = host;
    }

    /**
     * Gets the proxy host.
     * @return proxy host or <code>null</code> if not set
     * @see #isSet()
     */
    public Host getHost() {
        return host;
    }
    /**
     * Sets the proxy host.
     * @param host proxy host
     * @return this
     */
    public ProxySettings setHost(Host host) {
        this.host = host;
        return this;
    }

    /**
     * Gets the proxy scheme.
     * @return proxy scheme
     * @since 2.0.0
     */
    public String getScheme() {
        return scheme;
    }
    /**
     * Sets the proxy scheme.
     * @param scheme proxy scheme
     * @return this
     * @since 2.0.0
     */
    public ProxySettings setScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * Gets the proxy credentials.
     * @return proxy credentials (never <code>null</code>)
     * @since 2.0.0
     */
    public Credentials getCredentials() {
        return credentials;
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
     * Gets the proxy realm.
     * @return proxy realm
     * @since 2.0.0
     */
    public String getRealm() {
        return realm;
    }
    /**
     * Sets the proxy realm.
     * @param realm proxy realm
     * @return this
     * @since 2.0.0
     */
    public ProxySettings setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    /**
     * Gets the proxy host.
     * @return proxy host
     * @deprecated Since 2.0.0, use {@link #getHost()}.
     */
    @Deprecated
    public String getProxyHost() {
        if (host != null) {
            return host.getName();
        }
        return null;
    }
    /**
     * Sets proxy host.
     * @param proxyHost proxy host
     * @return this
     * @deprecated Since 2.0.0, use {@link #setHost(Host)}.
     */
    @Deprecated
    public ProxySettings setProxyHost(String proxyHost) {
        if (this.host != null) {
            this.host = new Host(proxyHost, host.getPort());
        }
        return this;
    }

    /**
     * Gets the proxy port.
     * @return proxy port
     * @deprecated Since 2.0.0, use {@link #getHost()}.
     */
    @Deprecated
    public int getProxyPort() {
        if (host != null) {
            return host.getPort();
        }
        return 0;
    }
    /**
     * Sets proxy port.
     * @param proxyPort proxy port
     * @return this
     * @deprecated Since 2.0.0, use {@link #setHost(Host)}.
     */
    @Deprecated
    public ProxySettings setProxyPort(int proxyPort) {
        if (this.host != null) {
            this.host = new Host(host.getName(), proxyPort);
        }
        return this;
    }

    /**
     * Gets the proxy scheme.
     * @return proxy scheme
     * @deprecated Since 2.0.0, use {@link #getScheme()}.
     */
    @Deprecated
    public String getProxyScheme() {
        return getScheme();
    }
    /**
     * Sets proxy scheme.
     * @param proxyScheme proxy scheme
     * @return this
     * @deprecated Since 2.0.0, use {@link #setScheme(String)}.
     */
    @Deprecated
    public ProxySettings setProxyScheme(String proxyScheme) {
        return setScheme(proxyScheme);
    }

    /**
     * Gets proxy username.
     * @return proxy username
     * @deprecated Since 2.0.0, use {@link #getCredentials()}.
     */
    @Deprecated
    public String getProxyUsername() {
        return  credentials.getUsername();
    }
    /**
     * Sets proxy username.
     * @param proxyUsername proxy username
     * @return this instance
     * @deprecated Since 2.0.0, use {@link #setCredentials(Credentials)}.
     */
    @Deprecated
    public ProxySettings setProxyUsername(String proxyUsername) {
        this.credentials.setUsername(proxyUsername);
        return this;
    }
    /**
     * Gets proxy password.
     * @return proxy password
     * @deprecated Since 2.0.0, use {@link #getCredentials()}.
     */
    @Deprecated
    public String getProxyPassword() {
        return credentials.getPassword();
    }
    /**
     * Sets proxy password.
     * @param proxyPassword proxy password
     * @return this instance
     * @deprecated Since 2.0.0,
     *             use {@link #setCredentials(Credentials)}.
     */
    @Deprecated
    public ProxySettings setProxyPassword(String proxyPassword) {
        this.credentials.setPassword(proxyPassword);
        return this;
    }
    /**
     * Gets proxy password key.
     * @return proxy password key
     * @deprecated Since 2.0.0, use {@link #getCredentials()}.
     */
    @Deprecated
    public EncryptionKey getProxyPasswordKey() {
        return credentials.getPasswordKey();
    }
    /**
     * Sets proxy password key.
     * @param proxyPasswordKey proxy password key
     * @return this instance
     * @deprecated Since 2.0.0, use {@link #setCredentials(Credentials)}.
     */
    @Deprecated
    public ProxySettings setProxyPasswordKey(EncryptionKey proxyPasswordKey) {
        this.credentials.setPasswordKey(proxyPasswordKey);
        return this;
    }
    /**
     * Gets proxy realm.
     * @return proxy realm
     * @deprecated Since 2.0.0, use {@link #getRealm()}.
     */
    @Deprecated
    public String getProxyRealm() {
        return realm;
    }
    /**
     * Sets proxy realm.
     * @param proxyRealm proxy realm
     * @return this instance
     * @deprecated Since 2.0.0, use {@link #setRealm(String)}.
     */
    @Deprecated
    public ProxySettings setProxyRealm(String proxyRealm) {
        this.realm = proxyRealm;
        return this;
    }
    public boolean isSet() {
        return host != null && host.isSet();
    }

    public void copyTo(ProxySettings another) {
        BeanUtil.copyProperties(another, this);
    }
    public void copyFrom(ProxySettings another) {
        BeanUtil.copyProperties(this, another);
    }

    @Override
    public void loadFromXML(XML xml) {
        if (xml != null) {
            xml.checkDeprecated("proxyHost", "host", true);
            xml.checkDeprecated("proxyPort", "port", true);
            xml.checkDeprecated("proxyScheme", "scheme", true);
            xml.checkDeprecated("proxyRealm", "realm", true);
            //TODO make host IXMLConfigurable and use populate() ?
            host = Host.loadFromXML(xml.getXML("host"), host);
            scheme = xml.getString("scheme", scheme);
            realm = xml.getString("realm", realm);
            xml.ifXML("credentials", x -> x.populate(credentials));
        }
    }

    @Override
    public void saveToXML(XML xml) {
        if (xml != null) {
            Host.saveToXML(xml.addElement("host"), host);
            xml.addElement("scheme", scheme);
            xml.addElement("realm", realm);
            credentials.saveToXML(xml.addElement("credentials"));
        }
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
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
