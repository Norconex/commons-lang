package com.norconex.commons.lang.map;

/**
 * <code>TypeProperties</code> exception.  Typically thrown when 
 * setting/getting invalid property values.
 * @author Pascal Essiembre (pascal.essiembre&#x40;norconex.com)
 * @see TypedProperties
 */
public class TypedPropertiesException extends RuntimeException {

    /** For serialization. */
    private static final long serialVersionUID = 3040976896770771979L;

    /**
     * @see Exception#Exception(java.lang.String)
     */
    public TypedPropertiesException(final String msg) {
        super(msg);
    }
    /**
     * @see Exception#Exception(java.lang.Throwable)
     */
    public TypedPropertiesException(final Throwable cause) {
        super(cause);
    }
    /**
     * @see Exception#Exception(java.lang.String, java.lang.Throwable)
     */
    public TypedPropertiesException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
