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
package com.norconex.commons.lang.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

/**
 * Provides indications that a class is configurable via XML.  Classes
 * implementing this should be careful to document XML configuration options
 * properly (e.g. in Javadoc).
 * @author Pascal Essiembre
 */
public interface IXMLConfigurable extends Serializable {

    /**
     * Load XML configuration values and initialized this object with them.
     * @param in XML input stream
     * @throws IOException something went wrong reading the XML
     */
    void loadFromXML(Reader in) throws IOException;
    
    /**
     * Saves this object as XML.
     * @param out XML writer
     * @throws IOException something went wrong writing the XML
     */
    void saveToXML(Writer out) throws IOException;
}
