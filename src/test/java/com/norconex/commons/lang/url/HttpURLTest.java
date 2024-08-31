/* Copyright 2015-2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HttpURLTest {

    private final String absURL = "https://www.example.com/a/b/c.html?blah";

    private String s;
    private String t;

    @AfterEach
    void tearDown() throws Exception {
        s = null;
        t = null;
    }

    @ParameterizedTest
    @CsvSource(
        value = {
                """
            	Keep protocol character case,\
            	HTTP://www.example.com,\
            	HTTP://www.example.com""",
                """
            	'http' protocol without port,\
            	http://www.example.com/blah,\
            	http://www.example.com/blah""",
                """
            	'http' protocol with default port,\
            	http://www.example.com:80/blah,\
            	http://www.example.com/blah""",
                """
            	'http' protocol with non-default port,\
            	http://www.example.com:81/blah,\
            	http://www.example.com:81/blah""",
                """
            	'https' protocol without port,\
            	https://www.example.com/blah,\
            	https://www.example.com/blah""",
                """
            	'https' protocol with default port,\
            	https://www.example.com:443/blah,\
            	https://www.example.com/blah""",
                """
            	'https' protocol with non-default port,\
            	https://www.example.com:444/blah,\
            	https://www.example.com:444/blah""",
                """
            	Non 'http(s)' protocol without port,\
            	ftp://ftp.example.com/dir,\
            	ftp://ftp.example.com/dir""",
                """
            	Non 'http(s)' protocol with port,\
            	ftp://ftp.example.com:20/dir,\
            	ftp://ftp.example.com:20/dir""",
                """
            	Invalid URL,\
            	http://www.example.com/"path",\
            	http://www.example.com/%22path%22""",
                """
            	URL with leading or trailing spaces,\
            	  http://www.example.com/path  ,\
            	http://www.example.com/path""",
        },
        ignoreLeadingAndTrailingWhitespace = false
    )
    void testURLToStringEquals(
            String desc, String actual, String expected) {
        assertThat(new HttpURL(actual))
                .describedAs("Test: %s (actual: \"%s\", expected: \"%s\")",
                        desc, actual, expected)
                .hasToString(expected);
    }

    @ParameterizedTest
    @CsvSource(
        value = {
                """
            	Relative to protocol,\
            	//www.relative.com/e/f.html,\
            	https://www.relative.com/e/f.html""",
                """
            	Relative to domain name,\
            	/e/f.html,\
            	https://www.example.com/e/f.html""",
                """
            	Relative to full page URL,\
            	?name=john,\
            	https://www.example.com/a/b/c.html?name=john""",
                """
            	Relative to last directory,\
            	g.html,\
            	https://www.example.com/a/b/g.html""",
                """
            	Absolute URL,\
            	http://www.sample.com/xyz.html,\
            	http://www.sample.com/xyz.html""",
                """
            	Relative with colon in parameter,\
            	h/i.html?param=1:2,\
            	https://www.example.com/a/b/h/i.html?param=1:2""",
                """
            	Starts with valid odd scheme,\
            	x1+2-3.4:yz,\
            	x1+2-3.4:yz""",
                """
            	Starts with invalid odd scheme,\
            	1+2-3.4x:yz,\
            	https://www.example.com/a/b/1+2-3.4x:yz""",
        },
        ignoreLeadingAndTrailingWhitespace = false
    )
    void testURLToAbsoluteEquals(
            String desc, String actual, String expected) {
        assertThat(HttpURL.toAbsolute(absURL, actual))
                .describedAs(
                        "Test absolute: %s (actual: \"%s\", expected: \"%s\")",
                        desc, actual, expected)
                .isEqualTo(expected);
    }

    //Test for issue https://github.com/Norconex/collector-http/issues/225
    @Test
    void testFromDomainNoTrailSlashToRelativeNoLeadSlash() {
        s = "http://www.sample.com";
        t = "http://www.sample.com/xyz.html";
        assertEquals(t, HttpURL.toAbsolute(s, "xyz.html"));
    }

    @Test
    void testNullOrBlankURLs() {
        s = null;
        t = "";
        assertEquals(t, new HttpURL(s).toString());
        s = "";
        t = "";
        assertEquals(t, new HttpURL(s).toString());
        s = "  ";
        t = "";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testRelativeURLs() {
        s = "./blah";
        t = "./blah";
        assertEquals(t, new HttpURL(s).toString());
        s = "/blah";
        t = "/blah";
        assertEquals(t, new HttpURL(s).toString());
        s = "blah?param=value#frag";
        t = "blah?param=value#frag";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testFileProtocol() {
        // Encode non-URI characters
        s = "file:///etc/some dir/my file.txt";
        t = "file:///etc/some%20dir/my%20file.txt";
        assertEquals(t, new HttpURL(s).toString());

        s = "file://./dir/another-dir/path";
        t = "file://./dir/another-dir/path";
        assertEquals(t, new HttpURL(s).toString());

        s = "file://localhost/c:/WINDOWS/éà.txt";
        t = "file://localhost/c:/WINDOWS/%C3%A9%C3%A0.txt";
        assertEquals(t, new HttpURL(s).toString());

        s = "file:///c:/WINDOWS/file.txt";
        t = "file:///c:/WINDOWS/file.txt";
        assertEquals(t, new HttpURL(s).toString());

        s = "file:/c:/WINDOWS/file.txt";
        t = "file:///c:/WINDOWS/file.txt";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testMisc() throws MalformedURLException, URISyntaxException {
        assertThat(new HttpURL().getHost()).isNull();
        var url = new URL("http://example.com/some/path.html?param1=value1#A1");
        assertThat(new HttpURL(url).toURL()).isEqualTo(url);
        assertThat(new HttpURL(url, "UTF-8").toURL()).isEqualTo(url);

        assertThatExceptionOfType(UrlException.class).isThrownBy(
                () -> new HttpURL("blah:I am invalid"));

        var httpUrl = new HttpURL(url, "UTF-8");
        assertThat(httpUrl)
                .returns("UTF-8", HttpURL::getEncoding)
                .returns("/some/path.html", HttpURL::getPath)
                .returns(new QueryString(url), HttpURL::getQueryString)
                .returns("http", HttpURL::getProtocol)
                .returns(false, HttpURL::isSecure)
                .returns(80, HttpURL::getPort)
                .returns("A1", HttpURL::getFragment)
                .returns("path.html", HttpURL::getLastPathSegment)
                .returns("http://example.com", HttpURL::getRoot)
                .returns(url.toURI(), HttpURL::toURI);

        httpUrl = new HttpURL();
        httpUrl.setPath("/some/path.html");
        httpUrl.setHost("example.com");
        httpUrl.setProtocol("https");
        httpUrl.setPort(443);
        httpUrl.setFragment("A1");
        assertThat(httpUrl).hasToString(
                "https://example.com/some/path.html#A1");

        assertThat(HttpURL.toURL("http://example.com/some path"))
                .hasToString("http://example.com/some%20path");
        assertThat(HttpURL.toURI("http://example.com/some path"))
                .hasToString("http://example.com/some%20path");

        assertThat(HttpURL.getRoot(null)).isNull();
    }

    @Test
    void testGetRoot() throws MalformedURLException, URISyntaxException {
        assertThat(HttpURL.getRoot("http://acme.com"))
                .isEqualTo("http://acme.com");
        assertThat(HttpURL.getRoot("http://acme.com/"))
                .isEqualTo("http://acme.com");
        assertThat(HttpURL.getRoot("http://acme.com/asdf"))
                .isEqualTo("http://acme.com");
        assertThat(HttpURL.getRoot("http://acme.com/asdf/"))
                .isEqualTo("http://acme.com");
        assertThat(HttpURL.getRoot("http://acme.com?adsf=asdf"))
                .isEqualTo("http://acme.com");
        assertThat(HttpURL.getRoot("http://acme.com#asdf"))
                .isEqualTo("http://acme.com");
        assertThat(HttpURL.getRoot("http://acme.com%123"))
                .isEqualTo("http://acme.com%123");
    }

}