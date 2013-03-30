package com.norconex.commons.lang.io;

/**
 * Filters lines of text read from an InputStream decorated with 
 * {@link FilteredInputStream}.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public interface IInputStreamFilter {

    /**
     * Whether a line is "accepted" or not.  An accepted line is being
     * returned when read.
     * @param line line being read
     * @return <code>true</code> if line is accepted
     */
    boolean accept(String line);
}
