package com.norconex.commons.lang.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionUtil;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

/**
 * <p>Allows many classes implementing {@link IXMLConfigurable} to load and store an {@link EncryptionKey} in a standard manner through delegation.</p>
 *
 * @author davisda4
 * @since 1.9.1
 * @see EncryptionUtil
 *
 */
public class PasswordKeyUtil {
    public static EncryptionKey loadKeyFrom(Reader in, EncryptionKey defaultKey) {
        XMLConfiguration xml = XMLConfigurationUtil.newXMLConfiguration(in);
        return loadKeyFrom(xml, defaultKey);
    }

    public static EncryptionKey loadKeyFrom(XMLConfiguration xml, EncryptionKey defaultKey) {
        String xmlKey = xml.getString("passwordKey", null);
        String xmlSource = xml.getString("passwordKeySource", null);
        if (StringUtils.isNotBlank(xmlKey)) {
            EncryptionKey.Source source = null;
            if (StringUtils.isNotBlank(xmlSource)) {
                source = EncryptionKey.Source.valueOf(xmlSource.toUpperCase());
            }
            return new EncryptionKey(xmlKey, source);
        }
        return defaultKey;
    }


    public static void saveKeyTo(Writer out, EncryptionKey passwordKey) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            saveKeyTo(writer, passwordKey);
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);

        }
    }

    public static void saveKeyTo(EnhancedXMLStreamWriter writer, EncryptionKey passwordKey) throws XMLStreamException {
        if (passwordKey != null) {
            writer.writeElementString("passwordKey", passwordKey.getValue());
            if (passwordKey.getSource() != null) {
                writer.writeElementString("passwordKeySource",
                        passwordKey.getSource().name().toLowerCase());
            }
        }
    }

}
