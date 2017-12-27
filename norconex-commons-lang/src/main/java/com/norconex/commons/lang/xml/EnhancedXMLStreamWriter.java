/* Copyright 2010-2017 Norconex Inc.
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

import java.awt.Dimension;
import java.io.Writer;
import java.util.Objects;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <p>
 * A version of {@link XMLStreamWriter} that adds convenience methods
 * for adding simple elements and typed attributes, as well as offering
 * pretty-printing.  Can be used on its own with a Writer, 
 * or as a wrapper to an existing <code>XMLStreamWriter</code> instance.
 * </p>
 * 
 * @author Pascal Essiembre
 * @since 1.5.0
 */
public class EnhancedXMLStreamWriter implements XMLStreamWriter {

    private static final Logger LOG = 
            LoggerFactory.getLogger(EnhancedXMLStreamWriter.class);
    
    private final XMLStreamWriter writer;
    //TODO consider constructor with new EnhancedXMLStreamWriterConfig instead
    private final boolean defaultWriteBlanks;
    // -1 = no indent, 0 = new lines only, 1+ = new lines + num of spaces,
    private final int indent;
    private int depth = 0;
    private boolean indentEnd = false; 

    public EnhancedXMLStreamWriter(Writer out) throws XMLStreamException {
        this(out, false);
    }
    /**
     * Creates a new xml stream writer.
     * @param out writer used to write XML
     * @param writeBlanks <code>true</code> to write attributes/elements 
     *        with no values when invoking methods without the 
     *        "writeBlanks" argument. This sets the default behavior which
     *        can be overwritten using methods with "writeBlanks" argument. 
     * @throws XMLStreamException problem creating XML stream writer
     */
    public EnhancedXMLStreamWriter(Writer out, boolean writeBlanks) 
            throws XMLStreamException {
        this(out, writeBlanks, -1);
    }
    /**
     * Creates a new xml stream writer.
     * @param out writer used to write XML
     * @param writeBlanks <code>true</code> to write attributes/elements 
     *        with no values when invoking methods without the 
     *        "writeBlanks" argument. This sets the default behavior which
     *        can be overwritten using methods with "writeBlanks" argument. 
     * @param indent how many spaces to use for indentation (-1=no indent; 
     *        0=newline only; 1+=number of spaces after newline)
     * @throws XMLStreamException problem creating XML stream writer
     * @since 1.13.0
     */
    public EnhancedXMLStreamWriter(Writer out, boolean writeBlanks, int indent) 
            throws XMLStreamException {
        super();
        XMLOutputFactory factory = createXMLOutputFactory();
        writer = factory.createXMLStreamWriter(out);
        this.defaultWriteBlanks = writeBlanks;
        this.indent = indent;
    }
    public EnhancedXMLStreamWriter(XMLStreamWriter xmlStreamWriter) {
        this(xmlStreamWriter, false);
    }
    public EnhancedXMLStreamWriter(
            XMLStreamWriter xmlStreamWriter, boolean writeBlanks) {
        this(xmlStreamWriter, writeBlanks, -1);
    }
    /**
     * Creates a new xml stream writer.
     * @param xmlStreamWriter wrapped stream writer
     * @param writeBlanks <code>true</code> to write attributes/elements 
     *        with no values
     * @param indent how many spaces to use for indentation (-1=no indent; 
     *        0=newline only; 1+=number of spaces after newline)
     * @since 1.13.0
     */
    public EnhancedXMLStreamWriter(
            XMLStreamWriter xmlStreamWriter, boolean writeBlanks, int indent) {
        super();
        this.writer = xmlStreamWriter;
        this.defaultWriteBlanks = writeBlanks;
        this.indent = indent;
    }

    private void indent() throws XMLStreamException { 
        indentEnd = true;
        if (indent > -1) {
            writer.writeCharacters("\n"); 
            if (indent > 0) {
                writer.writeCharacters(StringUtils.repeat(' ', depth * indent));
            }
        }
    }
    
