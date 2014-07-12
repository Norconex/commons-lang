/**
 * 
 */
package com.norconex.commons.lang.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.unit.DataUnit;

/**
 * @author Pascal Essiembre
 *
 */
public class CachingInputStreamTest {

    @Test
    public void testContentMatchMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());
        InputStream cache = new ReusableInputStream(
                is, (int) DataUnit.B.toBytes(100));
        try {
            // first time should cache
            System.out.println("Fist time.");
            Assert.assertEquals(content, readCacheToString(cache));
            // second time should read from cache
            System.out.println("Second time.");
            Assert.assertEquals(content, readCacheToString(cache));
        }  finally {
            IOUtils.closeQuietly(cache);
        }
    }

    @Test
    public void testContentMatchFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());
        InputStream cache = new ReusableInputStream(
                is, (int) DataUnit.B.toBytes(10));
        try {
            // first time should cache
            System.out.println("Fist time.");
            Assert.assertEquals(content, readCacheToString(cache));
            // second time should read from cache
            System.out.println("Second time.");
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
