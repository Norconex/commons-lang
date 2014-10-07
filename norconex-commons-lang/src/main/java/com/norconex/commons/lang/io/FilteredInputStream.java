/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

/**
 * Decorates an InputStream with a stream filter.  The stream filter
 * allows to control which line of text is being returned by the decorated
 * instance when read.
 * @author Pascal Essiembre
 */
public class FilteredInputStream extends InputStream {
    
    private final BufferedReader bufferedInput;
    private final IInputStreamFilter filter;
    private final String encoding;
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
        this(is, filter, null);
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
        super();
        if (StringUtils.isBlank(encoding)) {
            this.encoding = CharEncoding.UTF_8;
        } else {
            this.encoding = encoding;
        }
        this.bufferedInput = new BufferedReader(
                new InputStreamReader(is, this.encoding));
        this.filter = filter;
        nextLine();
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
