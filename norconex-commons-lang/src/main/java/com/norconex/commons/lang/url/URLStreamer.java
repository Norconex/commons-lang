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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang3.time.StopWatch;
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
        try {
            URLConnection conn;
            if (proxy != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Streaming with proxy: "
                            + proxy.getHostName() + ":" + proxy.getPort());
                }
                Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        proxy.getHostName(), proxy.getPort()));
                //Authenticator.
                conn = new URL(url).openConnection(p);
                if (proxyCreds != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Streaming with proxy credentials.");
                    }
                    conn.setRequestProperty("Proxy-Authorization", 
                            base64BasicAuth(proxyCreds.getUsername(), 
                                    proxyCreds.getPassword()));
                }
            } else {
                conn = new URL(url).openConnection();
            }
            if (creds != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Streaming with credentials.");
                }
                conn.setRequestProperty("Authorization", base64BasicAuth(
                        creds.getUsername(), creds.getPassword()));
            }
            return responseInputStream(conn);
        } catch (IOException e) {
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
        if (LOG.isDebugEnabled() && watch != null) {
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
        if (LOG.isDebugEnabled() && watch != null) {
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
    
    private static InputStream responseInputStream(
            URLConnection conn) throws IOException {
        conn.connect();
        return new AutoCloseInputStream(conn.getInputStream());
    }
    private static String base64BasicAuth(String username, String password) {
        String userpass = username + ':' + password;
        return "Basic " 
                + DatatypeConverter.printBase64Binary(userpass.getBytes());
    }

    
    
    public static class Credentials {
        private final String username;
        private final String password;
        public Credentials(String username, String password) {
            super();
            this.username = username;
            this.password = password;
        }
        public String getUsername() {
            return username;
        }
        public String getPassword() {
            return password;
        }
    }

    
    public static class HttpHost {
        private final String hostName;
        private final int port;
        public HttpHost(String hostName, int port) {
            super();
            this.hostName = hostName;
            this.port = port;
        }
        public String getHostName() {
            return hostName;
        }
        public int getPort() {
            return port;
        }
    }
}
