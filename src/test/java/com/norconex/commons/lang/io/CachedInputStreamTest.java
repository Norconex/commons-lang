/* Copyright 2014-2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
class CachedInputStreamTest {

    @Test
    void testByteMasking() throws IOException {
        String content = "èéîïâ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8));

        CachedStreamFactory factory = new CachedStreamFactory(2028, 1024);
        CachedInputStream cache = factory.newInputStream(is);
        try {
            // first read
            cache.mark(8);
            for (int i = 0; i < 8; i++) {
                cache.read();
            }
            cache.reset();

            // second read
            while (cache.read() != -1) {}
            cache.rewind();

            // third read and test
            Assertions.assertEquals(content, toString(cache));
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }


    @Test
    void testContentMatchMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());

        CachedStreamFactory factory = new CachedStreamFactory(200, 100);
        CachedInputStream cache = factory.newInputStream(
                new BufferedInputStream(is));
        try {
            // first time should cache
            Assertions.assertEquals(content, toString(cache));
            cache.rewind();
            // second time should read from cache
            Assertions.assertEquals(content, toString(cache));
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }

    @Test
    void testMarking() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());

        CachedStreamFactory factory = new CachedStreamFactory(200, 100);
        CachedInputStream cache = factory.newInputStream(is);
        try {
            String enc = StandardCharsets.US_ASCII.toString();
            byte[] bytes = new byte[5];
            cache.read(bytes);
            Assertions.assertEquals("01234", new String(bytes, enc));
            cache.mark(999);
            cache.read(bytes);
            Assertions.assertEquals("56789", new String(bytes, enc));
            cache.reset();
            bytes = new byte[7];
            cache.read(bytes);
            Assertions.assertEquals("56789AB", new String(bytes, enc));
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }

    @Test
    void testContentMatchPoolMaxFileCache() throws IOException {
        CachedStreamFactory factory = new CachedStreamFactory(
                200 * 1024, 150 * 1024);
        CachedInputStream cache1 = null;
        CachedInputStream cache2 = null;
        try {
            // first time loads 6 bytes
            cache1 = factory.newInputStream(new NullInputStream(
                    140 * 1024));
            toString(cache1);
            assertThat(cache1.getMemCacheSize()).isEqualTo(140 * 1024);
            assertThat(cache1.isInMemory()).isTrue();

            // first time loads 6 bytes, totaling 12, forcing file cache
            cache2 = factory.newInputStream(new NullInputStream(
                    140 * 1024));
            toString(cache2);
            assertThat(cache2.getMemCacheSize()).isZero();
            assertThat(cache2.isInMemory()).isFalse();
        }  finally {
            try { cache1.close(); } catch (IOException e) { /*NOOP*/ }
            try { cache2.close(); } catch (IOException e) { /*NOOP*/ }
            cache1.dispose();
            cache2.dispose();
        }
    }

    @Test
    void testContentMatchInstanceFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());
        CachedStreamFactory factory = new CachedStreamFactory(200, 10);
        CachedInputStream cache = factory.newInputStream(is);
        try {
            // first time should cache
            Assertions.assertEquals(content, toString(cache));
            cache.rewind();
            // second time should read from cache
            Assertions.assertEquals(content, toString(cache));
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }

    @Test
    void testLengthNoReadFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        CachedInputStream cache = newCachedInputStream(content, 200, 10);
        try {
            // good length?
            Assertions.assertEquals(36, cache.length());
            // can re-read proper?
            cache.rewind();
            Assertions.assertEquals(content, toString(cache));
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }
    @Test
    void testLengthSmallReadFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        CachedInputStream cache = newCachedInputStream(content, 200, 10);
        try {
            byte[] bytes = new byte[5];
            // reads 01234
            cache.read(bytes);

            // good length?
            Assertions.assertEquals(content.length(), cache.length());
            // can resume reading where it was (56789)?
            cache.read(bytes);
            Assertions.assertEquals("56789", new String(bytes));
            // still good length?
            Assertions.assertEquals(content.length(), cache.length());
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }
    @Test
    void testLengthBigReadFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        CachedInputStream cache = newCachedInputStream(content, 200, 10);
        try {
            byte[] bytes = new byte[15];
            // reads 0123456789ABCDE
            cache.read(bytes);

            // good length?
            Assertions.assertEquals(content.length(), cache.length());
            // can resume reading where it was (FGHIJ)?
            bytes = new byte[5];
            cache.read(bytes);
            Assertions.assertEquals("FGHIJ", new String(bytes));
            // still good length?
            Assertions.assertEquals(content.length(), cache.length());
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }

    @Test
    void testLengthNoReadMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        CachedInputStream cache = newCachedInputStream(content, 200, 100);
        try {
            // good length?
            Assertions.assertEquals(36, cache.length());
            // can re-read proper?
            cache.rewind();
            Assertions.assertEquals(content, toString(cache));
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }
    @Test
    void testLengthSmallReadMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        CachedInputStream cache = newCachedInputStream(content, 200, 100);
        try {
            byte[] bytes = new byte[5];
            // reads 01234
            cache.read(bytes);

            // good length?
            Assertions.assertEquals(content.length(), cache.length());
            // can resume reading where it was (56789)?
            cache.read(bytes);
            Assertions.assertEquals("56789", new String(bytes));
            // still good length?
            Assertions.assertEquals(content.length(), cache.length());
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }
    @Test
    void testLengthBigReadMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        CachedInputStream cache = newCachedInputStream(content, 200, 100);
        try {
            byte[] bytes = new byte[15];
            // reads 0123456789ABCDE
            cache.read(bytes);

            // good length?
            Assertions.assertEquals(content.length(), cache.length());
            // can resume reading where it was (FGHIJ)?
            bytes = new byte[5];
            cache.read(bytes);
            Assertions.assertEquals("FGHIJ", new String(bytes));
            // still good length?
            Assertions.assertEquals(content.length(), cache.length());
        }  finally {
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
            cache.dispose();
        }
    }

    @Test
    void testCache() throws IOException {
        String val = "blah";
        CachedStreamFactory factory = new CachedStreamFactory(200, 10);
        CachedInputStream cis1 =
                CachedInputStream.cache(newInputStream(val), factory);
        assertThat(cis1.getStreamFactory()).isSameAs(factory);

        CachedInputStream cis2 =
                CachedInputStream.cache(newInputStream(val));
        assertThat(toString(cis1)).isEqualTo(val);
        assertThat(toString(cis2)).isEqualTo(val);

        assertThat(CachedInputStream.cache(null)).isNull();
        assertThat(CachedInputStream.cache(cis2)).isSameAs(cis2);
    }

    @Test
    void testMisc() throws IOException {
        CachedInputStream cis = CachedInputStream.cache(newInputStream("blah"));

        assertThat(cis.markSupported()).isTrue();
        cis.dispose();
        assertThat(cis.isDisposed()).isTrue();
        assertThat(cis.isCacheEmpty()).isTrue();
        assertThrows(IOException.class, () -> cis.read());

        assertThat(CachedInputStream.cache(
                newInputStream("")).isEmpty()).isTrue();

        assertThat(toString(
                cis.newInputStream(newInputStream("ok")))).isEqualTo("ok");
    }


    private CachedInputStream newCachedInputStream(
            String content, int poolMaxMemory, int instanceMaxMemory) {
        CachedStreamFactory factory = new CachedStreamFactory(200, 10);
        return factory.newInputStream(newInputStream(content));
    }

    private ByteArrayInputStream newInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private String toString(InputStream is) throws IOException {
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
}
