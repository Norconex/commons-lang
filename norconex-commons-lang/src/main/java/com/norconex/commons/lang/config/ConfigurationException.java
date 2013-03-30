package com.norconex.commons.lang.config;

/**
 * Runtime exception for configuration related issues.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 8484839654375152232L;

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
