/* Copyright 2018 Norconex Inc.
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
package com.norconex.commons.lang.xml;

/**
 * Provides indications that a class is configurable via XML.  Classes
 * implementing this should be careful to document XML configuration options
 * properly (e.g. in Javadoc).
 * @author Pascal Essiembre
 * @since 2.0.0 (Moved and modified from *.lang.config.IXMLConfigurable)
 */
public interface IXMLConfigurable {

    /**
     * Load XML configuration values and initialized this object with them.
     * @param xml the XML to load into this object
     */
    void loadFromXML(XML xml);

    /**
     * Saves this object as XML.
     * @param xml the XML that will representing this object
     */
    void saveToXML(XML xml);
}
