/* Copyright 2015-2017 Norconex Inc.
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

import java.io.IOException;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pascal Essiembre
 */
public class ByteArrayOutputStreamTest {

//    @Before
//    public void before() {
//        Logger logger = Logger.getRootLogger();
//        logger.setLevel(Level.DEBUG);
//        logger.setAdditivity(false);
//        logger.addAppender(new ConsoleAppender(
//                new PatternLayout("%-5p [%C{1}] %m%n"), 
//                ConsoleAppender.SYSTEM_OUT));
//    }

    @Test
    public void testByteArrayOutputStream() throws IOException {
        String val1 = "0123456789";
        String val2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        byte[] b = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(5);
        String enc = CharEncoding.US_ASCII;
        
        // test nothing written
        b = new byte[5];
        Assert.assertEquals("no-write-yet-getByte", -1, out.getByte(5));
        Assert.assertEquals("no-write-yet-getBytes", -1, out.getBytes(b, 0));

        // test getByte()
        out.write(val1.getBytes(enc));
        Assert.assertEquals("getByte-0", '0', (char) out.getByte(0));
        Assert.assertEquals("getByte-5", '5', (char) out.getByte(5));
        Assert.assertEquals("getByte-9", '9', (char) out.getByte(9));
        Assert.assertEquals("getByte-10", -1, out.getByte(10));
        Assert.assertEquals("val1-count", 10, out.size());

        // test getBytes()
        b = new byte[6];
        out.getBytes(b, 0);
        Assert.assertEquals("getBytes-012345", "012345", new String(b, enc));

        b = new byte[3];
        out.getBytes(b, 6);
        Assert.assertEquals("getBytes-678", "678", new String(b, enc));

        b = new byte[3];
        out.getBytes(b, 6);
        Assert.assertEquals("getBytes-678", "678", new String(b, enc));

        b = new byte[12];
        int read = out.getBytes(b, 8);
        Assert.assertEquals("getBytes-89", "89", new String(b, 0, read, enc));
        out.write(val2.getBytes(enc));
        out.getBytes(b, 8);
        Assert.assertEquals("getBytes-89ABCDEFGHIJ", "89ABCDEFGHIJ",
                new String(b, enc));

        // toByteArray()
        Assert.assertEquals("toByteArray", val1 + val2,
                new String(out.toByteArray(), enc));
        
        out.close();
    }

}
