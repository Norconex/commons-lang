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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Provides a quick and easy way to stream a URL.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
@SuppressWarnings("nls")
public final class URLStreamer {

    private static final Logger LOG = LogManager.getLogger(URLStreamer.class);
    
    private URLStreamer() {
        super();
    }

    public static InputStream stream(String url) {
        return stream(url, null);
    }
    public static InputStream stream(URL url) {
        return stream(url.toString(), null);
    }
    public static InputStream stream(HttpURL url) {
        return stream(url.toString(), null);
    }

    public static InputStream stream(String url, Credentials creds) {
        return stream(url, creds, null);
    }
    public static InputStream stream(URL url, Credentials creds) {
        return stream(url.toString(), creds, null);
    }
    public static InputStream stream(HttpURL url, Credentials creds) {
        return stream(url.toString(), creds, null);
    }
    
    public static InputStream stream(
            String url, Credentials creds, HttpHost proxy) {
        return stream(url, creds, proxy, null);
    }
    public static InputStream stream(
            URL url, Credentials creds, HttpHost proxy) {
        return stream(url.toString(), creds, proxy, null);
    }
    public static InputStream stream(
            HttpURL url, Credentials creds, HttpHost proxy) {
        return stream(url.toString(), creds, proxy, null);
    }

    public static InputStream stream(
            String url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            if (proxy != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Streaming with proxy: "
                            + proxy.getHostName() + ":" + proxy.getPort());
                }
                client.getParams().setParameter(
                        ConnRoutePNames.DEFAULT_PROXY, proxy);
                if (proxyCreds != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Streaming with proxy credentials.");
                    }
                    client.getCredentialsProvider().setCredentials(
                            new AuthScope(proxy.getHostName(), proxy.getPort()),
                            proxyCreds);
                }
            }
            
            if (creds != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Streaming with credentials.");
                }
                client.getCredentialsProvider().setCredentials(new AuthScope(
                        AuthScope.ANY_HOST, HttpURL.DEFAULT_HTTP_PORT,
                        AuthScope.ANY_REALM), creds);
            }
            
            HttpGet call = new HttpGet(url);
            HttpResponse response = client.execute(call);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = 
                        new BasicResponseHandler();
                LOG.error("Invalid HTTP response: " + statusCode
                        + ". Response body is: " 
                        + responseHandler.handleResponse(response));
                throw new IOException("Cannot stream URL: " + url);
            }
            return response.getEntity().getContent();
        } catch (IOException e) {
            throw new URLException("Could not stream URL: " + url, e);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }    
    public static InputStream stream(
            URL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return stream(url.toString(), creds, proxy, proxyCreds);
    }
    public static InputStream stream(
            HttpURL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return stream(url.toString(), creds, proxy, proxyCreds);
    }

    
    public static String streamToString(
            String url, Credentials creds, HttpHost proxy) {
        StopWatch watch = null;
        if (LOG.isDebugEnabled()) {
            watch = new StopWatch();
            watch.start();
            LOG.debug("Streaming URL:" + url);
        }
        String out;
        try {
            out = IOUtils.toString(stream(url, creds, proxy));
        } catch (IOException e) {
            throw new URLException("Could not stream URL to string: " + url, e);
        }
        if (LOG.isDebugEnabled()) {
            watch.stop();
            LOG.debug("Streaming elapsed time: " + watch.toString());
        }
        return out;
    }
    public static String streamToString(
            URL url, Credentials creds, HttpHost proxy) {
        return streamToString(url.toString(), creds, proxy);
    }
    public static String streamToString(
            HttpURL url, Credentials creds, HttpHost proxy) {
        return streamToString(url.toString(), creds, proxy);
    }

    public static String streamToString(
            String url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        StopWatch watch = null;
        if (LOG.isDebugEnabled()) {
            watch = new StopWatch();
            watch.start();
            LOG.debug("Streaming URL:" + url);
        }
        String out;
        try {
            out = IOUtils.toString(stream(url, creds, proxy, proxyCreds));
        } catch (IOException e) {
            throw new URLException("Could not stream URL to string: " + url, e);
        }
        if (LOG.isDebugEnabled()) {
            watch.stop();
            LOG.debug("Streaming elapsed time: " + watch.toString());
        }
        return out;
    }
    public static String streamToString(
            URL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return streamToString(url.toString(), creds, proxy, proxyCreds);
    }
    public static String streamToString(
            HttpURL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return streamToString(url.toString(), creds, proxy, proxyCreds);
    }
    
    public static String streamToString(String url, Credentials creds) {
        return streamToString(url, creds, null);
    }
    public static String streamToString(URL url, Credentials creds) {
        return streamToString(url.toString(), creds, null);
    }
    public static String streamToString(HttpURL url, Credentials creds) {
        return streamToString(url.toString(), creds, null);
    }
}
