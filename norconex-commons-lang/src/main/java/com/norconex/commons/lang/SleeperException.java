package com.norconex.commons.lang;

/**
 * Runtime <code>Sleep</code> exception wrapping any 
 * {@link InterruptedException} thrown.
 * @author Pascal Essiembre
 * @see Sleeper
 */
public class SleeperException extends RuntimeException {

    private static final long serialVersionUID = -6879301747242838385L;

    /**
     * @see Exception#Exception(java.lang.String, java.lang.Throwable)
     */
    public SleeperException(
            final String msg, final InterruptedException cause) {
        super(msg, cause);
    }
}
