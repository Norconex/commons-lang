/* Copyright 2010-2018 Norconex Inc.
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
package com.norconex.commons.lang.file;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a file Content-Type (also called MIME-Type or Media Type).
 * <br><br>
 * To detect the content type of a file, consider using an open-source library
 * such as <a href="https://tika.apache.org/">Apache Tika</a>.
 * <br><br>
 * To provide your own extension mappings or display names, copy the
 * appropriate <code>.properties</code> file to your classpath root, with
 * the word "custom" inserted: <code>ContentType-custom-[...]</code>.
 * The actual custom names and classpath location are:
 * <br><br>
 * <table border="1" summary="">
 *   <tr>
 *     <th>Original</th>
 *     <th>Custom</th>
 *   </tr>
 *   <tr>
 *     <td>com.norconex.commmons.lang.file.ContentType-extensions.properties</td>
 *     <td>ContentType-custom-extensions.properties</td>
 *   </tr>
 *   <tr>
 *     <td>com.norconex.commmons.lang.file.ContentType-name[_locale].properties</td>
 *     <td>ContentType-custom-name[_locale].propertiess</td>
 *   </tr>
 * </table>
 *
 * @author Pascal Essiembre
 * @since 1.4.0
 */
public final class ContentType implements Serializable {

    private static final long serialVersionUID = 6416074869536512030L;

    private static final Logger LOG =
            LoggerFactory.getLogger(ContentType.class);

    private static final Map<String, ContentType> REGISTRY = new HashMap<>();

