package com.norconex.commons.lang.url;

/**
 * Runtime exception thrown when dealing with a URL fails.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 *
 */
public class URLException extends RuntimeException {

    private static final long serialVersionUID = 8484839654375152232L;

    public URLException() {
        super();
    }

    public URLException(String message) {
        super(message);
    }

    public URLException(Throwable cause) {
        super(cause);
    }

    public URLException(String message, Throwable cause) {
        super(message, cause);
    }

}
