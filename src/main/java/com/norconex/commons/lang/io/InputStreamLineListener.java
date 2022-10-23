/* Copyright 2017-2022 Norconex Inc.
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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;

/**
 * Listener that is being notified every time a line is processed from a
 * given stream.  Not thread-safe. Use a new instance for each thread or at
 * a minimum, make sure to give a unique type argument to
 * each {@link InputStreamConsumer} to prevent lines content being mixed up.
 * @author Pascal Essiembre
 * @see InputStreamConsumer
 */
public abstract class InputStreamLineListener implements IInputStreamListener {

    private static final byte NL = (byte) '\n';
    private static final byte CR = (byte) '\r';
    private static final byte RESET = (byte) '\0';

    private final Map<String, Buffer> buffers = new HashMap<>();

    private final Charset charset;

    /**
     * Creates a line listener with UTF-8 character encoding.
     */
    protected InputStreamLineListener() {
        this(StandardCharsets.UTF_8);
    }
    /**
     * Creates a line listener with supplied character encoding
     * (defaults to UTF-8 if null).
     * @param charset character encoding
     */
    protected InputStreamLineListener(Charset charset) {
        if (charset == null) {
            this.charset = StandardCharsets.UTF_8;
        } else {
            this.charset = charset;
        }
    }
    /**
     * Creates a line listener with supplied character encoding
     * (defaults to UTF-8 if null).
     * @param charset character encoding
     */
    protected InputStreamLineListener(String charset) {
        if (charset == null) {
            this.charset = StandardCharsets.UTF_8;
        } else {
            this.charset = Charsets.toCharset(charset);
        }
    }

    /**
     * Gets the character encoding this listener is expected to receive.
     * @return character encoding
     * @since 3.0.0
     */
    protected Charset getCharset() {
        return charset;
    }

    @Override
    public void streamed(String type, byte[] bytes, int length) {
        Buffer buffer = getBuffer(type);
        if (length == -1) {
            if (!buffer.lastEmpty) {
                flushBuffer(type, buffer);
            }
            return;
        }

        for (int i = 0; i < length; i++) {
            byte b = bytes[i];
            if (isEOL(b)) {
                if (isEOL(buffer.lastEolByte) && b != buffer.lastEolByte) {
                    buffer.lastEolByte = RESET;
                } else {
                    flushBuffer(type, buffer);
                    buffer.lastEolByte = b;
                }
            } else {
                buffer.lastEolByte = RESET;
                buffer.baos.write(b);
                buffer.lastEmpty = false;
            }
        }
    }

    /**
     * Invoked when a line is streamed.
     * @param type type of line, as defined by the class using the listener
     * @param line line processed
     */
    protected abstract void lineStreamed(String type, String line);

    private boolean isEOL(byte b) {
        return b == NL || b == CR;
    }

    private void flushBuffer(String type, Buffer buffer) {
        try {
            lineStreamed(type, buffer.baos.toString(charset.toString()));
            buffer.baos.reset();
            buffer.lastEmpty = true;
        } catch (UnsupportedEncodingException e) {
            throw new StreamException("Unsupported charset: " + charset, e);
        }
    }

    private synchronized Buffer getBuffer(String type) {
        String key = type;
        if (key == null) {
            key = StringUtils.EMPTY;
        }
        return buffers.computeIfAbsent(key, k -> new Buffer());
    }

    class Buffer {
        private boolean lastEmpty = true;
        private byte lastEolByte = RESET;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    }
}
