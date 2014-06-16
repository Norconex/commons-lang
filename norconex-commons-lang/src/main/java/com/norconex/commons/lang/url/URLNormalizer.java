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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.EqualsUtil;

/**
 * <p>
 * The general idea behind URL normalization is to make different URLs 
 * "equivalent" (i.e. eliminate URL variations pointing to the same resource).
 * To achieve this, 
 * <code>URLNormalizer</code> takes a URL and modifies it to its 
 * most basic or standard form (for the context in which it is used).
 * Of course <code>URLNormalizer</code> can simply be used as a generic
 * URL manipulation tool for your needs.
 * </p>
 * <p>
 * You would typically "build" your normalized URL by invoking each method
 * of interest, in the relevant order, using a similar approach:
 * </p>
 * <pre>
 * String url = "Http://Example.com:80//foo/index.html";
 * URL normalizedURL = new URLNormalizer(url)
 *         .lowerCaseSchemeHost()
 *         .removeDefaultPort()
 *         .removeDuplicateSlashes()
 *         .removeDirectoryIndex()
 *         .addWWW()
 *         .toURL();
 * System.out.println(normalizedURL.toString());
 * // Output: http://www.example.com/foo/</pre>
 * <p>
 * Several normalization methods implemented come from the 
 * <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a> standard.
 * These standards and several more normalization techniques
 * are very well summarized on the Wikipedia article titled 
 * <i><a href="http://en.wikipedia.org/wiki/URL_normalization">
 * URL Normalization</a></i>.
 * This class implements most normalizations described on that article and
 * borrows several of its examples, as well as a few additional ones. 
 * </p>
 * <p>
 * The normalization methods available can be broken down into three 
 * categories:
 * </p>
 * 
 * <h3>Preserving Semantics</h3>
 * <p>
 * The following normalizations are part of the 
 * <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a> standard 
 * and should result in equivalent 
 * URLs (one that identifies the same resource):
 * </p>
 * <ul>
 *   <li>{@link #lowerCaseSchemeHost() 
 *       Convert scheme and host to lower case}</li>
 *   <li>{@link #upperCaseEscapeSequence()
 *       Convert escape sequence to uppercase}</li>
 *   <li>{@link #decodeUnreservedCharacters()
 *       Decode percent-encoded unreserved characters}</li>
 *   <li>{@link #removeDefaultPort() Removing default ports}</li>
 * </ul>
 * 
 * <h3>Usually Preserving Semantics</h3>
 * <p>
 * The following techniques will generate a semantically equivalent URL for 
 * the majority of use cases but are not enforced as a standard.
 * </p>
 * <ul>
 *   <li>{@link #addTrailingSlash() Add trailing slash}</li>
 *   <li>{@link #removeDotSegments() Remove .dot segments}</li>
 * </ul>
 * 
 * <h3>Not Preserving Semantics</h3>
 * <p>
 * These normalizations will fail to produce semantically equivalent URLs in
 * many cases.  They usually work best when you have a good understanding of 
 * the website behind the supplied URL and whether for that site, 
 * which normalizations can be be considered to produce semantically equivalent 
 * URLs or not.
 * </p>
 * <ul>
 *   <li>{@link #removeDirectoryIndex() Remove directory index}</li>
 *   <li>{@link #removeFragment() Remove fragment (#)}</li>
 *   <li>{@link #replaceIPWithDomainName() Replace IP with domain name}</li>
 *   <li>{@link #unsecureScheme() Unsecure schema (https &rarr; http)}</li>
 *   <li>{@link #secureScheme() Secure schema (http &rarr; https)}</li>
 *   <li>{@link #removeDuplicateSlashes() Remove duplicate slashes}</li>
 *   <li>{@link #removeWWW() Remove "www."}</li>
 *   <li>{@link #addWWW() Add "www."}</li>
 *   <li>{@link #sortQueryParameters() Sort query parameters}</li>
 *   <li>{@link #removeEmptyParameters() Remove empty query parameters}</li>
 *   <li>{@link #removeTrailingQuestionMark() Remove trailing question mark (?)}</li>
 *   <li>{@link #removeSessionIds() Remove session IDs}</li>
 * </ul>
 * <p>
 * Refer to each methods below for description and examples (or click on a
 * normalization name above).
 * </p>
 * @author Pascal Essiembre
 */
