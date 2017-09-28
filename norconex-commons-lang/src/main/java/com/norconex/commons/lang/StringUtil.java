/* Copyright 2017 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * String-related utility methods not found in popular libraries.
 * @author Pascal Essiembre
 * @since 1.14.0
 */
public final class StringUtil {

    private static final Logger LOG = LogManager.getLogger(StringUtil.class);
    
    public static final int TRUNCATE_HASH_LENGTH = 24;
    
    private StringUtil() {
        super();
    }

    /**
     * Truncate text larger than the given max Length and appends a hash
     * value from the truncated text.  The hash is added to fit within
     * the maxLength. The maxLength 
     * argument must be minimum 24, to leave room for the hash.
     * The hash is added without a separator.  To insert a separator between
     * the truncated text and the hash code, use 
     * {@link #truncateWithHash(String, int, char)}
     * @param text text to truncate
     * @param maxLength maximum length the truncated text must have
     * @return truncated text, or original text if no truncation required
     */
    public static String truncateWithHash(
            String text, int maxLength) {
       return truncateWithHash(text, maxLength, '\0'); 
    }
    /**
     * Truncate text larger than the given max Length and appends a hash
     * value from the truncated text.  The hash is added to fit within
     * the maxLength. The maxLength 
     * argument must be minimum 24, to leave room for the hash.
     * Unless the hashSeparator is null ('\0'), it will be inserted
     * between the truncated text and the hash code.
     * @param text text to truncate
     * @param maxLength maximum length the truncated text must have
     * @param hashSeparator character separating truncated text from hash code
     * @return truncated text, or original text if no truncation required
     */
    public static String truncateWithHash(
            String text, int maxLength, char hashSeparator) {
        if (text == null) {
            return null;
        }
        if (maxLength < TRUNCATE_HASH_LENGTH) {
            throw new IllegalArgumentException("\"maxLength\" (" 
                    + maxLength + ") cannot be smaller than "
                    + TRUNCATE_HASH_LENGTH + ".");
        }
        if (text.length() <= maxLength) {
            return text;
        }
        int cutIndex = maxLength - TRUNCATE_HASH_LENGTH;
        String truncated = StringUtils.left(text, cutIndex);
        int hash = StringUtils.substring(text, cutIndex).hashCode();
        if (hashSeparator != '\0') {
            truncated += Character.toString(hashSeparator);
        }
        truncated += Integer.toString(hash);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Truncated text: " + truncated);
        }
        return truncated;
    }
}
