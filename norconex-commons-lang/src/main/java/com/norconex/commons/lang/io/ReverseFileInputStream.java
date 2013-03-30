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

    private final byte[] buffer = new byte[4096];

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

    /**
     * @see java.io.InputStream#close()
     */
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
