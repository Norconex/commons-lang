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
    
    public static final int TRUNCATE_HASH_LENGTH = 10;
    
    private StringUtil() {
        super();
    }

    /**
     * Truncate text larger than the given max length and appends a hash
     * value from the truncated text. The hash size has 10 digits.
     * The hash is added to fit within the maximum length supplied.
     * For this reason, the <code>maxLength</code> argument must be 
     * be minimum 10 for any truncation to occur.
     * The hash is added without a separator.  To insert a separator between
     * the truncated text and the hash code, use 
     * {@link #truncateWithHash(String, int, String)}
     * @param text text to truncate
     * @param maxLength maximum length the truncated text must have
     * @return truncated text, or original text if no truncation required
     */
    public static String truncateWithHash(
            String text, int maxLength) {
        return truncateWithHash(text, maxLength, null); 
    }
    /**
     * Truncate text larger than the given max length and appends a hash
     * value from the truncated text, with an optional separator in-between.
     * The hash size has 10 digits. The hash and separator are added to fit 
     * within the maximum length supplied. 
     * For this reason, the <code>maxLength</code> argument must be 
     * be minimum 10 + separator length for any truncation to occur.
     * @param text text to truncate
     * @param maxLength maximum length the truncated text must have
     * @param separator string separating truncated text from hash code
     * @return truncated text, or original text if no truncation required
     */
    public static String truncateWithHash(
            String text, int maxLength, String separator) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        
        int separatorLength = separator == null ? 0 : separator.length();
        int roomLength = TRUNCATE_HASH_LENGTH + separatorLength;
        
        if (maxLength < roomLength) {
            LOG.warn("\"maxLength\" is smaller than hash length ("
                    + TRUNCATE_HASH_LENGTH + ") + separator length ("
                    + separatorLength + "). No truncation will occur.");
        }
        int cutIndex = maxLength - roomLength;
        String truncated = StringUtils.left(text, cutIndex);
        int hash = StringUtils.substring(text, cutIndex).hashCode();
        if (separator != null) {
            truncated += separator;
        }
        truncated += StringUtils.leftPad(StringUtils.stripStart(
                Integer.toString(hash), "-"), TRUNCATE_HASH_LENGTH, '0');
        if (LOG.isTraceEnabled()) {
            LOG.trace("Truncated text: " + truncated);
        }
        return truncated;
    }
}
