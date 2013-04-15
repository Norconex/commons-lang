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

/**
 * Decorates an InputStream with a stream filter.  The stream filter
 * allows to control which line of text is being returned by the decorated
 * instance when read.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public class FilteredInputStream extends InputStream {
    
    private final BufferedReader bufferedInput;
    private final IInputStreamFilter filter;
    private InputStream lineStream;
    private boolean closed = false;

    /**
     * Constructor.
     * @throws IOException
     */
    public FilteredInputStream(InputStream is, IInputStreamFilter filter)
            throws IOException {
        super();
        
        this.bufferedInput = new BufferedReader(
                new InputStreamReader(is));
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
                lineStream = new ByteArrayInputStream(line.getBytes());
                return true;
            }
        }
        bufferedInput.close();
        closed = true;
        return false;
    }
}
