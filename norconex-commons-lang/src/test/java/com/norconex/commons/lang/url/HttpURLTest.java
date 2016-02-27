/* Copyright 2015 Norconex Inc.
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

import static org.junit.Assert.assertEquals;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HttpURLTest {

    private final String absURL = "https://www.example.com/a/b/c.html?blah";
    
    private String s;
    private String t;
    
    
    @Before
    public void before() {
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
        logger.setAdditivity(false);
        logger.addAppender(new ConsoleAppender(
                new PatternLayout("%-5p [%C{1}] %m%n"), 
                ConsoleAppender.SYSTEM_OUT));
    }
    
    @After
    public void tearDown() throws Exception {
        s = null;
        t = null;
    }

    @Test
    public void testToAbsoluteRelativeToProtocol() {
        s = "//www.relative.com/e/f.html";
        t = "https://www.relative.com/e/f.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    public void testToAbsoluteRelativeToDomainName() {
        s = "/e/f.html";
        t = "https://www.example.com/e/f.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    public void testToAbsoluteRelativeToFullPageURL() {
        s = "?name=john";
        t = "https://www.example.com/a/b/c.html?name=john";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    public void testToAbsoluteRelativeToLastDirectory() {
        s = "g.html";
        t = "https://www.example.com/a/b/g.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    @Test
    public void testToAbsoluteAbsoluteURL() {
        s = "http://www.sample.com/xyz.html";
        t = "http://www.sample.com/xyz.html";
        assertEquals(t, HttpURL.toAbsolute(absURL, s));
    }
    //Test for issue https://github.com/Norconex/collector-http/issues/225
    @Test
    public void testFromDomainNoTrailSlashToRelativeNoLeadSlash() {
        s = "http://www.sample.com";
        t = "http://www.sample.com/xyz.html";
        assertEquals(t, HttpURL.toAbsolute(s, "xyz.html"));
    }
}
