package com.norconex.commons.lang.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * I/O related utility methods.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public final class IOUtil {

    /** Empty strings. */
    private static final String[] EMPTY_STRINGS = new String[] {};

    /**
     * Constructor.
     */
    private IOUtil() {
        super();
    }

    /**
     * Gets the last lines from an input stream.  This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.  For files, one should use
     * {@link FileUtil#tail(java.io.File, int)} which is more efficient,
     * especially on large files.
     * @param is input stream
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     */
    public static String[] tail(final InputStream is, final int lineQty)
            throws IOException {
        if (is == null) {
            return EMPTY_STRINGS;
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<String> lines = new ArrayList<String>(lineQty);
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(0, line);
            if (lines.size() > lineQty) {
                lines.remove(lineQty);
            }
        }
        br.close();
        Collections.reverse(lines);
        return (String[]) lines.toArray(EMPTY_STRINGS);
    }

    /**
     * Gets the first lines from an input stream.  This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.
     * @param is input stream
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     */
    public static String[] head(
            final InputStream is, final int lineQty)
            throws IOException {
        if (is == null) {
            return EMPTY_STRINGS;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<String> lines = new ArrayList<String>(lineQty);
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
            if (lines.size() == lineQty) {
                break;
            }
        }
        br.close();
        return (String[]) lines.toArray(EMPTY_STRINGS);
    }
}
