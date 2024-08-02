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

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.XMLConfigurable;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

@ToString
@EqualsAndHashCode
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Slf4j
@Data
@Accessors(chain = true)
public class MockProxySettings implements XMLConfigurable {
    private MockHost  host;
    private String scheme;
    private String realm;
    private final MockCredentials credentials =
            new MockCredentials();

    public MockProxySettings() {}
    public MockProxySettings(String name, int port) {
        this(new MockHost(name, port));
    }
    public MockProxySettings(MockHost host) {
        this.host = host;
    }
    public MockProxySettings setCredentials(
            MockCredentials credentials) {
        this.credentials.copyFrom(credentials);
        return this;
    }
    public boolean isSet() {
        return host != null && host.isSet();
    }
    public void copyTo(MockProxySettings another) {
        BeanUtil.copyProperties(another, this);
    }
    public void copyFrom(MockProxySettings another) {
        BeanUtil.copyProperties(this, another);
    }
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
        var address = new InetSocketAddress(host.getName(), host.getPort());
        return new Proxy(type, address);
    }
    @Override
    public void loadFromXML(XML xml) {
        if (xml != null) {
            xml.checkDeprecated("proxyHost", Fields.host, true);
            xml.checkDeprecated("proxyPort", Fields.host, true);
            xml.checkDeprecated("proxyScheme", Fields.scheme, true);
            xml.checkDeprecated("proxyRealm", Fields.realm, true);
            //MAYBE: make host XMLConfigurable and use populate() ?
            host = MockHost.loadFromXML(
                    xml.getXML(Fields.host), host);
            scheme = xml.getString(Fields.scheme, scheme);
            realm = xml.getString(Fields.realm, realm);
            xml.ifXML(Fields.credentials, x -> x.populate(credentials));
        }
    }

    @Override
    public void saveToXML(XML xml) {
        if (xml != null) {
            MockHost.saveToXML(xml.addElement(Fields.host), host);
            xml.addElement(Fields.scheme, scheme);
            xml.addElement(Fields.realm, realm);
            credentials.saveToXML(xml.addElement(Fields.credentials));
        }
    }
}