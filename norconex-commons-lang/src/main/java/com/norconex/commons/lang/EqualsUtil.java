package com.norconex.commons.lang;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Convenience methods related to object equality.
 * @author Pascal Essiembre
 */
public final class EqualsUtil {

    private EqualsUtil() {
        super();
    }

    /**
     * Whether a source object equals ANY of the target objects
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if any of the target objects is equal
     */
    public static boolean equalsAny(Object source, Object... targets) {
        if (targets == null) {
            return source == null;
        }
        for (Object object : targets) {
            if (ObjectUtils.equals(source, object)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Whether a source object equals ALL of the target objects
     * @param source object being tested for equality with targets
     * @param targets one or more objects to be tested with source for equality
     * @return <code>true</code> if all of the target objects is equal
     */
    public static boolean equalsAll(Object source, Object... targets) {
        if (targets == null) {
            return source == null;
        }
        for (Object object : targets) {
            if (!ObjectUtils.equals(source, object)) {
                return false;
            }
        }
        return true;
    }
}
