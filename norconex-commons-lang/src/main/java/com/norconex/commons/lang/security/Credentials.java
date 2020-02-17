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
package com.norconex.commons.lang.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionUtil;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>
 * User credentials with an optionally encrypted password. To be encrypted,
 * there needs to be an encryption key.  Without one, the password
 * is assumed not to be encrypted despite this class name.
 * </p>
 *
 * {@nx.block #doc
 * <h3>Password encryption in XML configuration:</h3>
 * <p>
 * The <code>&lt;password&gt;</code> tag can take a password that has been
 * encrypted using <code>EncryptionUtil</code>.
 * In order for the password to be decrypted properly, you need
 * to specify the encryption key used to encrypt it. The key can obtained
 * from a different few supported locations. The combination of the password key
 * <code>&lt;value&gt;</code> and <code>&lt;source&gt;</code> is used
 * to properly locate the key. The supported sources and their values
 * are:
 * </p>
 * <table border="1" summary="">
 *   <tr>
 *     <th><code>source</code></th>
 *     <th><code>value</code></th>
 *   </tr>
 *   <tr>
 *     <td><code>key</code></td>
 *     <td>The actual encryption key.</td>
 *   </tr>
 *   <tr>
 *     <td><code>file</code></td>
 *     <td>Path to a file containing the encryption key.</td>
 *   </tr>
 *   <tr>
 *     <td><code>environment</code></td>
 *     <td>Name of an environment variable containing the key.</td>
 *   </tr>
 *   <tr>
 *     <td><code>property</code></td>
 *     <td>Name of a JVM system property containing the key.</td>
 *   </tr>
 * </table>
 * }
 *
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
    private String password;
    private EncryptionKey passwordKey;

    public Credentials() {
        super();
    }
    public Credentials(Credentials copy) {
        super();
        copyFrom(copy);
    }

    public boolean isSet() {
        return !isEmpty();
    }
    public boolean isEmpty() {
        return StringUtils.isAllBlank(username, password);
    }

    public String getPassword() {
        return password;
    }
    public Credentials setPassword(String password) {
        this.password = password;
        return this;
    }
    public EncryptionKey getPasswordKey() {
        return passwordKey;
    }
    public Credentials setPasswordKey(EncryptionKey passwordKey) {
        this.passwordKey = passwordKey;
        return this;
    }
    public String getUsername() {
        return username;
    }
    public Credentials setUsername(String username) {
        this.username = username;
        return this;
    }

    public void copyTo(Credentials creds) {
        BeanUtil.copyProperties(creds, this);
    }
    public void copyFrom(Credentials creds) {
        BeanUtil.copyProperties(this, creds);
    }

    @Override
    public void loadFromXML(XML xml) {
        setUsername(xml.getString("username", getUsername()));
        setPassword(xml.getString("password", getPassword()));
        setPasswordKey(EncryptionKey.loadFromXML(
                xml.getXML("passwordKey"), passwordKey));
    }
    @Override
    public void saveToXML(XML xml) {
        xml.addElement("username", getUsername());
        xml.addElement("password", getPassword());
        EncryptionKey.saveToXML(xml.addElement("passwordKey"), passwordKey);
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
