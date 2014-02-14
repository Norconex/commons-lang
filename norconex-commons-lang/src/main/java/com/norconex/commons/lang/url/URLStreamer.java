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
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Provides a quick and easy way to stream a URL.
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public final class URLStreamer {

    private static final Logger LOG = LogManager.getLogger(URLStreamer.class);
    
    private URLStreamer() {
        super();
    }

    /**
     * Streams URL content.
     * @param url the URL to stream
     * @return a URL content InputStream
     */
    public static InputStream stream(String url) {
        return stream(url, null);
    }
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @return a URL content InputStream
     */
    public static InputStream stream(URL url) {
        return stream(url.toString(), null);
    }
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @return a URL content InputStream
     */
    public static InputStream stream(HttpURL url) {
        return stream(url.toString(), null);
    }

    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @return a URL content InputStream
     */
    public static InputStream stream(String url, Credentials creds) {
        return stream(url, creds, null);
    }
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @return a URL content InputStream
     */
    public static InputStream stream(URL url, Credentials creds) {
        return stream(url.toString(), creds, null);
    }
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @return a URL content InputStream
     */
    public static InputStream stream(HttpURL url, Credentials creds) {
        return stream(url.toString(), creds, null);
    }
    
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @return a URL content InputStream
     */
    public static InputStream stream(
            String url, Credentials creds, HttpHost proxy) {
        return stream(url, creds, proxy, null);
    }
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @return a URL content InputStream
     */
    public static InputStream stream(
            URL url, Credentials creds, HttpHost proxy) {
        return stream(url.toString(), creds, proxy, null);
    }
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @return a URL content InputStream
     */
    public static InputStream stream(
            HttpURL url, Credentials creds, HttpHost proxy) {
        return stream(url.toString(), creds, proxy, null);
    }

    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @param proxyCreds credentials to access the proxy
     * @return a URL content InputStream
     */
    public static InputStream stream(
            String url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        
        CloseableHttpClient client = null;
        HttpClientBuilder httpBuilder = HttpClientBuilder.create();
        try {
            CredentialsProvider credsProvider = null;
            if (proxy != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Streaming with proxy: "
                            + proxy.getHostName() + ":" + proxy.getPort());
                }
                httpBuilder.setProxy(proxy);
                if (proxyCreds != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Streaming with proxy credentials.");
                    }
                    credsProvider = new BasicCredentialsProvider();
                    credsProvider.setCredentials(new AuthScope(
                            proxy.getHostName(), proxy.getPort()), proxyCreds);
                }
            }
            if (creds != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Streaming with credentials.");
                }
                if (credsProvider == null) {
                    credsProvider = new BasicCredentialsProvider();
                }                    
                credsProvider.setCredentials(new AuthScope(
                        AuthScope.ANY_HOST, HttpURL.DEFAULT_HTTP_PORT,
                        AuthScope.ANY_REALM), creds);
            }
            if (credsProvider != null) {
                httpBuilder.setDefaultCredentialsProvider(credsProvider);
            }
            
            final CloseableHttpClient finalClient = httpBuilder.build();
            client = finalClient;
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
            return new AutoCloseInputStream(response.getEntity().getContent()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    if (finalClient != null) {
                        IOUtils.closeQuietly(finalClient);
                    }
                }
            };
        } catch (IOException e) {
            if (client != null) {
                IOUtils.closeQuietly(client);
            }
            throw new URLException("Could not stream URL: " + url, e);
        }
    }    
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @param proxyCreds credentials to access the proxy
     * @return a URL content InputStream
     */
    public static InputStream stream(
            URL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return stream(url.toString(), creds, proxy, proxyCreds);
    }
    /**
     * Streams URL content.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @param proxyCreds credentials to access the proxy
     * @return a URL content InputStream
     */
    public static InputStream stream(
            HttpURL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return stream(url.toString(), creds, proxy, proxyCreds);
    }

    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @return a URL content as a String
     */
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
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @return a URL content as a String
     */
    public static String streamToString(
            URL url, Credentials creds, HttpHost proxy) {
        return streamToString(url.toString(), creds, proxy);
    }
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @return a URL content as a String
     */
    public static String streamToString(
            HttpURL url, Credentials creds, HttpHost proxy) {
        return streamToString(url.toString(), creds, proxy);
    }

    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @param proxyCreds credentials to access the proxy
     * @return a URL content as a String
     */
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
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @param proxyCreds credentials to access the proxy
     * @return a URL content as a String
     */
    public static String streamToString(
            URL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return streamToString(url.toString(), creds, proxy, proxyCreds);
    }
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @param proxy proxy to use to stream the URL
     * @param proxyCreds credentials to access the proxy
     * @return a URL content as a String
     */
    public static String streamToString(
            HttpURL url, Credentials creds, HttpHost proxy, 
            Credentials proxyCreds) {
        return streamToString(url.toString(), creds, proxy, proxyCreds);
    }
    
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @return a URL content as a String
     */
    public static String streamToString(String url, Credentials creds) {
        return streamToString(url, creds, null);
    }
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @return a URL content as a String
     */
    public static String streamToString(URL url, Credentials creds) {
        return streamToString(url.toString(), creds, null);
    }
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @param creds credentials for a protected URL
     * @return a URL content as a String
     */
    public static String streamToString(HttpURL url, Credentials creds) {
        return streamToString(url.toString(), creds, null);
    }
    
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @return a URL content as a String
     * @since 1.3
     */
    public static String streamToString(String url) {
        return streamToString(url, null);
    }
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @return a URL content as a String
     * @since 1.3
     */
    public static String streamToString(URL url) {
        return streamToString(url, null);
    }    
    /**
     * Streams URL content to a String.
     * @param url the URL to stream
     * @return a URL content as a String
     * @since 1.3
     */
    public static String streamToString(HttpURL url) {
        return streamToString(url.toString(), null);
    }
}
