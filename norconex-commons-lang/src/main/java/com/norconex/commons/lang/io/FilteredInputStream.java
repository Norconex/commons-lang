/* Copyright 2010-2017 Norconex Inc.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Decorates an InputStream with a stream filter.  The stream filter
 * allows to control which line of text is being returned by the decorated
 * instance when read.
 * @author Pascal Essiembre
 */
public class FilteredInputStream extends InputStream {
    
    private final BufferedReader bufferedInput;
    private final IInputStreamFilter filter;
    private final Charset encoding;
    private InputStream lineStream;
    private boolean closed = false;

    /**
     * Constructor, using UTF-8 as the character encoding.
     * @param is input stream to filter
     * @param filter the filter to apply
     * @throws IOException i/o problem
     */
    public FilteredInputStream(InputStream is, IInputStreamFilter filter)
            throws IOException {
        this(is, filter, StandardCharsets.UTF_8);
    }
    /**
     * Constructor.
     * @param is input stream to filter
     * @param filter the filter to apply
     * @param encoding character encoding
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public FilteredInputStream(
            InputStream is, IInputStreamFilter filter, Charset encoding)
            throws IOException {
        super();
        if (encoding == null) {
            this.encoding = StandardCharsets.UTF_8;
        } else {
            this.encoding = encoding;
        }
        this.bufferedInput = new BufferedReader(
                new InputStreamReader(is, this.encoding));
        this.filter = filter;
        nextLine();
    }
    /**
     * Constructor.
     * @param is input stream to filter
     * @param filter the filter to apply
     * @param encoding character encoding
     * @throws IOException i/o problem
     * @since 1.5.0
     */
    public FilteredInputStream(
            InputStream is, IInputStreamFilter filter, String encoding)
            throws IOException {
        this(is, filter, Charset.forName(encoding));
    }

    @Override
    public int read() throws IOException {
        if (lineStream == null) {
            return -1;
        }
        int ch = lineStream.read();
        if (ch == -1) {
            if (!nextLine()) {
                return -1;
            }
            return read();
        }
        return ch;
    }

    @SuppressWarnings("nls")
    private boolean nextLine() throws IOException {
        if (lineStream != null) {
            lineStream.close();
            lineStream = null;
        }
        if (closed) {
            return false;
        }
        String line;
        while ((line = bufferedInput.readLine()) != null) {
            if (filter.accept(line)) {
                line += "\n";
                lineStream = new ByteArrayInputStream(line.getBytes(encoding));
                return true;
            }
        }
        bufferedInput.close();
        closed = true;
        return false;
    }
}
