/* Copyright 2018 Norconex Inc.
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
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionUtil;
import com.norconex.commons.lang.xml.XML;

/**
 * Convenience class for implementation requiring proxy settings.
 *
 * @author Pascal Essiembre
 * @since 1.14.0
 */
public class ProxySettings implements IXMLConfigurable, Serializable {

    private static final long serialVersionUID = 1L;

    private String proxyHost;
    private int proxyPort;
    private String proxyScheme;
    private String proxyUsername;
    private String proxyPassword;
    private EncryptionKey proxyPasswordKey;
    private String proxyRealm;

    public ProxySettings() {
        super();
    }
    public ProxySettings(String host, int port) {
        super();
        this.proxyHost = host;
        this.proxyPort = port;
    }

    public String getProxyHost() {
        return proxyHost;
    }
    public ProxySettings setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }
    public int getProxyPort() {
        return proxyPort;
    }
    public ProxySettings setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }
    public String getProxyScheme() {
        return proxyScheme;
    }
    public ProxySettings setProxyScheme(String proxyScheme) {
        this.proxyScheme = proxyScheme;
        return this;
    }
    public String getProxyUsername() {
        return proxyUsername;
    }
    public ProxySettings setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
        return this;
    }
    public String getProxyPassword() {
        return proxyPassword;
    }
    public ProxySettings setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }
    public EncryptionKey getProxyPasswordKey() {
        return proxyPasswordKey;
    }
    public ProxySettings setProxyPasswordKey(EncryptionKey proxyPasswordKey) {
        this.proxyPasswordKey = proxyPasswordKey;
        return this;
    }
    public String getProxyRealm() {
        return proxyRealm;
    }
    public ProxySettings setProxyRealm(String proxyRealm) {
        this.proxyRealm = proxyRealm;
        return this;
    }
    public boolean isSet() {
        return StringUtils.isNotBlank(proxyHost);
    }

    public void copyFrom(ProxySettings another) {
        proxyHost = another.proxyHost;
        proxyPort = another.proxyPort;
        proxyScheme = another.proxyScheme;
        proxyUsername = another.proxyUsername;
        proxyPassword = another.proxyPassword;
        proxyPasswordKey = another.proxyPasswordKey;
        proxyRealm = another.proxyRealm;
    }

    /**
     * Creates an Apache {@link HttpHost}.
     * @return HttpHost or <code>null</code> if proxy is not set.
     */
    public HttpHost createHttpHost() {
        if (isSet()) {
            return new HttpHost(proxyHost, proxyPort, proxyScheme);
        }
        return null;
    }
    public AuthScope createAuthScope() {
        if (isSet()) {
            return new AuthScope(proxyHost, proxyPort, proxyRealm);
        }
        return null;
    }
    public Credentials createCredentials() {
        if (isSet() && StringUtils.isNotBlank(proxyUsername)) {
            String password =
                    EncryptionUtil.decrypt(proxyPassword, proxyPasswordKey);
            return new UsernamePasswordCredentials(proxyUsername, password);
        }
        return null;
    }
    public CredentialsProvider createCredentialsProvider() {
        if (isSet()) {
            Credentials creds = createCredentials();
            if (creds != null) {
                CredentialsProvider cp = new BasicCredentialsProvider();
                cp.setCredentials(createAuthScope(), creds);
                return cp;
            }
        }
        return null;
    }

    @Override
    public void loadFromXML(XML xml) {
        proxyHost = xml.getString("proxyHost", proxyHost);
        proxyPort = xml.getInteger("proxyPort", proxyPort);
        proxyScheme = xml.getString("proxyScheme", proxyScheme);
        proxyUsername = xml.getString("proxyUsername", proxyUsername);
        proxyPassword = xml.getString("proxyPassword", proxyPassword);
        proxyPasswordKey = EncryptionKey.getFromXML(
                xml, "proxyPasswordKey", proxyPasswordKey);
        proxyRealm = xml.getString("proxyRealm", proxyRealm);
    }

    @Override
    public void saveToXML(XML xml) {
        xml.addElement("proxyHost", proxyHost);
        xml.addElement("proxyPort", proxyPort);
        xml.addElement("proxyScheme", proxyScheme);
        xml.addElement("proxyUsername", proxyUsername);
        xml.addElement("proxyPassword", proxyPassword);
        EncryptionKey.addToXML(xml, "proxyPasswordKey", proxyPasswordKey);
        xml.addElement("proxyRealm", proxyRealm);
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
