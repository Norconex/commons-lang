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
 * @author Pascal Essiembre (pascal.essiembre&#x40;norconex.com)
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
