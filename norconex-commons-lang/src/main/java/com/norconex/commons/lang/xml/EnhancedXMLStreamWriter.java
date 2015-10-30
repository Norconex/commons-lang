/* Copyright 2010-2015 Norconex Inc.
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

import java.io.Writer;
import java.util.Objects;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * <p>
 * A version of {@link XMLStreamWriter} that adds convenience methods
 * for adding simple elements and typed attributes.
 * </p>
 * 
 * @author Pascal Essiembre
 * @since 1.5.0
 */
public class EnhancedXMLStreamWriter implements XMLStreamWriter {

    private static final Logger LOG = 
            LogManager.getLogger(EnhancedXMLStreamWriter.class);
    
    private final XMLStreamWriter writer;
    private final boolean writeBlanks;

    public EnhancedXMLStreamWriter(Writer out) throws XMLStreamException {
        this(out, false);
    }
    public EnhancedXMLStreamWriter(Writer out, boolean writeBlanks) 
            throws XMLStreamException {
        super();
        XMLOutputFactory factory = createXMLOutputFactory();
        writer = factory.createXMLStreamWriter(out);
        this.writeBlanks = writeBlanks;
    }
    public EnhancedXMLStreamWriter(XMLStreamWriter xmlStreamWriter) {
        this(xmlStreamWriter, false);
    }
    public EnhancedXMLStreamWriter(
            XMLStreamWriter xmlStreamWriter, boolean writeBlanks) {
        super();
        this.writer = xmlStreamWriter;
        this.writeBlanks = writeBlanks;
    }

    private static XMLOutputFactory createXMLOutputFactory() {
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        // If using Woodstox factory, disable structure validation
        // which can cause issues when you want to use the xml writer on 
        // a stream that already has XML written to it (could cause 
        // "multiple roots" error).
        if (factory.getClass().getName().equals(
                "com.ctc.wstx.stax.WstxOutputFactory")) {
            try {
                Object config = factory.getClass().getMethod(
                        "getConfig").invoke(factory);
                config.getClass().getMethod(
                        "doValidateStructure", boolean.class).invoke(
                                config, false);
            } catch (Exception e) {
                LOG.warn("Could not disable structure validation on "
                        + "WstxOutputFactory. This can cause issues when "
                        + "using EnhancedXMLStreamWriter on an partially "
                        + "written XML stream (\"multiple roots\" error).");
            }
        }
        return factory;
    }
    
    //--- New methods ----------------------------------------------------------

    public void writeAttributeInteger(String localName, Integer value) 
            throws XMLStreamException {
        writeAttributeObject(localName, value);
    }
    public void writeAttributeLong(String localName, Long value) 
            throws XMLStreamException {
        writeAttributeObject(localName, value);
    }
    public void writeAttributeFloat(String localName, Float value) 
            throws XMLStreamException {
        writeAttributeObject(localName, value);
    }
    public void writeAttributeDouble(String localName, Double value) 
            throws XMLStreamException {
        writeAttributeObject(localName, value);
    }
    public void writeAttributeBoolean(String localName, Boolean value) 
            throws XMLStreamException {
        writeAttributeObject(localName, value);
    }
    public void writeAttributeString(String localName, String value) 
            throws XMLStreamException {
        writeAttributeObject(localName, value);
    }
    public void writeAttributeClass(String localName, Class<?> value) 
            throws XMLStreamException {
        if (value == null) {
            writeAttributeObject(localName, null);
        } else {
            writeAttributeObject(localName, value.getCanonicalName());
        }
    }
    private void writeAttributeObject(String localName, Object value)
            throws XMLStreamException {
        String strValue = Objects.toString(value, null);
        if (StringUtils.isNotBlank(strValue)) {
            writeAttribute(localName, strValue);
        } else if (writeBlanks) {
            writeAttribute(localName, StringUtils.EMPTY);
        }
    }
    
    public void writeElementInteger(String localName, Integer value) 
            throws XMLStreamException {
        writeElementObject(localName, value);
    }
    public void writeElementLong(String localName, Long value) 
            throws XMLStreamException {
        writeElementObject(localName, value);
    }
    public void writeElementFloat(String localName, Float value) 
            throws XMLStreamException {
        writeElementObject(localName, value);
    }
    public void writeElementDouble(String localName, Double value) 
            throws XMLStreamException {
        writeElementObject(localName, value);
    }
    public void writeElementBoolean(String localName, Boolean value) 
            throws XMLStreamException {
        writeElementObject(localName, value);
    }
    public void writeElementString(String localName, String value) 
            throws XMLStreamException {
        writeElementObject(localName, value);
    }
    public void writeElementClass(String localName, Class<?> value) 
            throws XMLStreamException {
        if (value == null) {
            writeElementObject(localName, null);
        } else {
            writeElementObject(localName, value.getCanonicalName());
        }
    }
    private void writeElementObject(String localName, Object value)
            throws XMLStreamException {
        String strValue = Objects.toString(value, null);
        if (StringUtils.isNotBlank(strValue)) {
            writer.writeStartElement(localName);
            writer.writeCharacters(strValue);
            writer.writeEndElement();
        } else if (writeBlanks) {
            writer.writeEmptyElement(localName);
        }
    }
    
    //--- Overridden methods ---------------------------------------------------
    
    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        writer.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName)
            throws XMLStreamException {
        writer.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName,
            String namespaceURI) throws XMLStreamException {
        writer.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName)
            throws XMLStreamException {
        writer.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName,
            String namespaceURI) throws XMLStreamException {
        writer.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        writer.writeEmptyElement(localName);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        writer.writeEndElement();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        writer.writeEndDocument();
    }

    @Override
    public void close() throws XMLStreamException {
        writer.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        writer.flush();
    }

    @Override
    public void writeAttribute(String localName, String value)
            throws XMLStreamException {
        writer.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI,
            String localName, String value) throws XMLStreamException {
        writer.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName,
            String value) throws XMLStreamException {
        writeAttribute(namespaceURI, value);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI)
            throws XMLStreamException {
        writer.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI)
            throws XMLStreamException {
        writer.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        writer.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(String target)
            throws XMLStreamException {
        writer.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data)
            throws XMLStreamException {
        writer.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        writer.writeCData(data);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        writer.writeDTD(dtd);
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        writer.writeEntityRef(name);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        writer.writeStartDocument();
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        writer.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument(String encoding, String version)
            throws XMLStreamException {
        writer.writeStartDocument(encoding, version);
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        writer.writeCharacters(text);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len)
            throws XMLStreamException {
        writer.writeCharacters(text, start, len);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return writer.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        writer.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        writer.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context)
            throws XMLStreamException {
        writer.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return writer.getNamespaceContext();
    }

    @Override
    public Object getProperty(String name) {
        return writer.getProperty(name);
    }

    
}
