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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
class CachedOutputStreamTest {

    @Test
    void testContentMatchMemCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        CachedStreamFactory factory = new CachedStreamFactory(200, 100);
        CachedOutputStream cache = factory.newOuputStream();
        InputStream is = null;

        try {
            cache.write(content.getBytes());
            is = cache.getInputStream();
            Assertions.assertEquals(content, readCacheToString(is));
        }  finally {
            try { is.close(); } catch (IOException e) { /*NOOP*/ }
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
        }
    }

    @Test
    void testContentMatchFileCache() throws IOException {
        String content = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        CachedStreamFactory factory = new CachedStreamFactory(200, 10);
        CachedOutputStream cache = factory.newOuputStream();

        InputStream is = null;
        try {
            cache.write(content.getBytes());
            is = cache.getInputStream();
            Assertions.assertEquals(content, readCacheToString(is));
        }  finally {
            try { is.close(); } catch (IOException e) { /*NOOP*/ }
            try { cache.close(); } catch (IOException e) { /*NOOP*/ }
        }
    }

    @Test
    void testMisc() throws IOException {
        CachedStreamFactory factory = new CachedStreamFactory(200, 10);
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();

        CachedOutputStream cos1 = factory.newOuputStream(out1);
        CachedOutputStream cos2 =
                factory.newOuputStream(new BufferedOutputStream(out2));

        cos1.write("blah".getBytes());
        cos1.write((byte) '!');
        assertThat(cos1.isCacheEmpty()).isFalse();
        cos1.close();
        assertThat(cos1.isCacheEmpty()).isTrue();

        cos2.write("blah".getBytes());
        cos2.write((byte) '!');
        assertThat(cos2.isCacheEmpty()).isFalse();
        cos2.close();
        assertThat(cos2.isCacheEmpty()).isTrue();

        assertThat(new String(out1.toByteArray())).isEqualTo("blah!");
        assertThat(new String(out2.toByteArray())).isEqualTo("blah!");

        assertThat(cos1.getStreamFactory()).isSameAs(factory);
        assertThat(cos2.getStreamFactory()).isSameAs(factory);

        assertDoesNotThrow(() -> {
            cos1.newOuputStream(new java.io.ByteArrayOutputStream());
            cos2.newOuputStream();
        });
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
