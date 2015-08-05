/* Copyright 2010-2015 Norconex Inc.
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
package com.norconex.commons.lang.url;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class act as a mutable URL, which could be a replacement
 * or "wrapper" to the {@link URL} class. It can also be used as a safer way 
 * to build a {@link URL} or a {@link URI} instance as it will properly escape 
 * appropriate characters before creating those.
 * 
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class HttpURL implements Serializable {

    private static final long serialVersionUID = -8886393027925815099L;
    
    /** Default URL HTTP Port. */
    public static final int DEFAULT_HTTP_PORT = 80;
    /** Default Secure URL HTTP Port. */
    public static final int DEFAULT_HTTPS_PORT = 443;
    
    private QueryString queryString;
    private String host;
    private int port = DEFAULT_HTTP_PORT;
    private String path;
    private String protocol;
    private final String encoding;
    
    /**
     * Creates a blank HttpURL using UTF-8 for URL encoding.
     */
    public HttpURL() {
        this("", null);
    }

    /**
     * Creates a new HttpURL from the URL object using UTF-8 for URL encoding.
     * @param url a URL
     */
    public HttpURL(URL url) {
        this(url.toString());
    }
    /**
     * Creates a new HttpURL from the URL string using UTF-8 for URL encoding.
     * @param url a URL
     */
    public HttpURL(String url) {
        this(url, null);
    }

    /**
     * Creates a new HttpURL from the URL object using the provided encoding
     * for URL encoding.
     * @param url a URL
     * @param encoding character encoding
     * @since 1.7.0
     */
    public HttpURL(URL url, String encoding) {
        this(url.toString(), encoding);
    }
    /**
     * Creates a new HttpURL from the URL string using the provided encoding
     * for URL encoding.
     * @param url a URL string
     * @param encoding character encoding
     * @since 1.7.0
     */
    public HttpURL(String url, String encoding) {
        if (StringUtils.isBlank(encoding)) {
            this.encoding = CharEncoding.UTF_8;
        } else {
            this.encoding = encoding;
        }
        if (url.startsWith("http")) {
            URL urlwrap;
            try {
                urlwrap = new URL(url);
            } catch (MalformedURLException e) {
                throw new URLException("Could not interpret URL: " + url, e);
            }
            protocol = urlwrap.getProtocol();
            host = urlwrap.getHost();
            port = urlwrap.getPort();
            path = urlwrap.getPath();
        }
        
        // Parameters
        if (StringUtils.contains(url, "?")) {
            queryString = new QueryString(url, encoding);
        }
    }

    
    /**
     * Gets the character encoding. Default is UTF-8.
     * @return character encoding
     * @since 1.7.0
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Gets the URL path.
     * @return URL path
     */
    public String getPath() {
        return path;
    }
    /**
     * Sets the URL path.
     * @param path url path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the URL query string.
     * @return URL query string, or <code>null</code> if none
     */
    public QueryString getQueryString() {
        return queryString;
    }
    /**
     * Sets the URL query string.
     * @param queryString the query string
     */
    public void setQueryString(QueryString queryString) {
        this.queryString = queryString;
    }
    
    /**
     * Gets the host portion of the URL.
     * @return the host portion of the URL
     */
    public String getHost() {
        return host;
    }
    /**
     * Sets the host portion of the URL.
     * @param host the host portion of the URL
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets the protocol portion of the URL (e.g. http, https);
     * @return the protocol portion of the URL
     */
    public String getProtocol() {
        return protocol;
    }
    /**
     * Sets the protocol portion of the URL.
     * @param protocol the protocol portion of the URL
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    /**
     * Whether this URL is secure (e.g. https).
     * @return <code>true</code> if protocol is secure
     */
    public boolean isSecure() {
        return getProtocol().equalsIgnoreCase("https");
    }

    /**
     * Gets the URL port.
     * @return the URL port
     */
    public int getPort() {
        return port;
    }
    /**
     * Sets the URL port.
     * @param port the URL port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the last URL path segment without the query string.
     * If there are segment to return, 
     * an empty string will be returned instead.
     * @return the last URL path segment
     */
    public String getLastPathSegment() {
        if (StringUtils.isBlank(path)) {
            return StringUtils.EMPTY;
        }
        String segment = path;
        segment = StringUtils.substringAfterLast(segment, "/");
        return segment;
    }
    /**
     * Converts this HttpURL to a regular {@link URL}, making sure 
     * appropriate characters are escaped properly.
     * @return a URL
     * @throws URLException when URL is malformed
     */
    public URL toURL() {
        String url = toString();
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new URLException("Cannot convert to URL: " + url, e);
        }
    }
    /**
     * Converts this HttpURL to a {@link URI}, making sure 
     * appropriate characters are escaped properly.
     * @return a URI
     * @since 1.7.0
     * @throws URLException when URL is malformed
     */
    public URI toURI() {
        String url = toString();
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new URLException("Cannot convert to URI: " + url, e);
        }
    }
    /**
     * <p>
     * Converts the supplied URL to a {@link URL}, making sure 
     * appropriate characters are encoded properly using UTF-8. This method
     * is a short form of:<br>
     * <code>new HttpURL("http://example.com").toURL();</code>
     * </p>
     * @param url a URL string
     * @return a URL object
     * @since 1.7.0
     * @throws URLException when URL is malformed
     */
    public static URL toURL(String url) {
        return new HttpURL(url).toURL();
    }
    /**
     * <p>Converts the supplied URL to a {@link URI}, making sure 
     * appropriate characters are encoded properly using UTF-8. This method
     * is a short form of:<br>
     * <code>new HttpURL("http://example.com").toURI();</code>
     * </p>
     * @param url a URL string
     * @return a URI object
     * @since 1.7.0
     * @throws URLException when URL is malformed
     */
    public static URI toURI(String url) {
        return new HttpURL(url).toURI();
    }
    
    /**
     * Returns a string representation of this URL, properly encoded.
     * @return URL as a string
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(protocol);
        b.append("://");
        b.append(host);
        if (port != DEFAULT_HTTP_PORT) {
            b.append(':');
            b.append(port);
        }
        if (StringUtils.isNotBlank(path)) {
            if (!path.startsWith("/")) {
                b.append('/');
            }
            b.append(encodePath(path));
        }
        if (queryString != null && !queryString.isEmpty()) {
            b.append(queryString.toString());
        }
        return b.toString();
    }

    /**
     * <p>URL-Encodes a URL path. The provided string is assumed to represent
     * just the path portion of a URL.  Any characters that are not one
     * of the following is encoded: </p>
     * <p><code>a-z A-Z 0-9 . - _ ~ ! $ & ' ( ) * + , ; = : @ / %</code></p>
     * @param path path portion of a URL
     * @return encoded path
     * @since 1.7.0
     */
    public static String encodePath(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        StringBuilder b = new StringBuilder();
        for (char ch : path.toCharArray()) {
            // Space to plus sign
            if (ch == ' ') {
                b.append('+');
            // Valid: keep it as is.
            } else if (CharUtils.isAsciiAlphanumeric(ch)
                    || ".-_~!$&'()*+,;=:@/%".indexOf(ch) != -1)  {
                b.append(ch);
            // Invalid: encode it
            } else {
                String code = Integer.toHexString(ch).toUpperCase();
                b.append('%');
                b.append(code);
            }
        }
        return b.toString();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(host)
                .append(path)
                .append(port)
                .append(protocol)
                .append(queryString)
                .append(encoding)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof HttpURL)) {
            return false;
        }
        HttpURL other = (HttpURL) obj;
        return new EqualsBuilder()
                .append(host, other.host)
                .append(path, other.path)
                .append(port, other.port)
                .append(protocol, other.protocol)
                .append(queryString, other.queryString)
                .append(encoding, other.encoding)
                .isEquals();
    }
}
