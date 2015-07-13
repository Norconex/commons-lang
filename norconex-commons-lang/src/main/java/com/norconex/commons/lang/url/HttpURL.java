/* Copyright 2010-2014 Norconex Inc.
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
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class act as a mutable URL, which could be a replacement
 * or "wrapper" to the {@link URL} class.
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
    
    /**
     * Constructor.
     */
    public HttpURL() {
        super();
    }

    /**
     * Constructor.
     * @param url a URL
     */
    public HttpURL(URL url) {
        this(url.toString());
    }
    /**
     * Constructor.
     * @param url a URL
     */
    public HttpURL(String url) {
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
            queryString = new QueryString(url);
        }
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
     * Convert this HttpURL to a regular URL.
     * @return a URL
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
     * Returns a string representation of this URL.
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
            b.append('/');
            b.append(path);
        }
        if (queryString != null && !queryString.isEmpty()) {
            b.append(queryString.toString());
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
                .isEquals();
    }
}
