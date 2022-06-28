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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpURLTest {

    private final String absURL = "https://www.example.com/a/b/c.html?blah";

    private String s;
    private String t;

    @AfterEach
    void tearDown() throws Exception {
        s = null;
        t = null;
    }

    @Test
    void testKeepProtocolUpperCase() {
        s = "HTTP://www.example.com";
        t = "HTTP://www.example.com";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testToAbsoluteRelativeToProtocol() {
        s = "//www.relative.com/e/f.html";
        t = "https://www.relative.com/e/f.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    void testToAbsoluteRelativeToDomainName() {
        s = "/e/f.html";
        t = "https://www.example.com/e/f.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    void testToAbsoluteRelativeToFullPageURL() {
        s = "?name=john";
        t = "https://www.example.com/a/b/c.html?name=john";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    void testToAbsoluteRelativeToLastDirectory() {
        s = "g.html";
        t = "https://www.example.com/a/b/g.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    void testToAbsoluteAbsoluteURL() {
        s = "http://www.sample.com/xyz.html";
        t = "http://www.sample.com/xyz.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }

    //Test for issue https://github.com/Norconex/collector-http/issues/788
    @Test
    void testToAbsoluteRelativeWithColon() {
        s = "h/i.html?param=1:2";
        t = "https://www.example.com/a/b/h/i.html?param=1:2";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    void testToAbsoluteStartsWithScheme() {
        s = "x1+2-3.4:yz";
        t = "x1+2-3.4:yz";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
        s = "1+2-3.4x:yz";
        t = "https://www.example.com/a/b/1+2-3.4x:yz";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }

    //Test for issue https://github.com/Norconex/collector-http/issues/225
    @Test
    void testFromDomainNoTrailSlashToRelativeNoLeadSlash() {
        s = "http://www.sample.com";
        t = "http://www.sample.com/xyz.html";
        assertEquals(t, HttpURL.toAbsolute(s, "xyz.html"));
    }

    @Test
    void testHttpProtocolNoPort() {
        s = "http://www.example.com/blah";
        t = "http://www.example.com/blah";
        assertEquals(t, new HttpURL(s).toString());
    }
    @Test
    void testHttpProtocolDefaultPort() {
        s = "http://www.example.com:80/blah";
        t = "http://www.example.com/blah";
        assertEquals(t, new HttpURL(s).toString());
    }
    @Test
    void testHttpProtocolNonDefaultPort() {
        s = "http://www.example.com:81/blah";
        t = "http://www.example.com:81/blah";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testHttpsProtocolNoPort() {
        s = "https://www.example.com/blah";
        t = "https://www.example.com/blah";
        assertEquals(t, new HttpURL(s).toString());
    }
    @Test
    void testHttpsProtocolDefaultPort() {
        s = "https://www.example.com:443/blah";
        t = "https://www.example.com/blah";
        assertEquals(t, new HttpURL(s).toString());
    }
    @Test
    void testHttpsProtocolNonDefaultPort() {
        s = "https://www.example.com:444/blah";
        t = "https://www.example.com:444/blah";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testNonHttpProtocolNoPort() {
        s = "ftp://ftp.example.com/dir";
        t = "ftp://ftp.example.com/dir";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testNonHttpProtocolWithPort() {
        s = "ftp://ftp.example.com:20/dir";
        t = "ftp://ftp.example.com:20/dir";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testInvalidURL() {
        s = "http://www.example.com/\"path\"";
        t = "http://www.example.com/%22path%22";
        assertEquals(t, new HttpURL(s).toString());
    }

    @Test
    void testURLWithLeadingTrailingSpaces() {
        s = "  http://www.example.com/path  ";
        t = "http://www.example.com/path";
        assertEquals(t, new HttpURL(s).toString());
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
}