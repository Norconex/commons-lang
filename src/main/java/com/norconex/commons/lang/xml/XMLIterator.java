/* Copyright 2021 Norconex Inc.
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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * XML cursor referencing the current position or element in XML traversal.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
class XMLIterator implements Iterator<XMLCursor> {

    private final XMLEventReader reader;
    private final LinkedList<String> pathSegments = new LinkedList<>();

    XMLIterator(XMLEventReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {
        try {
            while (reader.hasNext()) {
                XMLEvent peekedEvent = reader.peek();
                // if next event is start, we return without consuming it
                if (peekedEvent.isStartElement()) {
                    return true;
                }
                // if next event is NOT start we consume it, and if it is a
                // closing element, we track that.
                if (peekedEvent.isEndElement()) {
                    pathSegments.removeLast();
                }
                reader.next();
            }
            reader.close();
            return false;
        } catch (XMLStreamException e) {
            throw new XMLException(
                    "Could not verify if there is a next element.", e);
        }
    }
    @Override
    public XMLCursor next() {
        try {
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();
                if (nextEvent.isStartElement()) {
                    pathSegments.addLast(XMLUtil.toName(
                            nextEvent.asStartElement().getName()));
                    return new XMLCursor(reader,
                            nextEvent.asStartElement(), pathSegments);
                }
                if (nextEvent.isEndElement()) {
                    pathSegments.removeLast();
                }
            }
            reader.close();
            return null;
        } catch (XMLStreamException e) {
            throw new XMLException("Could not get next XML element.", e);
        }
    }
}
