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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pascal Essiembre
 */
public class CachedOutputStreamTest {

    @Before
    public void before() {
        Logger logger = LogManager.getRootLogger();
        logger.addAppender(new ConsoleAppender(
                new PatternLayout("%-5p [%C{1}] %m%n"), 
                ConsoleAppender.SYSTEM_OUT));
        logger.setAdditivity(false);
    }
    
    @Test
    public void testContentMatchMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        CachedStreamFactory factory = new CachedStreamFactory(200, 100);
        CachedOutputStream cache = factory.newOuputStream();
        InputStream is = null;
        
        try {
            cache.write(content.getBytes());
            is = cache.getInputStream();
            Assert.assertEquals(content, readCacheToString(is));
        }  finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(cache);
        }
    }

    @Test
    public void testContentMatchFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        CachedStreamFactory factory = new CachedStreamFactory(200, 10);
        CachedOutputStream cache = factory.newOuputStream();

        InputStream is = null;
        try {
            cache.write(content.getBytes());
            is = cache.getInputStream();
            Assert.assertEquals(content, readCacheToString(is));
        }  finally {
            IOUtils.closeQuietly(is);
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
