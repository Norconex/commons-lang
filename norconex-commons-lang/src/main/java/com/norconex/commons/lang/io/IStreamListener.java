package com.norconex.commons.lang.io;

/**
 * Listener that is being notified every time a line is processed from a 
 * given stream.
 * @author Pascal Essiembre (pascal.essiembre&#x40;norconex.com)
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