    private static final ResourceBundle BUNDLE_EXTENSIONS;
    static {
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle(
                    ContentType.class.getSimpleName() + "-custom-extensions");
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle(
                    ContentType.class.getName() + "-extensions");
        }
        BUNDLE_EXTENSIONS = bundle;
    }
    private static final Map<Locale, ResourceBundle> BUNDLE_DISPLAYNAMES =
            new HashMap<>();

    //TODO how many do we want? Do we list them all??
    public static final ContentType TEXT = new ContentType("text/plain");
    public static final ContentType HTML = new ContentType("text/html");
    public static final ContentType PDF = new ContentType("application/pdf");
    public static final ContentType XML = new ContentType("application/xml");
    public static final ContentType CSV = new ContentType("text/csv");
    public static final ContentType TSV =
            new ContentType("text/tab-separated-values");
    /** @since 1.14.0*/
    public static final ContentType ZIP = new ContentType("application/zip");

    // Common images:
    public static final ContentType JPEG = new ContentType("image/jpeg");
    public static final ContentType GIF = new ContentType("image/gif");
    public static final ContentType BMP = new ContentType("image/bmp");
    public static final ContentType PNG = new ContentType("image/png");

    private final String type;

    /**
     * Constructor.
     * @param contentType string representation of a content type
     */
    private ContentType(String contentType) {
        super();
        this.type = contentType;
        REGISTRY.put(contentType, this);
    }

    /**
     * Creates a new content type.  Returns an existing instance if the
     * same content type is requested more than once.
     * @param contentType the official media type name
     * @return content type instance or {@code null} if content type string is
     *         {@code null} or blank.
     */
    public static ContentType valueOf(String contentType) {
        String trimmedType = StringUtils.trim(contentType);
        if (StringUtils.isBlank(trimmedType)) {
            return null;
        }
        ContentType type = REGISTRY.get(trimmedType);
        if (type != null) {
            return type;
        }
        return new ContentType(trimmedType);
    }

    /**
     * Creates a null-safe array of content types.  The same number of elements
     * as the supplied strings are returned.  A <code>null</code> value will
     * return an empty array.  Each content types are individually obtained
     * by invoking {@link #valueOf(String)}.
     * @param contentTypes the official media type names
     * @return content type array.
     */
    public static ContentType[] valuesOf(String... contentTypes) {
        if (!ArrayUtils.isEmpty(contentTypes)) {
            ContentType[] cts = new ContentType[contentTypes.length];
            for (int i = 0; i < contentTypes.length; i++) {
                String ctString = contentTypes[i];
                cts[i] = ContentType.valueOf(ctString);
            }
            return cts;
        }
        return new ContentType[] {};
    }
    /**
     * Creates a null-safe array of content types.  The same number of elements
     * as the supplied strings are returned.  A <code>null</code> value will
     * return an empty array.  Each content types are individually obtained
     * by invoking {@link #valueOf(String)}.
     * @param contentTypes the official media type names
     * @return content type array.
     * @since 2.0.0
     */
    public static List<ContentType> valuesOf(List<String> contentTypes) {
        if (CollectionUtils.isNotEmpty(contentTypes)) {
            return contentTypes.stream().map(
                    ContentType::valueOf).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Gets a name for the content type suitable for display to a user.
     * The system locale is used to defined the language of the display name.
     * If no name has been defined for a content type, the raw content type
     * is returned (equivalent to {@link #toString()}).
     * @return display name
     */
    public String getDisplayName() {
        return getDisplayName(null);
    }
    /**
     * Gets a name for the content type suitable for display to a user.
     * If the locale is {@code null}, the system locale is used.
     * If no name has been defined for a content type with the provided locale,
     * the name defaults to English.
     * If no name has been defined for any locale, the raw content type
     * is returned (equivalent to {@link #toString()}).
     * @param locale the locale to use to get the display name
     * @return display name
     */
    public String getDisplayName(Locale locale) {
        Locale safeLocale = locale;
        if (safeLocale == null) {
            safeLocale = Locale.getDefault();
        }
        try {
            return getDisplayBundle(safeLocale).getString(toBaseTypeString());
        } catch (MissingResourceException e) {
            LOG.debug("Could not find display name for content type: "
                    + type);
        }
        return "[" + type + "]";
    }
    private ResourceBundle getDisplayBundle(Locale locale) {
        ResourceBundle bundle = BUNDLE_DISPLAYNAMES.get(locale);
        if (bundle != null) {
            return bundle;
        }
        try {
            bundle = ResourceBundle.getBundle(ContentType.class.getSimpleName()
                    + "-custom-names", locale);
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle(
                    ContentType.class.getName() + "-names", locale);
        }
        BUNDLE_DISPLAYNAMES.put(locale, bundle);
        return bundle;
    }

    public ContentFamily getContentFamily() {
        return ContentFamily.forContentType(type);
    }

    /**
     * Gets the file extension usually associated with this content type.
     * If the content type has more than one extension, the first one
     * is returned.
     * @return file extension or empty string if no extension is defined
     */
    public String getExtension() {
        String[] exts = getExtensions();
        if (exts == null || exts.length == 0) {
            return StringUtils.EMPTY;
        }
        return exts[0];
    }
    /**
     * Gets the file extensions usually associated with this content type.
     * Most content types only have one commonly used extension.
     * @return file extension or empty array if no extension is defined
     */
    public String[] getExtensions() {
        try {
            String ext = BUNDLE_EXTENSIONS.getString(toBaseTypeString());
            return StringUtils.split(ext, ',');
        } catch (MissingResourceException e) {
            LOG.debug("Could not find extension(s) for content type: "
                    + type);
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    /**
     * Whether the given string matches this content type.
     * @param contentType the content type
     * @return {@code true} if the given string matches this content type
     */
    public boolean matches(String contentType) {
        return this.type.equals(StringUtils.trim(contentType));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContentType other = (ContentType) obj;
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the raw content-type representation.
     * @return content type as string
     */
    @Override
    public String toString() {
        return type;
    }
    /**
     * Returns the raw content-type representation without any parameters
     * (removes ";" and any values afterwards).
     * @return content type as string without parameters
     * @since 1.14.0
     */
    public String toBaseTypeString() {
        return StringUtils.substringBefore(type, ";");
    }
    /**
     * Returns a content-type without any parameters
     * (removes ";" and any values afterwards).  Invoking a content type
     * without parameters will return itself.
     * @return content type without parameters
     * @since 1.14.0
     */
    public ContentType toBaseType() {
        if (type.contains(";")) {
            return ContentType.valueOf(toBaseTypeString());
        }
        return this;
    }

}
