package com.norconex.commons.lang.net;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionUtil;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

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
        if (isSet()) {
            String password = 
                    EncryptionUtil.decrypt(proxyPassword, proxyPasswordKey);
            return new UsernamePasswordCredentials(proxyUsername, password);
        }
        return null;
    }
    public CredentialsProvider createCredentialsProvider() {
        if (isSet()) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(
                        createAuthScope(),
                        createCredentials());
            return credsProvider;
        }
        return null;
    }

    protected String getXmlTag() {
        return "proxy";
    }
    
    /**
     * Loads from a {@link #getXmlTag()} tag.
     */
    @Override
    public void loadFromXML(Reader in) throws IOException {
        loadProxyFromXML(XMLConfigurationUtil.newXMLConfiguration(in));
    }
    /**
     * Loads assuming we are already in a parent tag.
     */
    public void loadProxyFromXML(XMLConfiguration xml) {
        proxyHost = xml.getString("proxyHost", proxyHost);
        proxyPort = xml.getInt("proxyPort", proxyPort);
        proxyScheme = xml.getString("proxyScheme", proxyScheme);
        proxyUsername = xml.getString("proxyUsername", proxyUsername);
        proxyPassword = xml.getString("proxyPassword", proxyPassword);
        proxyPasswordKey = 
                loadXMLPasswordKey(xml, "proxyPasswordKey", proxyPasswordKey);
        proxyRealm = xml.getString("proxyRealm", proxyRealm);
    }
    /**
     * Saves to a {@link #getXmlTag()} tag.
     */
    @Override
    public void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            writer.writeStartElement(getXmlTag());
            writer.writeAttribute("class", getClass().getCanonicalName());
            writer.flush();
            saveProxyToXML(writer);
            writer.flush();
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }
    /**
     * Saves assuming we are already in a parent tag.
     */
    public void saveProxyToXML(XMLStreamWriter out) throws XMLStreamException {
        EnhancedXMLStreamWriter writer;
        if (out instanceof EnhancedXMLStreamWriter) {
            writer = (EnhancedXMLStreamWriter) out;
        } else {
            writer = new EnhancedXMLStreamWriter(out);
        }
        writer.writeElementString("proxyHost", proxyHost);
        writer.writeElementInteger("proxyPort", proxyPort);
        writer.writeElementString("proxyScheme", proxyScheme);
        writer.writeElementString("proxyUsername", proxyUsername);
        writer.writeElementString("proxyPassword", proxyPassword);
        saveXMLPasswordKey(writer, "proxyPasswordKey", proxyPasswordKey);
        writer.writeElementString("proxyRealm", proxyRealm);
    }

    private void saveXMLPasswordKey(EnhancedXMLStreamWriter writer, 
            String field, EncryptionKey key) throws XMLStreamException {
        if (key == null) {
            return;
        }
        writer.writeElementString(field, key.getValue());
        if (key.getSource() != null) {
            writer.writeElementString(
                    field + "Source", key.getSource().name().toLowerCase());
        }
    }
    
    private EncryptionKey loadXMLPasswordKey(
            XMLConfiguration xml, String field, EncryptionKey defaultKey) {
        String xmlKey = xml.getString(field, null);
        String xmlSource = xml.getString(field + "Source", null);
        if (StringUtils.isBlank(xmlKey)) {
            return defaultKey;
        }
        EncryptionKey.Source source = null;
        if (StringUtils.isNotBlank(xmlSource)) {
            source = EncryptionKey.Source.valueOf(xmlSource.toUpperCase());
        }
        return new EncryptionKey(xmlKey, source);
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ProxySettings)) {
            return false;
        }
        ProxySettings castOther = (ProxySettings) other;
        return new EqualsBuilder()
                .append(proxyHost, castOther.proxyHost)
                .append(proxyPort, castOther.proxyPort)
                .append(proxyScheme, castOther.proxyScheme)
                .append(proxyUsername, castOther.proxyUsername)
                .append(proxyPassword, castOther.proxyPassword)
                .append(proxyPasswordKey, castOther.proxyPasswordKey)
                .append(proxyRealm, castOther.proxyRealm)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(proxyHost)
                .append(proxyPort)
                .append(proxyScheme)
                .append(proxyUsername)
                .append(proxyPassword)
                .append(proxyPasswordKey)
                .append(proxyRealm)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("proxyHost", proxyHost)
                .append("proxyPort", proxyPort)
                .append("proxyScheme", proxyScheme)
                .append("proxyUsername", proxyUsername)
                .append("proxyPassword", proxyPassword)
                .append("proxyPasswordKey", proxyPasswordKey)
                .append("proxyRealm", proxyRealm)
                .toString();
    }    
}