/* Copyright 2010-2013 Norconex Inc.
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
package com.norconex.commons.lang.map;

/**
 * <code>Properties</code> exception.  Typically thrown when 
 * setting/getting invalid property values.
 * @author Pascal Essiembre
 * @see Properties
 */
public class PropertiesException extends RuntimeException {

    /** For serialization. */
    private static final long serialVersionUID = 3040976896770771979L;

    /**
     * @see Exception#Exception(java.lang.String)
     */
    public PropertiesException(final String msg) {
        super(msg);
    }
    /**
     * @see Exception#Exception(java.lang.Throwable)
     */
    public PropertiesException(final Throwable cause) {
        super(cause);
    }
    /**
     * @see Exception#Exception(java.lang.String, java.lang.Throwable)
     */
    public PropertiesException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