public class URLNormalizer implements Serializable {

    private static final long serialVersionUID = 7236478212865008971L;

    private static final Logger LOG = LogManager.getLogger(
            URLNormalizer.class);
    
    private static final Pattern PATTERN_PERCENT_ENCODED_CHAR = 
            Pattern.compile("(%[0-9a-f]{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_PATH_LAST_SEGMENT = Pattern.compile(
            "(.*/)(index\\.html|index\\.htm|index\\.shtml|index\\.php"
          + "|default\\.html|default\\.htm|home\\.html|home\\.htm|index\\.php5"
          + "|index\\.php4|index\\.php3|index\\.cgi|placeholder\\.html"
          + "|default\\.asp)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DOMAIN = Pattern.compile(
            "^[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SCHEMA = Pattern.compile(
            "(.*?)(://.*)$",
            Pattern.CASE_INSENSITIVE);
    
    private String url;

    /**
     * Create a new <code>URLNormalizer</code> instance.
     * @param url the url to normalize
     */
    public URLNormalizer(URL url) {
        this(url.toString());
    }

    /**
     * Create a new <code>URLNormalizer</code> instance.
     * @param url the url to normalize
     */
    public URLNormalizer(String url) {
        super();
        // make sure URL is valid
        String fixedURL = url;
        try {
            if (StringUtils.contains(fixedURL, " ")) {
                LOG.warn("URL syntax is invalid as it contains space "
                        + "character(s). Replacing them with %20. URL: " + url);
                fixedURL = StringUtils.replace(fixedURL, " ", "%20");
            }
            new URI(fixedURL);
        } catch (URISyntaxException e) {
            throw new URLException("Invalid URL syntax: " + url, e);
        }
        if (!CharEncoding.isSupported(CharEncoding.UTF_8)) {
            throw new URLException(
                    "UTF-8 is not supported by your system.");
        }
        this.url = fixedURL.trim();
    }

