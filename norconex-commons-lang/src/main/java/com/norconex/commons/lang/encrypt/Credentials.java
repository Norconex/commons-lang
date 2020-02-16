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
package com.norconex.commons.lang.encrypt;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>
 * User credentials with an optionally encrypted password. To be encrypted,
 * there needs to be an encryption key.  Without one, the password
 * is assumed not to be encrypted despite this class name.
 * </p>
 * {@nx.xml.usage
 * <username>(the username)</username>
 * <password>(the optionally encrypted password)</password>
 * <passwordKey>
 *   <value>(The actual password encryption key or a reference to it.)</value>
 *   <source>[key|file|environment|property]</source>
 *   <size>(Size in bits of encryption key. Default is 128.)</size>
 * </passwordKey>
 * }
 * <p>
 * The expected parent tag name is defined by the consuming classes.
 * </p>
 *
 * {@nx.xml.example
 * <sampleConfig>
 *   <username>goldorak</username>
 *   <password>3ncryp73d</password>
 *   <passwordKey>
 *     <value>/path/to/my.key</value>
 *     <source>file</source>
 *   </passwordKey>
 * </sampleConfig>
 * }
 * <p>
 * The above example has the password encrypted with a key.  The encryption
 * key is stored in a file (required to decrypt the password).
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see EncryptionKey
 * @see EncryptionUtil
 */
public class Credentials implements IXMLConfigurable {

    private String username;
    private char[] password;
    private EncryptionKey passwordKey;

    public String getPassword() {
        if (password != null) {
            return new String(password);
        }
        return null;
    }
    public void setPassword(String password) {
        if (password != null) {
            this.password = password.toCharArray();
        } else {
            this.password = null;
        }
    }
    public EncryptionKey getPasswordKey() {
        return passwordKey;
    }
    public void setPasswordKey(EncryptionKey passwordKey) {
        this.passwordKey = passwordKey;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void loadFromXML(XML xml) {
        setUsername(xml.getString("username", getUsername()));
        setPassword(xml.getString("password", getPassword()));
        setPasswordKey(
                EncryptionKey.getFromXML(xml, "passwordKey", passwordKey));
    }
    @Override
    public void saveToXML(XML xml) {
        xml.addElement("username", getUsername());
        xml.addElement("password", getPassword());
        EncryptionKey.addToXML(xml, "passwordKey", passwordKey);
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
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("username", username)
            .append("password", "********")
            .append("passwordKey", passwordKey)
            .toString();
    }
}
