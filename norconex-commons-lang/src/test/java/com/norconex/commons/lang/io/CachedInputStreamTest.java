/* Copyright 2014 Norconex Inc.
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
