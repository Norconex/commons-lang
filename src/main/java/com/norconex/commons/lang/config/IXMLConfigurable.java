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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.w3c.dom.Element;

import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.XMLException;

/**
 * Provides indications that a class is configurable via XML.  Classes
 * implementing this should be careful to document XML configuration options
 * properly (e.g. in Javadoc).
 * @author Pascal Essiembre
 * @deprecated Since 2.0.0, use
 *             {@link com.norconex.commons.lang.xml.IXMLConfigurable}.
 */
@Deprecated
public interface IXMLConfigurable extends com.norconex.commons.lang.xml.IXMLConfigurable {

    /**
     * Load XML configuration values and initialized this object with them.
     * @param reader XML input stream
     * @throws IOException something went wrong reading the XML
     */
    @Deprecated
    default void loadFromXML(Reader reader) throws IOException {
        //NOOP
    }

    /**
     * Load XML configuration values and initialized this object with them.
     * @param xml the XML to load into this object
     */
    @Override
    default void loadFromXML(XML xml) {
        try {
            loadFromXML(new StringReader(xml.toString()));
        } catch (IOException e) {
            throw new XMLException("Could not load from XML.", e);
        }
    }

    /**
     * Saves this object as XML.
     * @param writer XML writer
     * @throws IOException something went wrong writing the XML
     */
    @Deprecated
    default void saveToXML(Writer writer) throws IOException {
        //NOOP
    }

    /**
     * Saves this object as XML.
     * @param xml the XML that will representing this object
     */
    @Override
    default void saveToXML(XML xml) {
        try {
            StringWriter w = new StringWriter();
            saveToXML(w);
            xml.addXML(w.toString());
            Element node = (Element) xml.getNode();
            Element childNode = (Element) node.getFirstChild();
            if (childNode != null
                    && childNode.getNodeName().equals(node.getNodeName())) {
                xml.unwrap();
            }
        } catch (IOException e) {
            throw new XMLException("Could not save to XML.", e);
        }
    }
}
