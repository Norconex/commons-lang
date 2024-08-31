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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.xml.Xml;
import com.norconex.commons.lang.xml.XmlConfigurable;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(level = AccessLevel.PRIVATE)
@Data
@Accessors(chain = true)
public class MockCredentials
        implements XmlConfigurable {
    private String username;
    private String password;
    private MockEncryptionKey passwordKey;

    public MockCredentials() {
        this(null, null);
    }

    public MockCredentials(String username, String password) {
        this(username, password, null);
    }

    public MockCredentials(
            String username, String password, MockEncryptionKey passwordKey) {
        this.username = username;
        this.password = password;
        this.passwordKey = passwordKey;
    }

    public MockCredentials(MockCredentials copy) {
        copyFrom(copy);
    }

    public boolean isSet() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return StringUtils.isAllBlank(username, password);
    }

    public void copyTo(MockCredentials creds) {
        BeanUtil.copyProperties(creds, this);
    }

    public void copyFrom(MockCredentials creds) {
        BeanUtil.copyProperties(this, creds);
    }

    @Override
    public void loadFromXML(Xml xml) {
        if (xml != null) {
            setUsername(xml.getString(Fields.username, getUsername()));
            setPassword(xml.getString(Fields.password, getPassword()));
            setPasswordKey(MockEncryptionKey.loadFromXML(
                    xml.getXML(Fields.passwordKey), passwordKey));
        }
    }

    @Override
    public void saveToXML(Xml xml) {
        if (xml != null) {
            xml.addElement(Fields.username, getUsername());
            xml.addElement(Fields.password, getPassword());
            MockEncryptionKey.saveToXML(
                    xml.addElement(Fields.passwordKey), passwordKey);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE)
                        .append(Fields.username, username)
                        .append(Fields.password, "********")
                        .append(Fields.passwordKey, passwordKey)
                        .toString();
    }
}