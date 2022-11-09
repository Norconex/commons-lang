/* Copyright 2017-2022 Norconex Inc.
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionKey.Source;
import com.norconex.commons.lang.security.Credentials;
import com.norconex.commons.lang.xml.XML;

class ProxySettingsTest {

    @Test
    void testProxySettings() {
        ProxySettings proxy = new ProxySettings("example.com", 123)
                .setScheme("myscheme")
                .setRealm("myrealm")
                .setCredentials(new Credentials("Bob", "Secure"));
        assertThat(proxy.getHost()).isEqualTo(new Host("example.com", 123));
        assertThat(proxy.getScheme()).isEqualTo("myscheme");
        assertThat(proxy.getRealm()).isEqualTo("myrealm");
        assertThat(proxy.isSet()).isTrue();

        ProxySettings anotherProxy = new ProxySettings();
        proxy.copyTo(anotherProxy);
        assertThat(anotherProxy).isEqualTo(proxy);

        proxy = new ProxySettings();
        assertThat(proxy.isSet()).isFalse();
        anotherProxy.copyFrom(proxy);
        assertThat(proxy).isEqualTo(anotherProxy);
    }

    @Test
    void testWriteRead() {
        ProxySettings ps = new ProxySettings();
        ps.setHost(new Host("myhost", 99));
        ps.getCredentials().setUsername("myusername");
        ps.getCredentials().setPassword("mypassword");
        ps.getCredentials().setPasswordKey(
                new EncryptionKey("keyvalue", Source.KEY, 256));
        ps.setRealm("realm");
        ps.setScheme("scheme");

        XML.assertWriteRead(ps, "proxySettings");

        assertDoesNotThrow(() -> ps.loadFromXML(null));
        assertDoesNotThrow(() -> ps.saveToXML(null));
    }
}
