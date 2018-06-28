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
package com.norconex.commons.lang.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Provides indications that a class is configurable via XML.  Classes
 * implementing this should be careful to document XML configuration options
 * properly (e.g. in Javadoc).
 * @author Pascal Essiembre
 */
public interface IXMLConfigurable {

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
     * @deprecated Since 2.0.0, use {@link #saveToXML(Writer, String)}.
     */
    @Deprecated
    default void saveToXML(Writer out) throws IOException {
        //NOOP
    };
    
    /**
     * Saves this object as XML.
     * @param out XML writer
     * @param elementName XML element name
     * @throws IOException something went wrong writing the XML
     */
    default void saveToXML(Writer out, String elementName) throws IOException {
        saveToXML(out);
    }
}
