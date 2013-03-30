package com.norconex.commons.lang.io;

/**
 * Listener that is being notified every time a line is processed from a 
 * given stream.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 * @see StreamGobbler
 */
public interface IStreamListener {
    
    /**
     * Invoked when a line is streamed.
     * @param type type of line, as defined by the class using the listener
     * @param line line processed
     */
    void lineStreamed(String type, String line);
}