    /**
     * Converts the scheme and host to lower case.<p>
     * <code>HTTP://www.Example.com/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer lowerCaseSchemeHost() {
        URI u = toURI(url);
        url = Pattern.compile(u.getScheme(), 
                Pattern.CASE_INSENSITIVE).matcher(url).replaceFirst(
                        u.getScheme().toLowerCase());
        url = Pattern.compile(u.getHost(), 
                Pattern.CASE_INSENSITIVE).matcher(url).replaceFirst(
                        u.getHost().toLowerCase());
        return this;
    }
    /**
     * Converts letters in URL-encoded escape sequences to upper case.<p>
     * <code>http://www.example.com/a%c2%b1b &rarr; 
     *       http://www.example.com/a%C2%B1b</code></li>
     * @return this instance
     */
    public URLNormalizer upperCaseEscapeSequence() {
        if (url.contains("%")) {
            StringBuffer sb = new StringBuffer();
            Matcher m = PATTERN_PERCENT_ENCODED_CHAR.matcher(url);
            while (m.find()) {
                m.appendReplacement(sb, m.group(1).toUpperCase());
            }
            url = m.appendTail(sb).toString();
        }
        return this;
    }
    /**
     * Decodes percent-encoded unreserved characters.<p>
     * <code>http://www.example.com/%7Eusername/ &rarr;
     *       http://www.example.com/~username/</code>
     * @return this instance
     */
    public URLNormalizer decodeUnreservedCharacters() {
        if (url.contains("%")) {
            StringBuffer sb = new StringBuffer();
            Matcher m = PATTERN_PERCENT_ENCODED_CHAR.matcher(url);
            try {
                while (m.find()) {
                    String enc = m.group(1).toUpperCase();
                    if (isEncodedUnreservedCharacter(enc)) {
                        m.appendReplacement(sb, 
                                URLDecoder.decode(enc, CharEncoding.UTF_8));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                LOG.debug("UTF-8 is not supported by your system. "
                        + "URL will remain unchanged:" + url, e);
            }
            url = m.appendTail(sb).toString();
        }
        return this;
    }
    /**
     * Removes the default port (80 for http, and 443 for https).<p>
     * <code>http://www.example.com:80/bar.html &rarr; 
     *       http://www.example.com/bar.html</code>
     * @return this instance
     */
    public URLNormalizer removeDefaultPort() {
        URI u = toURI(url);
        if ("http".equalsIgnoreCase(u.getScheme())
                && u.getPort() == HttpURL.DEFAULT_HTTP_PORT) {
            url = url.replaceFirst(":" + HttpURL.DEFAULT_HTTP_PORT, "");
        } else if ("https".equalsIgnoreCase(u.getScheme()) 
                && u.getPort() == HttpURL.DEFAULT_HTTPS_PORT) {
            url = url.replaceFirst(":" + HttpURL.DEFAULT_HTTPS_PORT, "");
        }
        return this;
    }
    /**
     * <p>Adds a trailing slash (/) to a URL ending with a directory.  A URL is 
     * considered to end with a directory if the last path segment,
     * before fragment (#) or query string (?), does not contain a dot,
     * typically representing an extension.<p>
     *   
     * <p><b>Please Note:</b> URLs do not always denote a directory structure 
     * and many URLs can qualify to this method without truly representing a 
     * directory. Adding a trailing slash to these URLs could potentially break
     * its semantic equivalence.</p>
     * <code>http://www.example.com/alice &rarr; 
     *       http://www.example.com/alice/</code>
     * @return this instance
     */
    public URLNormalizer addTrailingSlash() {
        String name = StringUtils.substringAfterLast(url, "/");
        if (!name.contains(".") && !StringUtils.endsWith(name, "/")) {
            url = url + "/";
        }
        return this;
    }    
    
    /**
     * <p>Removes the unnecessary "." and ".." segments from the URL path.
     * {@link URI#normalize()} is invoked to perform this normalization.
     * Refer to it for exact behavior.</p>
     * <code>http://www.example.com/../a/b/../c/./d.html &rarr;
     *       http://www.example.com/a/c/d.html</code>
     * <p><b>Please Note:</b> URLs do not always represent a clean hierarchy 
     * structure and the dots/double-dots may have a different signification
     * on some sites.  Removing them from a URL could potentially break
     * its semantic equivalence.</p>
     * 
     * @return this instance
     * @see URI#normalize()
     */
    public URLNormalizer removeDotSegments() {
        url = toURI(url).normalize().toString();
        return this;
    }
    /**
     * <p>Removes directory index files.  They are often not needed in URLs.</p>
     * <code>http://www.example.com/a/index.html &rarr;
     *       http://www.example.com/a/</code>
     * <p>Index files must be the last URL path segment to be considered.
     * The following are considered index files:</p>
     * <ul>
     *   <li>index.html</li>
     *   <li>index.htm</li>
     *   <li>index.shtml</li>
     *   <li>index.php</li>
     *   <li>default.html</li>
     *   <li>default.htm</li>
     *   <li>home.html</li>
     *   <li>home.htm</li>
     *   <li>index.php5</li>
     *   <li>index.php4</li>
     *   <li>index.php3</li>
     *   <li>index.cgi</li>
     *   <li>placeholder.html</li>
     *   <li>default.asp</li>
     * </ul>
     * <p><b>Please Note:</b> There are no guarantees a URL without its
     * index files will be semantically equivalent, or even be valid.</p>
     * @return this instance
     */
    public URLNormalizer removeDirectoryIndex() {
        String path = toURI(url).getPath();
        if (PATTERN_PATH_LAST_SEGMENT.matcher(path).matches()) {
            url = StringUtils.replaceOnce(
                   url, path, StringUtils.substringBeforeLast(path, "/") + "/");
        }
        return this;
    }
    /**
     * <p>Removes the URL fragment (from the "#" character until the end).</p>
     * <code>http://www.example.com/bar.html#section1 &rarr; 
     *       http://www.example.com/bar.html</code>
     * @return this instance
     */
    public URLNormalizer removeFragment() {
        url = url.replaceFirst("(.*?)(#.*)", "$1");
        return this;
    }
    /**
     * <p>Replaces IP address with domain name.  This is often not
     * reliable due to virtual domain names and can be slow, as it has
     * to access the network.</p>
     * <code>http://208.77.188.166/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer replaceIPWithDomainName() {
        URI u = toURI(url);
        if (!PATTERN_DOMAIN.matcher(u.getHost()).matches()) {
            try {
                InetAddress addr = InetAddress.getByName(u.getHost());
                String host = addr.getHostName();
                if (!u.getHost().equalsIgnoreCase(host)) {
                    url = url.replaceFirst(u.getHost(), host);
                }
            } catch (UnknownHostException e) {
                LOG.debug("Cannot resolve IP to host for :" + u.getHost(), e);
            }
        }
        return this;
    }
    /**
     * <p>Converts <code>https</code> scheme to <code>http</code>.</p>
     * <code>https://www.example.com/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer unsecureScheme() {
        Matcher m = PATTERN_SCHEMA.matcher(url);
        if (m.find()) {
            String schema = m.group(1);
            if ("https".equalsIgnoreCase(schema)) {
                url = m.replaceFirst(StringUtils.stripEnd(schema, "Ss") + "$2");
            }
        }
        return this;
    }
    /**
     * <p>Converts <code>http</code> scheme to <code>https</code>.</p>
     * <code>http://www.example.com/ &rarr; https://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer secureScheme() {
        Matcher m = PATTERN_SCHEMA.matcher(url);
        if (m.find()) {
            String schema = m.group(1);
            if ("http".equalsIgnoreCase(schema)) {
                url = m.replaceFirst(schema + "s$2");
            }
        }
        return this;
    }
    /**
     * <p>Removes duplicate slashes.  Two or more adjacent slash ("/") 
     * characters will be converted into one.</p>
     * <code>http://www.example.com/foo//bar.html 
     *       &rarr; http://www.example.com/foo/bar.html </code>
     * @return this instance
     */
    public URLNormalizer removeDuplicateSlashes() {
        String path = toURI(url).getPath();
        String newPath = path.replaceAll("/{2,}", "/");
        url = StringUtils.replaceOnce(url, path, newPath);
        return this;
    }
    /**
     * <p>Removes "www." domain name prefix.</p>
     * <code>http://www.example.com/ &rarr; http://example.com/</code>
     * @return this instance
     */
    public URLNormalizer removeWWW() {
        String host = toURI(url).getHost();
        String newHost = StringUtils.removeStartIgnoreCase(host, "www.");
        url = StringUtils.replaceOnce(url, host, newHost);
        return this;
    }
    /**
     * <p>Adds "www." domain name prefix.</p>
     * <code>http://example.com/ &rarr; http://www.example.com/</code>
     * @return this instance
     */
    public URLNormalizer addWWW() {
        String host = toURI(url).getHost();
        if (!host.toLowerCase().startsWith("www.")) {
            url = StringUtils.replaceOnce(url, host, "www." + host);
        }
        return this;
    }
    /**
     * <p>Sorts query parameters.</p>
     * <code>http://www.example.com/?z=bb&y=cc&z=aa &rarr;
     *       http://www.example.com/?y=cc&z=bb&z=aa</code>
     * @return this instance
     */
    public URLNormalizer sortQueryParameters() {
        if (url.contains("?")) {
            // QueryString extends Properties which already has sorted keys.
            QueryString q = new HttpURL(url).getQueryString();
            if (q != null) {
                url = StringUtils.substringBefore(url, "?") + q.toString();
            }
        }
        return this;
    }
    /**
     * <p>Removes empty parameters.</p>
     * <code>http://www.example.com/display?a=b&a=&c=d&e=&f=g &rarr;
     *       http://www.example.com/display?a=b&c=d&f=g</code>
     * @return this instance
     */
    public URLNormalizer removeEmptyParameters() {
        QueryString q = new HttpURL(url).getQueryString();
        if (q != null) {
            QueryString newq = new QueryString();
            for (String key : q.keySet()) {
                List<String> values = q.get(key);
                if (values == null || values.isEmpty()) {
                    continue;
                }
                for (String value : values) {
                    if (StringUtils.isNotBlank(value)) {
                        newq.addString(key, value);
                    }
                }
            }
            url = newq.applyOnURL(url);
        }        
        return this;
    }
    /**
     * <p>Removes trailing question mark ("?").</p>
     * <code>http://www.example.com/display? &rarr;
     *       http://www.example.com/display </code>
     * @return this instance
     */
    public URLNormalizer removeTrailingQuestionMark() {
        if (url.endsWith("?") && StringUtils.countMatches(url, "?") == 1) {
            url = StringUtils.removeEnd(url, "?");
        }
        return this;
    }
    /**
     * <p>Removes a URL-based session id.  It removes PHP (PHPSESSID),
     * ASP (ASPSESSIONID), and Java EE (jsessionid) session ids.</p>
     * <code>http://www.example.com/servlet;jsessionid=1E6FEC0D14D044541DD84D2D013D29ED?a=b
     * &rarr; http://www.example.com/servlet?a=b</code>
     * <p><b>Please Note:</b> Removing session IDs from URLs is often 
     * a good way to have the URL return an error once invoked.</p>
     * @return this instance
     */
    public URLNormalizer removeSessionIds() {
        if (StringUtils.containsIgnoreCase(url, ";jsessionid=")) {
            url = url.replaceFirst("(;jsessionid=[0-9a-fA-F]*)", "");
        } else {
            String u = StringUtils.substringBefore(url, "?");
            String q = StringUtils.substringAfter(url, "?");
            if (StringUtils.containsIgnoreCase(url, "PHPSESSID=")) {
                q = q.replaceFirst("(&|^)(PHPSESSID=[0-9a-zA-Z]*)", "");
            } else if (StringUtils.containsIgnoreCase(url, "ASPSESSIONID")) {
                q = q.replaceFirst(
                        "(&|^)(ASPSESSIONID[a-zA-Z]{8}=[a-zA-Z]*)", "");
            }
            if (!StringUtils.isBlank(q)) {
                u += "?" + StringUtils.removeStart(q, "&");
            }
            url = u;
        }
        return this;
    }
        
    /**
     * Returns the normalized URL as string.
     * @return URL
     */
    @Override
    public String toString() {
        return url;
    }
    /**
     * Returns the normalized URL as {@link URI}.
     * @return URI
     */
    public URI toURI() {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            LOG.info("URL does not appear to be valid and cannot be parsed:"
                    + url, e);
            return null;
        }
    }
    /**
     * Returns the normalized URL as {@link URL}.
     * @return URI
     */
    public URL toURL() {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            LOG.info("URL does not appear to be valid and cannot be parsed:"
                    + url, e);
            return null;
        }
    }

    private static URI toURI(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            LOG.info("URL does not appear to be valid and cannot be parsed:"
                    + url);
            return null;
        }
    }
    
    private boolean isEncodedUnreservedCharacter(String enc) {
        // is ALPHA (a-zA-Z)
        if ((enc.compareTo("%41") >= 0 && enc.compareTo("%5A") <= 0)
         || (enc.compareTo("%61") >= 0 && enc.compareTo("%7A") <= 0)) {
            return true;
        }
        // is Digit (0-9)
        if (enc.compareTo("%30") >= 0 && enc.compareTo("%39") <= 0) {
            return true;
        }
        // is hyphen, period, underscore, tilde
        return EqualsUtil.equalsAny(enc, "%2D", "%2E", "%5F", "%7E");
    }
}
