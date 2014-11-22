/* Copyright 2014 Norconex Inc.
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
package com.norconex.commons.lang.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pascal Essiembre
 */
public class CachedInputStreamTest {

    @Before
    public void before() {
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
        logger.setAdditivity(false);
        logger.addAppender(new ConsoleAppender(
                new PatternLayout("%-5p [%C{1}] %m%n"), 
                ConsoleAppender.SYSTEM_OUT));
    }
    
    @Test
    public void testContentMatchMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());
        
        CachedStreamFactory factory = new CachedStreamFactory(200, 100);
        CachedInputStream cache = factory.newInputStream(is);
        try {
            // first time should cache
            Assert.assertEquals(content, readCacheToString(cache));
            cache.rewind();
            // second time should read from cache
            Assert.assertEquals(content, readCacheToString(cache));
        }  finally {
            IOUtils.closeQuietly(cache);
        }
    }

    @Test
    public void testContentMatchPoolMaxFileCache() throws IOException {
        CachedStreamFactory factory = new CachedStreamFactory(
                200 * 1024, 150 * 1024);
        CachedInputStream cache1 = null;
        CachedInputStream cache2 = null;
        try {
            // first time loads 6 bytes
            cache1 = factory.newInputStream(new NullInputStream(
                    140 * 1024));
            readCacheToString(cache1);
//            Assert.assertEquals(content, readCacheToString(cache1));

            
            // first time loads 6 bytes, totaling 12, forcing file cache
            cache2 = factory.newInputStream(new NullInputStream(
                    140 * 1024));
            readCacheToString(cache2);
        }  finally {
            IOUtils.closeQuietly(cache1);
            IOUtils.closeQuietly(cache2);
        }
    }

    @Test
    public void testContentMatchInstanceFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());
        CachedStreamFactory factory = new CachedStreamFactory(200, 10);
        CachedInputStream cache = factory.newInputStream(is);
        try {
            // first time should cache
            Assert.assertEquals(content, readCacheToString(cache));
            cache.rewind();
            // second time should read from cache
            Assert.assertEquals(content, readCacheToString(cache));
        }  finally {
            IOUtils.closeQuietly(cache);
        }
    }

    
    private String readCacheToString(InputStream is) throws IOException {
        long i;
        StringBuilder b = new StringBuilder();
        while ((i=is.read()) != -1) {
            b.append((char) i);
        }
        return b.toString();
    }
}
