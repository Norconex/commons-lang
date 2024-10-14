/* Copyright 2018-2019 Norconex Inc.
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>
 * Utility methods for loading and saving {@link EncryptionKey} with
 * {@link IXMLConfigurable} objects or other XML-driven classes.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 1.15.0
 * @deprecated Since 2.0.0, use {@link EncryptionKey} save/load methods.
 */
@Deprecated
public final class EncryptionXMLUtil {

    private EncryptionXMLUtil() {
    }

    /**
     * Convenience method for loading an encryption key from an XML reader.
     * @param in an XML reader
     * @param tagPrefix prefix of the XML tag names being loaded.
     * @param defaultKey default encryption key
     * @return encryption key
     * @see IXMLConfigurable
     */
    public static EncryptionKey loadFromXML(
            Reader in, String tagPrefix, EncryptionKey defaultKey) {
        var xml = XML.of(in).create();
        return loadFromXML(xml, tagPrefix, defaultKey);
    }
    /**
     * Convenience method for loading an encryption key from an {@link XML}.
     * @param xml xml configuration
     * @param tagPrefix prefix of the XML tag names being loaded.
     * @param defaultKey default encryption key
     * @return encryption key
     * @see IXMLConfigurable
     */
    public static EncryptionKey loadFromXML(
            XML xml, String tagPrefix, EncryptionKey defaultKey) {
        var tagKey = StringUtils.trimToEmpty(tagPrefix);
        tagKey = tagKey.length() > 0 ? tagKey + "Key" : "key";
        var tagSource = tagKey + "Source";
        var tagSize = tagKey + "Size";

        var xmlKey = xml.getString(tagKey, null);
        if (StringUtils.isNotBlank(xmlKey)) {
            var xmlSource = xml.getString(tagSource, null);
            var size = xml.getInteger(
                    tagSize, EncryptionKey.DEFAULT_KEY_SIZE);
            EncryptionKey.Source source = null;
            if (StringUtils.isNotBlank(xmlSource)) {
                source = EncryptionKey.Source.valueOf(xmlSource.toUpperCase());
            }
            return new EncryptionKey(xmlKey, source, size);
        }
        return defaultKey;
    }

    /**
     * Convenience method for saving an encryption key to an XML writer.
     * @param writer a writer
     * @param tagPrefix Prefix of the XML tag names being saved. If
     *        <code>null</code>, no prefix is used (not recommended unless
     *        wrapped in a parent tag).
     * @param encryptionKey the encryption key to save
     * @throws IOException problem saving to XML
     * @see IXMLConfigurable
     */
    public static void saveToXML(
            Writer writer, String tagPrefix, EncryptionKey encryptionKey)
                    throws IOException {
        saveToXML(new EnhancedXMLStreamWriter(writer),
                tagPrefix, encryptionKey);
    }
    /**
     * Convenience method for saving an encryption key to an
     * {@link XMLStreamWriter}.
     * @param writer an XML writer
     * @param tagPrefix Prefix of the XML tag names being saved. If
     *        <code>null</code>, no prefix is used (not recommended unless
     *        wrapped in a parent tag).
     * @param encryptionKey the encryption key to save
     * @throws IOException problem saving to XML
     * @see IXMLConfigurable
     */
    public static void saveToXML(EnhancedXMLStreamWriter writer,
            String tagPrefix, EncryptionKey encryptionKey) throws IOException {

        var tagKey = StringUtils.trimToEmpty(tagPrefix);
        tagKey = tagKey.length() > 0 ? tagKey + "Key" : "key";
        var tagSource = tagKey + "Source";
        var tagSize = tagKey + "Size";

        if (encryptionKey != null) {
            writer.writeElementString(tagKey, encryptionKey.getValue());
            writer.writeElementInteger(tagSize, encryptionKey.getSize());
            if (encryptionKey.getSource() != null) {
                writer.writeElementString(tagSource,
                        encryptionKey.getSource().name().toLowerCase());
            }
        }
    }
}