    private static XMLOutputFactory createXMLOutputFactory() {
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        // If using Woodstox factory, disable structure validation
        // which can cause issues when you want to use the xml writer on 
        // a stream that already has XML written to it (could cause 
        // "multiple roots" error).
        if ("com.ctc.wstx.stax.WstxOutputFactory".equals(
                factory.getClass().getName())) {
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
    
    //--- Attribute methods ----------------------------------------------------

    public void writeAttributeInteger(String localName, Integer value) 
            throws XMLStreamException {
        writeAttributeInteger(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Integer attribute.
     * @param localName attribute name
     * @param value Integer attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */
    public void writeAttributeInteger(
            String localName, Integer value, boolean writeBlanks) 
            throws XMLStreamException {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeLong(String localName, Long value) 
            throws XMLStreamException {
        writeAttributeLong(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Long attribute.
     * @param localName attribute name
     * @param value Long attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */
    public void writeAttributeLong(
            String localName, Long value, boolean writeBlanks) 
            throws XMLStreamException {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeFloat(String localName, Float value) 
            throws XMLStreamException {
        writeAttributeFloat(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Float attribute.
     * @param localName attribute name
     * @param value Float attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */
    public void writeAttributeFloat(
            String localName, Float value, boolean writeBlanks) 
            throws XMLStreamException {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeDouble(String localName, Double value) 
            throws XMLStreamException {
        writeAttributeDouble(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Double attribute.
     * @param localName attribute name
     * @param value Double attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */
    public void writeAttributeDouble(
            String localName, Double value, boolean writeBlanks) 
            throws XMLStreamException {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeBoolean(String localName, Boolean value) 
            throws XMLStreamException {
        writeAttributeBoolean(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Boolean attribute.
     * @param localName attribute name
     * @param value Boolean attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */
    public void writeAttributeBoolean(
            String localName, Boolean value, boolean writeBlanks) 
            throws XMLStreamException {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeString(String localName, String value) 
            throws XMLStreamException {
        writeAttributeString(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a String attribute.
     * @param localName attribute name
     * @param value String attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */
    public void writeAttributeString(
            String localName, String value, boolean writeBlanks) 
            throws XMLStreamException {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeClass(String localName, Class<?> value) 
            throws XMLStreamException {
        writeAttributeClass(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes an attribute containing a class name, getting it by invoking
     * {@link Class#getCanonicalName()}. 
     * @param localName attribute name
     * @param value Class attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */
    public void writeAttributeClass(
            String localName, Class<?> value, boolean writeBlanks) 
            throws XMLStreamException {
        if (value == null) {
            writeAttributeObject(localName, null, writeBlanks);
        } else {
            writeAttributeObject(
                    localName, value.getCanonicalName(), writeBlanks);
        }
    }
    /**
     * Writes an attribute object by first converting it to string
     * using its "toString()" method.
     * @param localName attribute name
     * @param value attribute value
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */    
    public void writeAttributeObject(String localName, Object value)
            throws XMLStreamException {
        writeAttributeObject(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes an attribute object by first converting it to string
     * using its "toString()" method.
     * @param localName attribute name
     * @param value attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @throws XMLStreamException problem writing attribute
     * @since 1.14.0
     */    
    public void writeAttributeObject(
            String localName, Object value, boolean writeBlanks)
            throws XMLStreamException {
        String strValue = Objects.toString(value, null);
        if (StringUtils.isNotBlank(strValue)) {
            writeAttribute(localName, strValue);
        } else if (writeBlanks) {
            writeAttribute(localName, StringUtils.EMPTY);
        }
    }

    //--- Element methods ------------------------------------------------------
    
    public void writeElementInteger(String localName, Integer value) 
            throws XMLStreamException {
        writeElementInteger(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Integer element.
     * @param localName element (tag) name
     * @param value the Integer value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementInteger(
            String localName, Integer value, boolean writeBlanks) 
            throws XMLStreamException {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementLong(String localName, Long value) 
            throws XMLStreamException {
        writeElementLong(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Long element.
     * @param localName element (tag) name
     * @param value the Long value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementLong(
            String localName, Long value, boolean writeBlanks) 
            throws XMLStreamException {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementFloat(String localName, Float value) 
            throws XMLStreamException {
        writeElementFloat(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Float element.
     * @param localName element (tag) name
     * @param value the Float value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementFloat(
            String localName, Float value, boolean writeBlanks) 
            throws XMLStreamException {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementDouble(String localName, Double value) 
            throws XMLStreamException {
        writeElementDouble(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Double element.
     * @param localName element (tag) name
     * @param value the Double value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementDouble(
            String localName, Double value, boolean writeBlanks) 
            throws XMLStreamException {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementBoolean(String localName, Boolean value) 
            throws XMLStreamException {
        writeElementBoolean(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Boolean element.
     * @param localName element (tag) name
     * @param value the Boolean value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementBoolean(
            String localName, Boolean value, boolean writeBlanks) 
            throws XMLStreamException {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementString(String localName, String value) 
            throws XMLStreamException {
        writeElementString(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple string element.
     * @param localName element (tag) name
     * @param value the string value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementString(
            String localName, String value, boolean writeBlanks) 
            throws XMLStreamException {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementClass(String localName, Class<?> value) 
            throws XMLStreamException {
        writeElementClass(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple element containing a class name, getting it by invoking
     * {@link Class#getCanonicalName()}.
     * @param localName element (tag) name
     * @param value the class
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementClass(
            String localName, Class<?> value, boolean writeBlanks) 
            throws XMLStreamException {
        if (value == null) {
            writeElementObject(localName, null, writeBlanks);
        } else {
            writeElementObject(
                    localName, value.getCanonicalName(), writeBlanks);
        }
    }

    /**
     * Writes a simple element containing a Dimension.  The dimension
     * will be written as [width]x[height] (e.g., 400x300) or just one
     * numeric value if width and height are the same.
     * @param localName element (tag) name
     * @param value the dimension
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementDimension(String localName, Dimension value) 
            throws XMLStreamException {
        writeElementDimension(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple element containing a Dimension.  The dimension
     * will be written as [width]x[height] (e.g., 400x300) or just one
     * numeric value if width and height are the same.
     * @param localName element (tag) name
     * @param value the dimension
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementDimension(
            String localName, Dimension value, boolean writeBlanks) 
            throws XMLStreamException {
        if (value == null) {
            writeElementObject(localName, null, writeBlanks);
        } else {
            String str;
            if (value.getWidth() == value.getHeight()) {
                str = Integer.toString((int) value.getWidth());
            } else {
                str = Integer.toString((int) value.getWidth())
                        + "x" + Integer.toString((int) value.getHeight());
            }
            writeElementObject(localName, str, writeBlanks);
        }
    }
    /**
     * Writes a simple element object by first converting it to string
     * using its "toString()" method.
     * @param localName element (tag) name
     * @param value element (tag) value
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementObject(String localName, Object value)
            throws XMLStreamException {
        writeElementObject(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple element object by first converting it to string
     * using its "toString()" method.
     * @param localName element (tag) name
     * @param value element (tag) value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @throws XMLStreamException problem writing element
     * @since 1.14.0
     */
    public void writeElementObject(
            String localName, Object value, boolean writeBlanks)
            throws XMLStreamException {
        String strValue = Objects.toString(value, null);
        if (StringUtils.isNotBlank(strValue)) {
            writeStartElement(localName);
            writeCharacters(strValue);
            writeEndElement();
        } else if (writeBlanks) {
            writeEmptyElement(localName);
        }
    }
    
    //--- Overridden methods ---------------------------------------------------
    
    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        indent();
        depth++;
        writer.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName)
            throws XMLStreamException {
        indent();
        depth++;
        writer.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName,
            String namespaceURI) throws XMLStreamException {
        indent();
        depth++;
        writer.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName)
            throws XMLStreamException {
        indent();
        writer.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName,
            String namespaceURI) throws XMLStreamException {
        indent();
        writer.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        indent();
        writer.writeEmptyElement(localName);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        depth--;
        if (indentEnd) {
            indent();
        }
        indentEnd = true;
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
        indent();
        writer.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(String target)
            throws XMLStreamException {
        indent();
        writer.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data)
            throws XMLStreamException {
        indent();
        writer.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        writer.writeCData(data);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        indent();
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
        if (indent < 0) {
            writer.writeCharacters(text);
            return;
        }

        // We are indenting...
        if (StringUtils.isNotBlank(text)) {
            String[] lines = text.split("\n");
            if (lines.length == 1) {
                writer.writeCharacters(lines[0]);
                indentEnd = false;
            } else {
                for (String line : lines) {
                    indent();
                    writer.writeCharacters(line);
                }
            }
        } else {
            indentEnd = false;
        }
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
