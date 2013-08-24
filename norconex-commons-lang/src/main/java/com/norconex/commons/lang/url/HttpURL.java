/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.url;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class can be seen as a mutable URL, which could be a replacement
 * or "wrapper" to the {@link URL} class.
 * 
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class HttpURL implements Serializable {

    private static final long serialVersionUID = -8886393027925815099L;
    
    public static final int DEFAULT_HTTP_PORT = 80;
    public static final int DEFAULT_HTTPS_PORT = 443;
    
    private QueryString queryString;
    private String host;
    private int port = DEFAULT_HTTP_PORT;
    private String path;
    private String protocol;
    
    public HttpURL() {
        super();
    }

    public HttpURL(URL url) {
        this(url.toString());
    }
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

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public QueryString getQueryString() {
        return queryString;
    }
    public void setQueryString(QueryString queryString) {
        this.queryString = queryString;
    }
    
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public boolean isSecure() {
        return getProtocol().equalsIgnoreCase("https");
    }

    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    
    public String getLastPathSegment() {
        String segment = toString();
        segment = StringUtils.substringAfterLast(segment, "/");
        return segment;
    }
    public URL toURL() {
        String url = toString();
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new URLException("Cannot convert to URL: "
                    + url, e);
        }
    }
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
