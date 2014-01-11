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
package com.norconex.commons.lang.url;

/**
 * Runtime exception thrown when dealing with URL failures.
 * @author Pascal Essiembre
 */
public class URLException extends RuntimeException {

    private static final long serialVersionUID = 8484839654375152232L;

    /**
     * Constructor.
     */
    public URLException() {
    }
    /**
     * Constructor.
     * @param message exception message
     */
    public URLException(String message) {
        super(message);
    }
    /**
     * Constructor.
     * @param cause exception cause
     */
    public URLException(Throwable cause) {
        super(cause);
    }
    /**
     * Constructor.
     * @param message exception message
     * @param cause exception cause
     */
    public URLException(String message, Throwable cause) {
        super(message, cause);
    }
}
