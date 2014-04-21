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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.map.Properties;


/**
 * Provides utility methods for getting and setting attributes on 
 * a URL query string. 
 * <p/>
 * <b>Since 1.4</b>, query string parameters are stored and returned in the
 * order they were provided.
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class QueryString extends Properties {
    
    private static final long serialVersionUID = 1744232652147275170L;

    private final String encoding;

    /**
     * Constructor.
     */
    public QueryString() {
        this(CharEncoding.UTF_8);
    }
    
    /**
     * Default URL character encoding is UTF-8.  
     * @param urlWithQueryString a URL from which to extract a query string.
     */
    public QueryString(URL urlWithQueryString) {
        this(urlWithQueryString.toString(), null);
    }
    /**
     * Constructor.
     * @param urlWithQueryString a URL from which to extract a query string.
     * @param encoding character encoding
     */
    public QueryString(URL urlWithQueryString, String encoding) {
        this(urlWithQueryString.toString(), encoding);
    }
    /**
     * Constructor.   Default URL character encoding is UTF-8.  
     * It is possible to only supply a query string as opposed to an
     * entire URL.
     * Key and values making up a query string are assumed to be URL-encoded.
     * Will throw a {@link URLException} if UTF-8 encoding is not supported.
     * @param urlWithQueryString a URL from which to extract a query string.
     */
    public QueryString(String urlWithQueryString) {
        this(urlWithQueryString, null);
    }
    /**
     * Constructor.  
     * It is possible to only supply a query string as opposed to an
     * entire URL.
     * Key and values making up a query string are assumed to be URL-encoded.
     * Will throw a {@link URLException} if the supplied encoding is 
     * unsupported or invalid.
     * @param urlWithQueryString a URL from which to extract a query string.
     * @param encoding character encoding
     */
    public QueryString(String urlWithQueryString, String encoding) {
        super(new ListOrderedMap<String, List<String>>());
        if (StringUtils.isBlank(encoding)) {
            this.encoding = CharEncoding.UTF_8;
        } else {
            this.encoding = encoding;
        }
        String paramString = urlWithQueryString;
        if (StringUtils.contains(paramString, "?")) {
            paramString = paramString.replaceAll("(.*?)(\\?)(.*)", "$3");
        }
        String[] paramParts = paramString.split("\\&");
        for (int i = 0; i < paramParts.length; i++) {
            String paramPart = paramParts[i];
            if (StringUtils.contains(paramPart, "=")) {
                String key = StringUtils.substringBefore(paramPart, "=");
                String value = StringUtils.substringAfter(paramPart, "=");
                try {
                    addString(URLDecoder.decode(key, this.encoding),
                              URLDecoder.decode(value, this.encoding));
                } catch (UnsupportedEncodingException e) {
                    throw new URLException(
                            "Cannot URL-decode query string (key=" 
                                    + key + "; value=" + value + ").", e);
                }
            }
        }
    }

    /**
     * Convert this <code>QueryString</code> to a URL-encoded string 
     * representation that can be appended as is to a URL with no query string.
     */
    @Override
    public synchronized String toString() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        char sep = '?';
        for (String key : keySet()) {
            for (String value : getStrings(key)) {
                b.append(sep);
                sep = '&';
                try {
                    b.append(URLEncoder.encode(key, encoding));
                    b.append('=');
                    b.append(URLEncoder.encode(value, encoding));
                } catch (UnsupportedEncodingException e) {
                    throw new URLException(
                            "Cannot URL-encode query string (key=" 
                                    + key + "; value=" + value + ").", e);
                }
            }
        }
        return b.toString();
    }
    
    /**
     * Apply this url QueryString on the given URL. If a query string already
     * exists, it is replaced by this one.
     * @param url the URL to apply this query string.
     * @return url with query string added
     */
    public String applyOnURL(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        return StringUtils.substringBefore(url, "?") + toString();
    }
    /**
     * Apply this url QueryString on the given URL. If a query string already
     * exists, it is replaced by this one.
     * @param url the URL to apply this query string.
     * @return url with query string added
     */
    public URL applyOnURL(URL url) {
        if (url == null) {
            return url;
        }
        try {
            return new URL(applyOnURL(url.toString()));
        } catch (MalformedURLException e) {
            throw new URLException("Cannot applyl query string to: " + url, e);
        }
    }
}
