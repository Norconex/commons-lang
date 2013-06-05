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
package com.norconex.commons.lang.io;

/**
 * Listener that is being notified every time a line is processed from a 
 * given stream.
 * @author Pascal Essiembre
 * @see StreamGobbler
 */
public interface IStreamListener {
    
    /**
     * Invoked when a line is streamed.
     * @param type type of line, as defined by the class using the listener
     * @param line line processed
     */
    void lineStreamed(String type, String line);
}
