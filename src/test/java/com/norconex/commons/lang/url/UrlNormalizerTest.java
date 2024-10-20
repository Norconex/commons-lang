/* Copyright 2010-2019 Norconex Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    private String s;
    private String t;

    @AfterEach
    void tearDown() throws Exception {
        s = null;
        t = null;
    }

    @Test
    void testAllAtOnce() {
        s = """
            https://www.Example.org/0/../1/././%7ea_b:c\\d_|e~f!g \
            h/./^i^J[k]//l./m/n/o/../../p/q/r?cc=&dd=ee&bb=aa\
            #fragment""";
        t = "http://example.org/1/~a_b:c%5Cd_%7Ce~f!g%20h/%5Ei%5EJ%5Bk%5D/l./"
                + "m/p/q/r/?bb=aa&dd=ee";
        //System.out.println("original  : " + s);

        var n = new UrlNormalizer(s)
                .addDirectoryTrailingSlash()
                .addWWW()
                .removeFragment()
                .decodeUnreservedCharacters()
                .encodeNonURICharacters()
                .lowerCaseSchemeHost()
                .removeDefaultPort()
                .removeDotSegments()
                .removeDuplicateSlashes()
                .removeEmptyParameters()
                .removeSessionIds()
                .removeTrailingQuestionMark()
                .removeWWW()
                .sortQueryParameters()
                .unsecureScheme()
                .upperCaseEscapeSequence();
        //          System.out.println("toString(): " + n.toString());
        //          System.out.println("toURL()   : " + n.toURL());
        //          System.out.println("toURI()   : " + n.toURI());
        assertEquals(t, n.toString());
        assertEquals(t, n.toURL().toString());
        assertEquals(t, n.toURI().toString());
    }

    @Test
    void testAddDomainTrailingSlash() {
        s = "http://www.example.com";
        t = "http://www.example.com/";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());

        s = "http://www.example.com/";
        t = "http://www.example.com/";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());

        s = "http://www.example.com/blah";
        t = "http://www.example.com/blah";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());

        s = "http://www.example.com/blah/path";
        t = "http://www.example.com/blah/path";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());

        s = "http://www.example.com?param1=value1&param2=value2";
        t = "http://www.example.com/?param1=value1&param2=value2";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());

        s = "http://www.example.com/?param1=value1&param2=value2";
        t = "http://www.example.com/?param1=value1&param2=value2";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());

        s = "http://www.example.com#hash";
        t = "http://www.example.com/#hash";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());

        s = "http://www.example.com/#hash";
        t = "http://www.example.com/#hash";
        assertEquals(t, n(s).addDomainTrailingSlash().toString());
    }

    @Test
    void testEncodeUTF8Characters() {
        s = "http://www.example.com/élève?série=0é0è";
        t = "http://www.example.com/%C3%A9l%C3%A8ve?s%C3%A9rie=0%C3%A90%C3%A8";
        assertEquals(t, n(s).encodeNonURICharacters().toString());
    }

    @Test
    void testEncodeNonURICharacters() {
        s = "http://www.example.com/^a [b]/c?d e=";
        t = "http://www.example.com/%5Ea%20%5Bb%5D/c?d+e=";
        assertEquals(t, n(s).encodeNonURICharacters().toString());

        //Test for https://github.com/Norconex/collector-http/issues/294
        //Was failing when HTTP was uppercase
        s = "HTTP://www.Example.com/";
        t = "HTTP://www.Example.com/";
        assertEquals(t, n(s).encodeNonURICharacters().toString());
    }

    @Test
    void testEncodeSpaces() {
        s = "http://www.example.com/a b c?d e=f g";
        t = "http://www.example.com/a%20b%20c?d+e=f+g";
        assertEquals(t, n(s).encodeSpaces().toString());
    }

    @Test
    void testLowerCase() {

        // All
        s = "HTTP://www.Example.com/Path/Query?Param1=AAA&amp;Param2=BBB";
        t = "http://www.example.com/path/query?param1=aaa&amp;param2=bbb";
        assertEquals(t, n(s).lowerCase().toString());

        // SchemeHost
        s = "HTTP://www.Example.com/Hello.html";
        t = "http://www.example.com/Hello.html";
        assertEquals(t, n(s).lowerCaseSchemeHost().toString());

        // Path
        s = "HTTP://www.Example.com/Path/Query?Param1=AAA&amp;Param2=BBB";
        t = "HTTP://www.Example.com/path/query?Param1=AAA&amp;Param2=BBB";
        assertEquals(t, n(s).lowerCasePath().toString());

        // Query String
        s = "HTTP://www.Example.com/Path/Query?Param1=AAA&amp;Param2=BBB";
        t = "HTTP://www.Example.com/Path/Query?param1=aaa&amp;param2=bbb";
        assertEquals(t, n(s).lowerCaseQuery().toString());
        s = "HTTP://www.Example.com/Path/Query?Param1&amp;Param2=BBB#frag";
        t = "HTTP://www.Example.com/Path/Query?param1&amp;param2=bbb#frag";
        assertEquals(t, n(s).lowerCaseQuery().toString());
        s = "HTTP://www.Example.com/Path/Query?Param1=#frag";
        t = "HTTP://www.Example.com/Path/Query?param1=#frag";
        assertEquals(t, n(s).lowerCaseQuery().toString());
        s = "HTTP://www.Example.com/Path/Query/Param1&amp;Param2=BBB#frag";
        t = "HTTP://www.Example.com/Path/Query/Param1&amp;Param2=BBB#frag";
        assertEquals(t, n(s).lowerCaseQuery().toString());

        // Query parameter names
        s = "HTTP://www.Example.com/Path/Query?Param1=AAA&amp;Param2=BBB";
        t = "HTTP://www.Example.com/Path/Query?param1=AAA&amp;param2=BBB";
        assertEquals(t, n(s).lowerCaseQueryParameterNames().toString());
        s = "HTTP://www.Example.com/Path/Query?Param1&amp;Param2=BBB#frag";
        t = "HTTP://www.Example.com/Path/Query?param1&amp;param2=BBB#frag";
        assertEquals(t, n(s).lowerCaseQueryParameterNames().toString());
        s = "HTTP://www.Example.com/Path/Query?Param1=#frag";
        t = "HTTP://www.Example.com/Path/Query?param1=#frag";
        assertEquals(t, n(s).lowerCaseQueryParameterNames().toString());

        // Query parameter values
        s = "HTTP://www.Example.com/Path/Query?Param1=AAA&amp;Param2=BBB";
        t = "HTTP://www.Example.com/Path/Query?Param1=aaa&amp;Param2=bbb";
        assertEquals(t, n(s).lowerCaseQueryParameterValues().toString());
        s = "HTTP://www.Example.com/Path/Query?Param1&amp;Param2=BBB#frag";
        t = "HTTP://www.Example.com/Path/Query?Param1&amp;Param2=bbb#frag";
        assertEquals(t, n(s).lowerCaseQueryParameterValues().toString());
        s = "HTTP://www.Example.com/Path/Query?=AAA#frag";
        t = "HTTP://www.Example.com/Path/Query?=aaa#frag";
        assertEquals(t, n(s).lowerCaseQueryParameterValues().toString());
    }

    @Test
    void testUpperCaseEscapeSequence() {
        s = "http://www.example.com/a%c2%b1b";
        t = "http://www.example.com/a%C2%B1b";
        assertEquals(t, n(s).upperCaseEscapeSequence().toString());
    }

    @Test
    void testDecodeUnreservedCharacters() {
        // ALPHA (%41-%5A and %61-%7A), DIGIT (%30-%39), hyphen (%2D),
        // period (%2E), underscore (%5F), or tilde (%7E)
        s = """
            http://www.example.com/%41%42%59%5Aalpha\
            %61%62%79%7A/digit%30%31%38%39/%2Dhyphen/period%2E\
            /underscore%5F/%7Etilde/reserved%2F%3A%5B%26""";
        t = "http://www.example.com/ABYZalphaabyz/digit0189"
                + "/-hyphen/period./underscore_/~tilde/reserved%2F%3A%5B%26";
        assertEquals(t, n(s).decodeUnreservedCharacters().toString());
    }

    @Test
    void testRemoveDefaultPort() {
        s = "http://www.example.com:80/bar.html";
        t = "http://www.example.com/bar.html";
        assertEquals(t, n(s).removeDefaultPort().toString());
        s = "https://www.example.com:443/bar.html";
        t = "https://www.example.com/bar.html";
        assertEquals(t, n(s).removeDefaultPort().toString());
        s = "http://www.example.com/bar.html";
        t = "http://www.example.com/bar.html";
        assertEquals(t, n(s).removeDefaultPort().toString());
        s = "http://www.example.com:8080/bar.html";
        t = "http://www.example.com:8080/bar.html";
        assertEquals(t, n(s).removeDefaultPort().toString());
        s = "http://www.example.com:80";
        t = "http://www.example.com";
        assertEquals(t, n(s).removeDefaultPort().toString());
        s = "http://www.example.com";
        t = "http://www.example.com";
        assertEquals(t, n(s).removeDefaultPort().toString());
        s = "http://www.example.com/bar/:80";
        t = "http://www.example.com/bar/:80";
        assertEquals(t, n(s).removeDefaultPort().toString());
    }

    @Test
    void testAddTrailingSlash() {
        s = "http://www.example.com/alice";
        t = "http://www.example.com/alice/";
        assertEquals(t, n(s).addDirectoryTrailingSlash().toString());
        s = "http://www.example.com/alice.html";
        t = "http://www.example.com/alice.html";
        assertEquals(t, n(s).addDirectoryTrailingSlash().toString());
        s = "http://www.example.com";
        t = "http://www.example.com";
        assertEquals(t, n(s).addDirectoryTrailingSlash().toString());
        s = "http://www.example.com/blah/?param=value";
        t = "http://www.example.com/blah/?param=value";
        assertEquals(t, n(s).addDirectoryTrailingSlash().toString());
        s = "http://www.example.com/blah?param=value";
        t = "http://www.example.com/blah/?param=value";
        assertEquals(t, n(s).addDirectoryTrailingSlash().toString());
        // This one is for HTTP Collector GitHub issue #163:
        s = "http://www.example.com/";
        t = "http://www.example.com/";
        assertEquals(t, n(s).addDirectoryTrailingSlash().toString());
    }

    @Test
    void testRemoveTrailingSlash() {
        s = "http://www.example.com/alice/";
        t = "http://www.example.com/alice";
        assertEquals(t, n(s).removeTrailingSlash().toString());
        s = "http://www.example.com/alice.html";
        t = "http://www.example.com/alice.html";
        assertEquals(t, n(s).removeTrailingSlash().toString());
        s = "http://www.example.com";
        t = "http://www.example.com";
        assertEquals(t, n(s).removeTrailingSlash().toString());
        s = "http://www.example.com/blah/?param=value";
        t = "http://www.example.com/blah?param=value";
        assertEquals(t, n(s).removeTrailingSlash().toString());
        s = "http://www.example.com/blah?param=value";
        t = "http://www.example.com/blah?param=value";
        assertEquals(t, n(s).removeTrailingSlash().toString());
        s = "http://www.example.com/blah/#value";
        t = "http://www.example.com/blah#value";
        assertEquals(t, n(s).removeTrailingSlash().toString());
        s = "http://www.example.com/blah#value";
        t = "http://www.example.com/blah#value";
        assertEquals(t, n(s).removeTrailingSlash().toString());
        s = "http://www.example.com/";
        t = "http://www.example.com";
        assertEquals(t, n(s).removeTrailingSlash().toString());
    }

    @Test
    void testRemoveTrailingHash() {
        s = "http://www.example.com/blah#";
        t = "http://www.example.com/blah";
        assertEquals(t, n(s).removeTrailingHash().toString());
        s = "http://www.example.com/blah#whatever";
        t = "http://www.example.com/blah#whatever";
        assertEquals(t, n(s).removeTrailingHash().toString());
        s = "http://www.example.com";
        t = "http://www.example.com";
        assertEquals(t, n(s).removeTrailingHash().toString());
    }

    @Test
    void testRemoveDotSegments() {
        s = "http://www.example.com/../a/b/../c/./d.html";
        t = "http://www.example.com/a/c/d.html";
        assertEquals(t, n(s).removeDotSegments().toString());
        s = "http://www.example.com/a/../b/../c/./d.html";
        t = "http://www.example.com/c/d.html";
        assertEquals(t, n(s).removeDotSegments().toString());
        // From ticket #173:
        s = "http://www.example.com/a/../../../../b/c/d/e/f.jpg";
        t = "http://www.example.com/b/c/d/e/f.jpg";
        assertEquals(t, n(s).removeDotSegments().toString());

        //--- Tests from http://tools.ietf.org/html/rfc3986#section-5.4 ---
        var urlRoot = "http://a.com";
        Map<String, String> m = new HashMap<>();

        // 5.4.1 Normal Examples
        m.put("/b/c/.", "/b/c/");
        m.put("/b/c/./", "/b/c/");
        m.put("/b/c/..", "/b/");
        m.put("/b/c/../", "/b/");
        m.put("/b/c/../g", "/b/g");
        m.put("/b/c/../..", "/");
        m.put("/b/c/../../", "/");
        m.put("/b/c/../../g", "/g");

        // 5.4.2. Abnormal Examples
        m.put("/b/c/../../../g", "/g");
        m.put("/b/c/../../../../g", "/g");

        m.put("/./g", "/g");
        m.put("/../g", "/g");
        m.put("/b/c/g.", "/b/c/g.");
        m.put("/b/c/.g", "/b/c/.g");
        m.put("/b/c/g..", "/b/c/g..");
        m.put("/b/c/..g", "/b/c/..g");

        m.put("/b/c/./../g", "/b/g");
        m.put("/b/c/./g/.", "/b/c/g/");
        m.put("/b/c/g/./h", "/b/c/g/h");
        m.put("/b/c/g/../h", "/b/c/h");
        m.put("/b/c/g;x=1/./y", "/b/c/g;x=1/y");
        m.put("/b/c/g;x=1/../y", "/b/c/y");

        m.put("/b/c/g?y/./x", "/b/c/g?y/./x");
        m.put("/b/c/g?y/../x", "/b/c/g?y/../x");
        m.put("/b/c/g#s/./x", "/b/c/g#s/./x");
        m.put("/b/c/g#s/../x", "/b/c/g#s/../x");

        for (Entry<String, String> e : m.entrySet()) {
            s = urlRoot + e.getKey();
            t = urlRoot + e.getValue();
            assertEquals(t, n(s).removeDotSegments().toString());
        }
    }

    @Test
    void testRemoveDirectoryIndex() {
        s = "http://www.example.com/index.html";
        t = "http://www.example.com/";
        assertEquals(t, n(s).removeDirectoryIndex().toString());
        s = "http://www.example.com/index.html/a";
        t = "http://www.example.com/index.html/a";
        assertEquals(t, n(s).removeDirectoryIndex().toString());
        s = "http://www.example.com/a/Default.asp";
        t = "http://www.example.com/a/";
        assertEquals(t, n(s).removeDirectoryIndex().toString());
        s = "http://www.example.com/a/index.php?a=b&c=d";
        t = "http://www.example.com/a/?a=b&c=d";
        assertEquals(t, n(s).removeDirectoryIndex().toString());
        s = "http://www.example.com/a/z.php?a=b&c=d/index.htm";
        t = "http://www.example.com/a/z.php?a=b&c=d/index.htm";
        assertEquals(t, n(s).removeDirectoryIndex().toString());
        s = "http://www.example.com/index,html";
        t = "http://www.example.com/index,html";
        assertEquals(t, n(s).removeDirectoryIndex().toString());
    }

    @Test
    void testRemoveFragment() {
        s = "http://www.example.com/bar.html#section1";
        t = "http://www.example.com/bar.html";
        assertEquals(t, n(s).removeFragment().toString());
        s = "http://www.example.com/bar.html#";
        t = "http://www.example.com/bar.html";
        assertEquals(t, n(s).removeFragment().toString());
    }

    @Test
    void testRemoveTrailingFragment() {
        s = "http://www.example.com/abc.html#section1";
        t = "http://www.example.com/abc.html";
        assertEquals(t, n(s).removeTrailingFragment().toString());
        s = "http://www.example.com/abc#/def/ghi";
        t = "http://www.example.com/abc#/def/ghi";
        assertEquals(t, n(s).removeTrailingFragment().toString());
        s = "http://www.example.com/abc#def/ghi#klm";
        t = "http://www.example.com/abc#def/ghi";
        assertEquals(t, n(s).removeTrailingFragment().toString());
    }

    @Test
    void testRemoveQueryString() {
        s = "http://www.example.com/q?param1=AAA&param2=BBB";
        t = "http://www.example.com/q";
        assertEquals(t, n(s).removeQueryString().toString());
        s = "http://www.example.com/q?param1=AAA&param2=BBB#frag";
        t = "http://www.example.com/q#frag";
        assertEquals(t, n(s).removeQueryString().toString());
        s = "http://www.example.com/q?";
        t = "http://www.example.com/q";
        assertEquals(t, n(s).removeQueryString().toString());
        s = "http://www.example.com/q?param";
        t = "http://www.example.com/q";
        assertEquals(t, n(s).removeQueryString().toString());
    }

    @Test
    @Disabled("This test may not have proper network condition to execute.")
    void testReplaceIPWithDomainName() {
        s = "http://208.80.154.224/wiki/Main_Page";
        t = null;
        Assertions.assertTrue(
                n(s).replaceIPWithDomainName().toString()
                        .contains("wikimedia"));
        s = "http://wikipedia.org/wiki/Main_Page";
        t = "http://wikipedia.org/wiki/Main_Page";
        assertEquals(t, n(s).replaceIPWithDomainName().toString());
        s = "http://200.200.200.200/nohost.html";
        t = "http://200.200.200.200/nohost.html";
        assertEquals(t, n(s).replaceIPWithDomainName().toString());
    }

    @Test
    void testUnsecureScheme() {
        s = "https://www.example.com/secure.html";
        t = "http://www.example.com/secure.html";
        assertEquals(t, n(s).unsecureScheme().toString());
        s = "HTtPS://www.example.com/secure.html";
        t = "HTtP://www.example.com/secure.html";
        assertEquals(t, n(s).unsecureScheme().toString());
        s = "HTTP://www.example.com/nonsecure.html";
        t = "HTTP://www.example.com/nonsecure.html";
        assertEquals(t, n(s).unsecureScheme().toString());
    }

    @Test
    void testSecureScheme() {
        s = "https://www.example.com/secure.html";
        t = "https://www.example.com/secure.html";
        assertEquals(t, n(s).secureScheme().toString());
        s = "HTtP://www.example.com/secure.html";
        t = "HTtPs://www.example.com/secure.html";
        assertEquals(t, n(s).secureScheme().toString());
        s = "HTTP://www.example.com/nonsecure.html";
        t = "HTTPs://www.example.com/nonsecure.html";
        assertEquals(t, n(s).secureScheme().toString());
    }

    @Test
    void testRemoveDuplicateSlashes() {
        s = "http://www.example.com/a//b///c////d/////e.html";
        t = "http://www.example.com/a/b/c/d/e.html";
        assertEquals(t, n(s).removeDuplicateSlashes().toString());
        s = "http://www.example.com/a//b//c.html?path=/d//e///f";
        t = "http://www.example.com/a/b/c.html?path=/d//e///f";
        assertEquals(t, n(s).removeDuplicateSlashes().toString());
        // This one is for HTTP Collector GitHub issue #163:
        s = "http://www.example.com//";
        t = "http://www.example.com/";
        assertEquals(t, n(s).removeDuplicateSlashes().toString());
    }

    @Test
    void testRemoveWWW() {
        s = "http://www.example.com/foo.html";
        t = "http://example.com/foo.html";
        assertEquals(t, n(s).removeWWW().toString());
        s = "http://wwwexample.com/foo.html";
        t = "http://wwwexample.com/foo.html";
        assertEquals(t, n(s).removeWWW().toString());
    }

    @Test
    void testAddWWW() {
        s = "http://example.com/foo.html";
        t = "http://www.example.com/foo.html";
        assertEquals(t, n(s).addWWW().toString());
        s = "http://wwwexample.com/foo.html";
        t = "http://www.wwwexample.com/foo.html";
        assertEquals(t, n(s).addWWW().toString());
        s = "http://www.example.com/foo.html";
        t = "http://www.example.com/foo.html";
        assertEquals(t, n(s).addWWW().toString());
    }

    @Test
    void testSortQueryParameters() {
        // test with fragment
        s = "http://example.com?z=1&a=1#frag";
        t = "http://example.com?a=1&z=1#frag";
        assertEquals(t, n(s).sortQueryParameters().toString());
        // test duplicate params
        s = "http://example.com?z=1&a=1&a=1";
        t = "http://example.com?a=1&a=1&z=1";
        assertEquals(t, n(s).sortQueryParameters().toString());
        s = "http://www.example.com/display?lang=en&article=fred";
        t = "http://www.example.com/display?article=fred&lang=en";
        assertEquals(t, n(s).sortQueryParameters().toString());
        s = "http://www.example.com/?z=bb&y=cc&z=aa";
        t = "http://www.example.com/?y=cc&z=aa&z=bb";
        assertEquals(t, n(s).sortQueryParameters().toString());
        // Sorting should not change encoding
        s = "http://www.example.com/spa ce?z=b%2Fb&y=c c&z=a/a";
        t = "http://www.example.com/spa ce?y=c c&z=a/a&z=b%2Fb";
        assertEquals(t, n(s).sortQueryParameters().toString());
        s = "http://www.example.com/?z&y=c c&y=c c&a&d=&";
        t = "http://www.example.com/?a&d=&y=c c&y=c c&z";
        assertEquals(t, n(s).sortQueryParameters().toString());
    }

    @Test
    void testRemoveEmptyParameters() {
        s = "http://www.example.com/display?a=b&a=&c=d&e=&f=g&h&=i";
        t = "http://www.example.com/display?a=b&c=d&f=g";
        assertEquals(t, n(s).removeEmptyParameters().toString());
    }

    @Test
    void testRemoveTrailingQuestionMark() {
        s = "http://www.example.com/remove?";
        t = "http://www.example.com/remove";
        assertEquals(t, n(s).removeTrailingQuestionMark().toString());
        s = "http://www.example.com/keep?a=b";
        t = "http://www.example.com/keep?a=b";
        assertEquals(t, n(s).removeTrailingQuestionMark().toString());
        s = "http://www.example.com/keep?a=b?";
        t = "http://www.example.com/keep?a=b?";
        assertEquals(t, n(s).removeTrailingQuestionMark().toString());
    }

    @Test
    void testRemoveSessionIds() {
        //PHP
        s = "http://1.eg.com/app?a=b&PHPSESSID=f9f2770d591366bc&aa=bbb&c=d";
        t = "http://1.eg.com/app?a=b&aa=bbb&c=d";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://2.eg.com/app?PHPSESSID=f9f2770d591366bc&aaa=bbb&c=d";
        t = "http://2.eg.com/app?aaa=bbb&c=d";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://3.eg.com/app?a=b&PHPSESSID=f9f2770d591366bc";
        t = "http://3.eg.com/app?a=b";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://4.eg.com/app?a=b&c=d&NOPHPSESSID=f9f2770d591366bc&a=b";
        t = "http://4.eg.com/app?a=b&c=d&NOPHPSESSID=f9f2770d591366bc&a=b";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://5.eg.com/app?PHPSESSID=f9f2770d591366bc";
        t = "http://5.eg.com/app";
        assertEquals(t, n(s).removeSessionIds().toString());

        //Java EE
        s = "http://6.eg.com/app;jsessionid=1E6FEC03D29ED?a=b&c=d";
        t = "http://6.eg.com/app?a=b&c=d";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://7.eg.com/app;jsessionid=1E6FEC03D29ED";
        t = "http://7.eg.com/app";
        assertEquals(t, n(s).removeSessionIds().toString());

        //https://github.com/Norconex/collector-http/issues/311
        s = "http://myurl.com;jsessionid="
                + "E460A97852FA769CD9AD387095C680A9.tpdila09v_2?p=v";
        t = "http://myurl.com?p=v";
        assertEquals(t, n(s).removeSessionIds().toString());

        //ASP
        s = "http://8.eg.com/app?y=z&ASPSESSIONIDSCQCTTCT=CMHFECGKC&a=b&c=d";
        t = "http://8.eg.com/app?y=z&a=b&c=d";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://9.eg.com/app?ASPSESSIONIDSCQCTTCT=CMHFECGKC&a=b&c=d";
        t = "http://9.eg.com/app?a=b&c=d";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://10.eg.com/app?a=b&ASPSESSIONIDSCQCTTCT=CMHFECGKC";
        t = "http://10.eg.com/app?a=b";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://11.eg.com/app?a=b&NOASPSESSIONIDSCQCTTCT=CMHFECGKC&c=d";
        t = "http://11.eg.com/app?a=b&NOASPSESSIONIDSCQCTTCT=CMHFECGKC&c=d";
        assertEquals(t, n(s).removeSessionIds().toString());
        s = "http://12.eg.com/app?ASPSESSIONIDSCQCTTCT=CMHFECGKC";
        t = "http://12.eg.com/app";
        assertEquals(t, n(s).removeSessionIds().toString());
    }
    //  /myservlet;jsessionid=1E6FEC0D14D044541DD84D2D013D29ED?_option=XX[b]'[/b];[
    //  PHPSESSID=f9f2770d591366bc

    // Test for supporting file:// scheme, from here:
    // https://github.com/Norconex/commons-lang/issues/11
    @Test
    void testFileScheme() {

        // Encode non-URI characters
        s = "file:///etc/some dir/my file.txt";
        t = "file:///etc/some%20dir/my%20file.txt";
        assertEquals(t, n(s).encodeNonURICharacters().toString());

        s = "file://./dir/another-dir/path";
        t = "file://./dir/another-dir/path";
        assertEquals(t, n(s).encodeNonURICharacters().toString());

        s = "file://localhost/c:/WINDOWS/éà.txt";
        t = "file://localhost/c:/WINDOWS/%C3%A9%C3%A0.txt";
        assertEquals(t, n(s).encodeNonURICharacters().toString());

        s = "file:///c:/WINDOWS/file.txt";
        t = "file:///c:/WINDOWS/file.txt";
        assertEquals(t, n(s).encodeNonURICharacters().toString());
    }

    private UrlNormalizer n(String url) {
        return new UrlNormalizer(url);
    }
}
