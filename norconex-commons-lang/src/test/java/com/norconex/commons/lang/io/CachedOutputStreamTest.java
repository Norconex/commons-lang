/**
 * 
 */
package com.norconex.commons.lang.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.norconex.commons.lang.unit.DataUnit;

/**
 * @author Pascal Essiembre
 *
 */
public class CachedOutputStreamTest {

    @Before
    public void before() {
        Logger.getRootLogger().addAppender(new ConsoleAppender(
                new SimpleLayout(), ConsoleAppender.SYSTEM_OUT));
    }
    
    @Test
    public void testContentMatchMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        CachedOutputStream cache = 
                new CachedOutputStream((int) DataUnit.B.toBytes(100));
        InputStream is = null;
        
        try {
            cache.write(content.getBytes());
            
            // first time should read from cache
            System.out.println("Fist time.");
            is = cache.getInputStream();
            Assert.assertEquals(content, readCacheToString(is));
            IOUtils.closeQuietly(is);
            
            // second time should read from cache again
            System.out.println("Second time.");
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

        CachedOutputStream cache = 
                new CachedOutputStream((int) DataUnit.B.toBytes(10));
        InputStream is = null;
        
        try {
            cache.write(content.getBytes());
            
            // first time should read from cache
            System.out.println("Fist time.");
            is = cache.getInputStream();
            Assert.assertEquals(content, readCacheToString(is));
            IOUtils.closeQuietly(is);
            
            // second time should read from cache again
            System.out.println("Second time.");
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
