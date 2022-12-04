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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * {@link InputStream} implementation for streaming files in reverse order
 * (from the end of file to its beginning).
 */
public class ReverseFileInputStream extends InputStream { //NOSONAR

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
        return buffer[currentPositionInBuffer] & 0xFF; // make it a signed byte
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
