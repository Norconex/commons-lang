/* Copyright 2010-2014 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.file;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Represent a file Content-Type (also called MIME-Type or Media Type).
 * <p/>
 * To detect the content type of a file, consider using an open-source library
 * such as <a href="https://tika.apache.org/">Apache Tika</a>.
 * 
 * @author Pascal Essiembre
 * @since 1.4.0
 */
public final class ContentType implements Serializable {

    //TODO consider creating a ContentTypeFamily class, where display names
    //would be less specific (e.g. Microsoft Office Document), or consider
    //adding a getFamilyName method or similar.
    
    private static final long serialVersionUID = 6416074869536512030L;

    private static Logger LOG = LogManager.getLogger(ContentType.class);
    
    private static final Map<String, ContentType> REGISTRY = 
        new HashMap<String, ContentType>();

    public static final ContentType HTML = new ContentType("text/html");
    public static final ContentType PDF = new ContentType("application/pdf");

    //TODO how many do we want? Do we list them all??
    
    private String contentType;

    /**
     * Constructor.
     * @param contentType string representation of a content type
     */
    private ContentType(String contentType) {
        super();
        this.contentType = contentType;
        REGISTRY.put(contentType, this);
    }

    /**
     * Creates a new content type.  Returns an existing instance if the 
     * same content type is requested more than once.
     * @param contentType the official media type name
     * @return content type instance or {@code null} if content type string is
     *         {@code null} or blank.
     */
    public static ContentType get(String contentType) {
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
     * Gets a name for the content type suitable for display to a user.
     * The system locale is used to defined the langage of the display name.
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
            return ResourceBundle.getBundle(
                    ContentType.class.getName() + "-names",
                    safeLocale).getString(contentType);
        } catch (MissingResourceException e) {
            LOG.warn("Could not find display name for content type: "
                    + contentType);
        }
        return contentType;
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
            String ext = ResourceBundle.getBundle(ContentType.class.getName()
                    + "-extensions").getString(contentType);
            return StringUtils.split(ext, ',');
        } catch (MissingResourceException e) {
            LOG.warn("Could not find extension(s) for content type: "
                    + contentType);
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    
    /**
     * Whether the given string matches this content type.
     * @return {@code true} if the given string matches this content type
     */
    public boolean matches(String contentType) {
        return this.contentType.equals(StringUtils.trim(contentType));
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((contentType == null) ? 0 : contentType.hashCode());
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
        if (contentType == null) {
            if (other.contentType != null) {
                return false;
            }
        } else if (!contentType.equals(other.contentType)) {
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
        return contentType;
    }
    
}
