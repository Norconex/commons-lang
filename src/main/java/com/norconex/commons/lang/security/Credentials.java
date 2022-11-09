/* Copyright 2020-2022 Norconex Inc.
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

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionUtil;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

/**
 * <p>
 * User credentials with an optionally encrypted password. To be encrypted,
 * there needs to be an encryption key.  Without one, the password
 * is assumed not to be encrypted.
 * </p>
 *
 * {@nx.block #doc
 * <h3>Password encryption:</h3>
 * <p>
 * Passwords can be encrypted using <code>EncryptionUtil</code> (or
 * command-line "encrypt.bat" or "encrypt.sh" if those are available to you).
 * In order for the password to be decrypted properly, you need
 * to specify the encryption key used to encrypt it. The key can obtained
 * from a few supported locations. The combination of the password key
 * "value" and "source" is used to properly locate the key.
 * The supported sources are:
 * </p>
 * <p>
 * <table border="1" summary="">
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
 * </p>
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
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Data
@Accessors(chain = true)
public class Credentials implements IXMLConfigurable, Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private EncryptionKey passwordKey;

    public Credentials() {
        this(null, null);
    }
    /**
     * Creates a new Credentials instance with the supplied username and
     * password.
     * @param username the username
     * @param password the password
     * @since 3.0.0
     */
    public Credentials(String username, String password) {
        this(username, password, null);
    }
    /**
     * Creates a new Credentials instance with the supplied username,
     * password and password key.
     * @param username the username
     * @param password the password
     * @param passwordKey the password encryption key
     * @since 3.0.0
     */
    public Credentials(
            String username, String password, EncryptionKey passwordKey) {
        this.username = username;
        this.password = password;
        this.passwordKey = passwordKey;
    }
    public Credentials(Credentials copy) {
        copyFrom(copy);
    }

    public boolean isSet() {
        return !isEmpty();
    }
    public boolean isEmpty() {
        return StringUtils.isAllBlank(username, password);
    }

    public void copyTo(Credentials creds) {
        BeanUtil.copyProperties(creds, this);
    }
    public void copyFrom(Credentials creds) {
        BeanUtil.copyProperties(this, creds);
    }

    @Override
    public void loadFromXML(XML xml) {
        if (xml != null) {
            setUsername(xml.getString(Fields.username, getUsername()));
            setPassword(xml.getString(Fields.password, getPassword()));
            setPasswordKey(EncryptionKey.loadFromXML(
                    xml.getXML(Fields.passwordKey), passwordKey));
        }
    }
    @Override
    public void saveToXML(XML xml) {
        if (xml != null) {
            xml.addElement(Fields.username, getUsername());
            xml.addElement(Fields.password, getPassword());
            EncryptionKey.saveToXML(
                    xml.addElement(Fields.passwordKey), passwordKey);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append(Fields.username, username)
            .append(Fields.password, "********")
            .append(Fields.passwordKey, passwordKey)
            .toString();
    }
}
