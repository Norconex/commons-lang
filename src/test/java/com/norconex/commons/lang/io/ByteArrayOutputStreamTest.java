/* Copyright 2015-2019 Norconex Inc.
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
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Pascal Essiembre
 */
public class ByteArrayOutputStreamTest {

    @Test
    public void testByteArrayOutputStream() throws IOException {
        String val1 = "0123456789";
        String val2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        byte[] b = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(5);
        String enc = StandardCharsets.US_ASCII.toString();

        // test nothing written
        b = new byte[5];
        Assertions.assertEquals(-1, out.getByte(5), "no-write-yet-getByte");
        Assertions.assertEquals(
                -1, out.getBytes(b, 0), "no-write-yet-getBytes");

        // test getByte()
        out.write(val1.getBytes(enc));
        Assertions.assertEquals('0', (char) out.getByte(0), "getByte-0");
        Assertions.assertEquals('5', (char) out.getByte(5), "getByte-5");
        Assertions.assertEquals('9', (char) out.getByte(9), "getByte-9");
        Assertions.assertEquals(-1, out.getByte(10), "getByte-10");
        Assertions.assertEquals(10, out.size(), "val1-count");

        // test getBytes()
        b = new byte[6];
        out.getBytes(b, 0);
        Assertions.assertEquals(
                "012345", new String(b, enc), "getBytes-012345");

        b = new byte[3];
        out.getBytes(b, 6);
        Assertions.assertEquals("678", new String(b, enc), "getBytes-678");

        b = new byte[3];
        out.getBytes(b, 6);
        Assertions.assertEquals("678", new String(b, enc), "getBytes-678");

        b = new byte[12];
        int read = out.getBytes(b, 8);
        Assertions.assertEquals(
                "89", new String(b, 0, read, enc), "getBytes-89");
        out.write(val2.getBytes(enc));
        out.getBytes(b, 8);
        Assertions.assertEquals(
                "89ABCDEFGHIJ", new String(b, enc), "getBytes-89ABCDEFGHIJ");

        // toByteArray()
        Assertions.assertEquals(val1 + val2,
                new String(out.toByteArray(), enc), "toByteArray");

        out.close();
    }

}
