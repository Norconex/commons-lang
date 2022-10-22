/* Copyright 2015-2022 Norconex Inc.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Pascal Essiembre
 */
class ByteArrayOutputStreamTest {

    @Test
    void testByteArrayOutputStream() throws IOException {
        String val1 = "0123456789";
        String val2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        byte[] b = new byte[5];
        ByteArrayOutputStream out = new ByteArrayOutputStream(5);
        String enc = StandardCharsets.US_ASCII.toString();

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

    @Test
    void testNullAndErrors() throws IOException {
        assertThrows(IllegalArgumentException.class,
                () -> new ByteArrayOutputStream(-123));
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> new ByteArrayOutputStream().getBytes(null, 0));
        assertThrows(IndexOutOfBoundsException.class, //NOSONAR
                () -> new ByteArrayOutputStream().write(new byte [] {}, -1, 0));
        assertThrows(NullPointerException.class, //NOSONAR
                () -> new ByteArrayOutputStream().write(null, 0, 0));
    }

    @Test
    void testWrite() throws IOException {
        String val1 = "012345678";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(10)) {
            out.write(val1.getBytes(UTF_8));
            out.write('9');
            out.write('A');
            assertThat(out.toString()).hasToString("0123456789A");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(10);
                ByteArrayInputStream in =
                        new ByteArrayInputStream(val1.getBytes(UTF_8))) {
            assertThat(out.write(in)).isEqualTo(9);
            out.reset();
            assertThat(out.write(in)).isEqualTo(0);
        }

        String val2 = "012345678";
        try (ByteArrayOutputStream out1 = new ByteArrayOutputStream(10);
                ByteArrayOutputStream out2 = new ByteArrayOutputStream(10)) {
            out1.write(val2.getBytes(UTF_8));
            out1.writeTo(out2);
            assertThat(out2.toString()).hasToString("012345678");
        }
    }

    @Test
    void testToX() throws IOException {
        String val = "0123456789";
        try (ByteArrayInputStream in =
                        new ByteArrayInputStream(val.getBytes(UTF_8))) {
            InputStream is = ByteArrayOutputStream.toBufferedInputStream(in);
            assertThat(IOUtils.toString(is, UTF_8)).hasToString(val);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(10)) {
            assertThat(out.toByteArray()).isEmpty();
            out.write(val.getBytes(UTF_8));
            assertThat(out.toString(UTF_8)).isEqualTo(val);
            assertThat(out.toString(UTF_8.toString())).isEqualTo(val);
        }
    }
}
