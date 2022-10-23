/* Copyright 2010-2022 Norconex Inc.
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
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

/**
 * Decorates an InputStream with a stream filter.  The stream filter
 * allows to control which line of text is being returned by the decorated
 * instance when read.
 * @author Pascal Essiembre
 */
public class FilteredInputStream extends InputStream { // NOSONAR

    private final BufferedReader bufferedInput;
    private final Predicate<String> filter;
    private final Charset encoding;
    private InputStream lineStream;
    private boolean closed = false;

    /**
     * Constructor, using UTF-8 as the character encoding.
     * @param is input stream to filter
     * @param filter the filter to apply
     * @throws IOException i/o problem
     */
    public FilteredInputStream(InputStream is, Predicate<String> filter)
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
            InputStream is, Predicate<String> filter, Charset encoding)
            throws IOException {
        if (encoding == null) {
            this.encoding = StandardCharsets.UTF_8;
        } else {
            this.encoding = encoding;
        }
        bufferedInput = new BufferedReader(
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
            InputStream is, Predicate<String> filter, String encoding)
            throws IOException {
        this(is, filter, isBlank(encoding) ? UTF_8 : Charset.forName(encoding));
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
            if (filter.test(line)) {
                line += "\n"; //NOSONAR
                lineStream = new ByteArrayInputStream(line.getBytes(encoding));
                return true;
            }
        }
        bufferedInput.close();
        closed = true;
        return false;
    }
}
