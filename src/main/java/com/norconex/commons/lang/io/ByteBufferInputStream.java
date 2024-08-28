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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Wraps a {@link ByteBuffer} in an {@link InputStream}.
 * @since 1.5
 */
public class ByteBufferInputStream extends InputStream {

    private final ByteBuffer buf;

    public ByteBufferInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public int read() throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }
        int b = buf.get() & 0xFF;
        if (b == 0) {
            return -1;
        }
        return b;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }
        int newLen = Math.min(len, buf.remaining());
        buf.get(bytes, off, newLen);

        if (newLen < len) {
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] == 0) {
                    return i;
                }
            }
        }
        return newLen;
    }
}