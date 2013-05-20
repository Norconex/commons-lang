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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * {@link InputStream} implementation for streaming files in reverse order
 * (from the end of file to its beginning). 
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
@SuppressWarnings("nls")
public class ReverseFileInputStream extends InputStream {

    private static final int BUFFER_SIZE = 4096;
    
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private final RandomAccessFile raf;

    private long currentPositionInFile;

    private int currentPositionInBuffer;

    /**
     * Creates a new <code>ReverseFileInputStream</code> instance.
     * @param file the file to stream
     * @throws IOException problem streaming the file
     */
    public ReverseFileInputStream(File file) throws IOException {
        assertFile(file);
        raf = new RandomAccessFile(file, "r");
        currentPositionInFile = raf.length();
        currentPositionInBuffer = 0;
    }

    @Override
    public int read() throws IOException {
        if (currentPositionInFile <= 0) {
            return -1;
        }
        if (--currentPositionInBuffer < 0) {
            currentPositionInBuffer = buffer.length;
            long startOfBlock = currentPositionInFile - buffer.length;
            if (startOfBlock < 0) {
                currentPositionInBuffer = buffer.length + (int) startOfBlock;
                startOfBlock = 0;
            }
            raf.seek(startOfBlock);
            raf.readFully(buffer, 0, currentPositionInBuffer);
            return read();
        }
        currentPositionInFile--;
        return buffer[currentPositionInBuffer];
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
    
    private static void assertFile(File file) throws IOException {
        if (file == null || !file.exists()
                || !file.isFile() || !file.canRead()) {
            throw new IOException("Not a valid file: " + file);
        }
    }
}
