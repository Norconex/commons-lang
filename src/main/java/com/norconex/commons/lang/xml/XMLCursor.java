/* Copyright 2021-2022 Norconex Inc.
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMResult;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.norconex.commons.lang.map.Properties;

/**
 * XML cursor referencing the current position or element in XML traversal.
 * @since 2.0.0
 * @see XML#iterator()
 * @see XML#iterator(Object)
 * @see XML#stream()
 * @see XML#stream(Object)
 */
public class XMLCursor {

    private final XMLEventReader reader;
    private final StartElement element;
    private final LinkedList<String> pathSegments;

    private boolean read;

    XMLCursor(
            XMLEventReader reader,
            StartElement element,
            LinkedList<String> pathSegments) {
        this.reader = reader;
        this.element = element;
        this.pathSegments = pathSegments;
    }

    public StartElement getElement() {
        return element;
    }
    public String getPath() {
        return '/' + String.join("/", pathSegments);
    }
    public String getLocalPath() {
        return getPath().replaceAll("(/)[^/]+?\\:", "$1");
    }

    public String getName() {
        return XMLUtil.toName(element.getName());
    }
    public String getLocalName() {
        return XMLUtil.toLocalName(element.getName());
    }

    public Properties getAttributes() {
        return doGetAttributes(XMLUtil::toName);
    }
    public Properties getLocalAttributes() {
        return doGetAttributes(XMLUtil::toLocalName);
    }
    private Properties doGetAttributes(Function<QName, String> f) {
        Properties props = new Properties();
        Iterator<Attribute> attrs = element.getAttributes();
        while (attrs.hasNext()) {
            Attribute a = attrs.next();
            props.add(f.apply(a.getName()), a.getValue());
        }
        return props;
    }

    // Advances the cursor to the next item, assumed to be text, an returns
    // it. Does not use "reader.getElementText()" as it consumes the
    // closing tag which we do not want.
    public String readText() {
        ensureNotRead();
        try {
            XMLEvent ev = reader.peek();
            if (ev != null && ev.isCharacters()) {
                read = true;
                return StringUtils.trim(
                        reader.nextEvent().asCharacters().getData());
            }
            return null;
        } catch (XMLStreamException e) {
            throw new XMLException("Could not read XML element text.", e);
        }
    }

    // will consume entire node and children
    public Node readAsDOM() {
        ensureNotRead();
        try {
            read = true;
            Document dom = XMLUtil.createDocumentBuilderFactory()
                    .newDocumentBuilder().newDocument();
            final XMLEventWriter writer = XMLOutputFactory.newInstance()
                    .createXMLEventWriter(new DOMResult(dom));
            writer.add(element);
            int depth = 1; // start element counts for one
            XMLEvent event;
            while (reader.hasNext()) {
                event = reader.peek();
                writer.add(event);
                if (event.isStartElement()) {
                  depth++;
                } else if (event.isEndElement()) {
                  depth--;
                }
                if (event.isEndElement() && depth <= 0) {
                    break;
                }
                reader.nextEvent(); // real read, sine we peeked.
            }
            return dom.getDocumentElement();
        } catch (ParserConfigurationException
                | XMLStreamException | FactoryConfigurationError e) {
            throw new XMLException(
                    "Could not convert cursor to XML object.", e);
        }
    }
    public XML readAsXML() {
        return new XML(readAsDOM());
    }

    private void ensureNotRead() {
        if (read) {
            throw new XMLException("XML cursor already read.");
        }
    }
}
