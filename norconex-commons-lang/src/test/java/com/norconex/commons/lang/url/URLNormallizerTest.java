package com.norconex.commons.lang.url;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class URLNormallizerTest {

    private String s;
    private String t;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        s = null;
        t = null;
    }

    @Test
    public void testLowerCaseSchemeHost() {
        s = "HTTP://www.Example.com/Hello.html";
        t = "http://www.example.com/Hello.html";
        assertEquals(t, n(s).lowerCaseSchemeHost().toString());
    }

    @Test
    public void testUpperCaseEscapeSequence() {
        s = "http://www.example.com/a%c2%b1b";
        t = "http://www.example.com/a%C2%B1b";
        assertEquals(t, n(s).upperCaseEscapeSequence().toString());
    }
    
    @Test
    public void testDecodeUnreservedCharacters() {
        // ALPHA (%41�%5A and %61�%7A), DIGIT (%30�%39), hyphen (%2D), 
        // period (%2E), underscore (%5F), or tilde (%7E) 
        s = "http://www.example.com/%41%42%59%5Aalpha"
                + "%61%62%79%7A/digit%30%31%38%39/%2Dhyphen/period%2E"
                + "/underscore%5F/%7Etilde/reserved%2F%3A%5B%26";
        t = "http://www.example.com/ABYZalphaabyz/digit0189"
                + "/-hyphen/period./underscore_/~tilde/reserved%2F%3A%5B%26";
        assertEquals(t, n(s).decodeUnreservedCharacters().toString());
    }

    @Test
    public void testRemoveDefaultPort() {
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
    public void testAddTrailingSlash() {
        s = "http://www.example.com/alice";
        t = "http://www.example.com/alice/";
        assertEquals(t, n(s).addTrailingSlash().toString());
        s = "http://www.example.com/alice.html";
        t = "http://www.example.com/alice.html";
        assertEquals(t, n(s).addTrailingSlash().toString());
        s = "http://www.example.com";
        t = "http://www.example.com";
        assertEquals(t, n(s).addTrailingSlash().toString());
    }
    @Test
    public void testRemoveDotSegments() {
        s = "http://www.example.com/../a/b/../c/./d.html";
        t = "http://www.example.com/../a/c/d.html";
        assertEquals(t, n(s).removeDotSegments().toString());
        s = "http://www.example.com/a/../b/../c/./d.html";
        t = "http://www.example.com/c/d.html";
        assertEquals(t, n(s).removeDotSegments().toString());
    }
    @Test
    public void testRemoveDirectoryIndex() {
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
    public void testRemoveFragment() {
        s = "http://www.example.com/bar.html#section1";
        t = "http://www.example.com/bar.html";
        assertEquals(t, n(s).removeFragment().toString());
        s = "http://www.example.com/bar.html#";
        t = "http://www.example.com/bar.html";
        assertEquals(t, n(s).removeFragment().toString());
    }
    
    @Test
    public void testReplaceIPWithDomainName() {
        s = "http://208.80.152.201/wiki/Main_Page";
        t = null;
        Assert.assertTrue(
               n(s).replaceIPWithDomainName().toString().contains("wikipedia"));
        s = "http://wikipedia.org/wiki/Main_Page";
        t = "http://wikipedia.org/wiki/Main_Page";
        assertEquals(t, n(s).replaceIPWithDomainName().toString());
        s = "http://200.200.200.200/nohost.html";
        t = "http://200.200.200.200/nohost.html";
        assertEquals(t, n(s).replaceIPWithDomainName().toString());
    }

    @Test
    public void testUnsecureScheme() {
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
    public void testSecureScheme() {
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
    public void testRemoveDuplicateSlashes() {
        s = "http://www.example.com/a//b///c////d/////e.html";
        t = "http://www.example.com/a/b/c/d/e.html";
        assertEquals(t, n(s).removeDuplicateSlashes().toString());
        s = "http://www.example.com/a//b//c.html?path=/d//e///f";
        t = "http://www.example.com/a/b/c.html?path=/d//e///f";
        assertEquals(t, n(s).removeDuplicateSlashes().toString());
    }

    @Test
    public void testRemoveWWW() {
        s = "http://www.example.com/foo.html";
        t = "http://example.com/foo.html";
        assertEquals(t, n(s).removeWWW().toString());
        s = "http://wwwexample.com/foo.html";
        t = "http://wwwexample.com/foo.html";
        assertEquals(t, n(s).removeWWW().toString());
    }    
    
    @Test
    public void testAddWWW() {
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
    public void testSortQueryParameters() {
        s = "http://www.example.com/display?lang=en&article=fred";
        t = "http://www.example.com/display?article=fred&lang=en";
        assertEquals(t, n(s).sortQueryParameters().toString());
        s = "http://www.example.com/?z=bb&y=cc&z=aa";
        t = "http://www.example.com/?y=cc&z=bb&z=aa";
        assertEquals(t, n(s).sortQueryParameters().toString());
    }
    
    @Test
    public void testRemoveEmptyParameters() {
        s = "http://www.example.com/display?a=b&a=&c=d&e=&f=g";
        t = "http://www.example.com/display?a=b&c=d&f=g";
        assertEquals(t, n(s).removeEmptyParameters().toString());
    }    
    
    @Test
    public void testRemoveTrailingQuestionMark() {
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
    public void testRemoveSessionIds() {
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
    
//     
    
//    /myservlet;jsessionid=1E6FEC0D14D044541DD84D2D013D29ED?_option=XX[b]'[/b];[    
    //PHPSESSID=f9f2770d591366bc


    
    private URLNormalizer n(String url) {
        return new URLNormalizer(url);
    }


//
//    @Test
//    public void testLoadFromXML() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testSaveToXML() {
//        fail("Not yet implemented");
//    }

}
