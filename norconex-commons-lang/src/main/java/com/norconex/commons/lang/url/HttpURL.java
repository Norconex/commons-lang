package com.norconex.commons.lang.url;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

/**
 * This class can be seen as a mutable URL, which could be a replacement
 * or "wrapper" to the {@link URL} class.
 * 
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class HttpURL implements Serializable {

    private static final long serialVersionUID = -8886393027925815099L;
    
    public static final int DEFAULT_PORT = 80;
    
    private QueryString queryString;
    private String host;
    private int port = DEFAULT_PORT;
    private boolean secure;
    private String path;
    
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
            secure = urlwrap.getProtocol().equalsIgnoreCase("https");
            host = urlwrap.getHost();
            port = urlwrap.getPort();
            path = urlwrap.getPath();
        }
        
        // Parameters
        if (StringUtils.contains(url, "?")) {
            queryString = new QueryString(url);
        }
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

    public boolean isSecure() {
        return secure;
    }
    public void setSecure(boolean secure) {
        this.secure = secure;
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
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(secure ? "https://" : "http://");
        b.append(host);
        if (port != DEFAULT_PORT) {
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + port;
        result = prime * result
                + ((queryString == null) ? 0 : queryString.hashCode());
        result = prime * result + (secure ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HttpURL other = (HttpURL) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (port != other.port)
            return false;
        if (queryString == null) {
            if (other.queryString != null)
                return false;
        } else if (!queryString.equals(other.queryString))
            return false;
        if (secure != other.secure)
            return false;
        return true;
    }
}
