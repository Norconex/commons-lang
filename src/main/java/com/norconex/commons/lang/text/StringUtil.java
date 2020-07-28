/* Copyright 2017-2018 Norconex Inc.
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
package com.norconex.commons.lang.text;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * String-related utility methods not found in popular libraries.
 * @author Pascal Essiembre
 * @since 1.14.0
 */
public final class StringUtil {

    private static final Logger LOG = LoggerFactory.getLogger(StringUtil.class);

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
            LOG.warn("\"maxLength\" is smaller than hash length ({}) "
                    + "+ separator length ({}). No truncation will occur.",
                    TRUNCATE_HASH_LENGTH, separatorLength);
        }
        int cutIndex = maxLength - roomLength;
        String truncated = StringUtils.left(text, cutIndex);
        String remainer = StringUtils.substring(text, cutIndex);
        if (separator != null) {
            truncated += separator;
        }
        truncated += getHash(remainer);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Truncated text: " + truncated);
        }
        return truncated;
    }

    /**
     * Truncates text with size in bytes larger than the given max byte
     * length and appends a hash
     * value from the truncated text.
     * The hash size is equal to the byte length of 10 digits using the
     * given charset.
     * The hash and separator are added to fit within the maximum byte length
     * supplied.
     * For this reason, the <code>maxByteLength</code> argument must be
     * be large enough for any truncation to occur.
     * @param text text to truncate
     * @param charset character encoding
     * @param maxByteLength maximum byte length the truncated text must have
     * @return truncated character byte array, or original text if no
     *         truncation required
     * @throws CharacterCodingException character coding problem
     */
    public static String truncateBytesWithHash(String text,
            Charset charset, int maxByteLength)
                    throws CharacterCodingException {
        if (text == null) {
            return null;
        }
        return new String(truncateBytesWithHash(
                text.getBytes(charset), charset, maxByteLength, null),
                charset);
    }
    /**
     * Truncates text with size in bytes larger than the given max byte
     * length and appends a hash
     * value from the truncated text, with an optional separator in-between.
     * The hash size is equal to the byte length of 10 digits using the
     * given charset.
     * The hash and separator are added to fit within the maximum byte length
     * supplied.
     * For this reason, the <code>maxByteLength</code> argument must be
     * be large enough for any truncation to occur.
     * @param text text to truncate
     * @param charset character encoding
     * @param maxByteLength maximum byte length the truncated text must have
     * @param separator string separating truncated text from hash code
     * @return truncated character byte array, or original text if no
     *         truncation required
     * @throws CharacterCodingException character coding problem
     */
    public static String truncateBytesWithHash(String text,
            Charset charset, int maxByteLength, String separator)
                    throws CharacterCodingException {
        if (text == null) {
            return null;
        }
        return new String(truncateBytesWithHash(
                text.getBytes(charset), charset, maxByteLength, separator),
                charset);
    }
    /**
     * Truncates character byte array text larger than the given max byte
     * length and appends a hash
     * value from the truncated text.
     * The hash size is equal to the byte length of 10 digits using the
     * given charset.
     * The hash and separator are added to fit within the maximum byte length
     * supplied.
     * For this reason, the <code>maxByteLength</code> argument must be
     * be large enough for any truncation to occur.
     * @param bytes byte array of text to truncate
     * @param charset character encoding
     * @param maxByteLength maximum byte length the truncated text must have
     * @return truncated character byte array, or original text if no
     *         truncation required
     * @throws CharacterCodingException character coding problem
     */
    public static byte[] truncateBytesWithHash(
            byte[] bytes, Charset charset, int maxByteLength)
                    throws CharacterCodingException {
        if (bytes == null) {
            return bytes;
        }
        return truncateBytesWithHash(bytes, charset, maxByteLength, null);
    }
    /**
     * Truncates character byte array text larger than the given max byte
     * length and appends a hash
     * value from the truncated text, with an optional separator in-between.
     * The hash size is equal to the byte length of 10 digits using the
     * given charset.
     * The hash and separator are added to fit within the maximum byte length
     * supplied.
     * For this reason, the <code>maxByteLength</code> argument must be
     * be large enough for any truncation to occur.
     * @param bytes byte array of text to truncate
     * @param charset character encoding
     * @param maxByteLength maximum byte length the truncated text must have
     * @param separator string separating truncated text from hash code
     * @return truncated character byte array, or original text if no
     *         truncation required
     * @throws CharacterCodingException character coding problem
     */
    public static byte[] truncateBytesWithHash(
            byte[] bytes, Charset charset, int maxByteLength, String separator)
                    throws CharacterCodingException {
        if (bytes == null) {
            return bytes;
        }
        if (bytes.length <= maxByteLength) {
            return bytes;
        }

        Charset nullSafeCharset = charset;
        if (nullSafeCharset == null) {
            nullSafeCharset = StandardCharsets.UTF_8;
        }

        int separatorLength = separator == null
                ? 0 : separator.getBytes(nullSafeCharset).length;
        int hashLength = StringUtils.repeat(
                '0', TRUNCATE_HASH_LENGTH).getBytes(nullSafeCharset).length;
        int roomLength = hashLength + separatorLength;

        if (maxByteLength < roomLength) {
            LOG.warn("\"maxLength\" is smaller in bytes than hash length ({}) "
                    + "+ separator length ({}). No truncation will occur.",
                    hashLength, separatorLength);
        }

        int cutIndex = maxByteLength - roomLength;
        final CharsetDecoder decoder = nullSafeCharset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.reset();

        String fullString = new String(bytes, nullSafeCharset);
        String truncated =
                decoder.decode(ByteBuffer.wrap(bytes, 0, cutIndex)).toString();
        String remainer = StringUtils.substring(fullString, truncated.length());
        if (separator != null) {
            truncated += separator;
        }
        truncated += getHash(remainer);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Truncated text: {}", truncated);
        }
        return truncated.getBytes(nullSafeCharset);
    }

    static String getHash(String s) {
        return StringUtils.leftPad(StringUtils.stripStart(
                Integer.toString(s.hashCode()), "-"),
                TRUNCATE_HASH_LENGTH, '0');
    }
}
