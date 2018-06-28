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
package com.norconex.commons.lang.xml;

import java.awt.Dimension;
import java.io.Writer;
import java.util.Objects;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A version of {@link XMLStreamWriter} that adds convenience methods
 * for adding simple elements and typed attributes, as well as offering
 * pretty-printing.  Can be used on its own with a Writer, 
 * or as a wrapper to an existing <code>XMLStreamWriter</code> instance.
 * </p>
 * <p>
 * Since 2.0.0 checked exceptions are wrapped in an {@link XMLException}.
 * </p>
 * 
 * @author Pascal Essiembre
 * @since 1.5.0
 */
//TODO rename XMLWriter?  Or XmlStreamWriter?
//TODO rename writeAttributeXXX to writeAttrXXX ?
//TODO rename writeElementXXX to writeElemXXX ?
//TODO have XMLIn/XMLOut or XMLReader/XMLWriter instead?
public class EnhancedXMLStreamWriter implements XMLStreamWriter {

    private static final Logger LOG = 
            LoggerFactory.getLogger(EnhancedXMLStreamWriter.class);
    
    private final XMLStreamWriter writer;
    //TODO consider constructor with new EnhancedXMLStreamWriterConfig instead
    //TODO blanks should always be written???
    private final boolean defaultWriteBlanks;
    // -1 = no indent, 0 = new lines only, 1+ = new lines + num of spaces,
    private final int indent;
    private int depth = 0;
    private boolean indentEnd = false; 

    public EnhancedXMLStreamWriter(Writer out) {
        this(out, false);
    }
    /**
     * Creates a new xml stream writer.
     * @param out writer used to write XML
     * @param writeBlanks <code>true</code> to write attributes/elements 
     *        with no values when invoking methods without the 
     *        "writeBlanks" argument. This sets the default behavior which
     *        can be overwritten using methods with "writeBlanks" argument. 
     */
    public EnhancedXMLStreamWriter(Writer out, boolean writeBlanks) {
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
     * @since 1.13.0
     */
    public EnhancedXMLStreamWriter(
            Writer out, boolean writeBlanks, int indent) {
        super();
        try {
            XMLOutputFactory factory = createXMLOutputFactory();
            writer = factory.createXMLStreamWriter(out);
            this.defaultWriteBlanks = writeBlanks;
            this.indent = indent;
        } catch (XMLStreamException e) {
            throw new XMLException(
                    "Could not create EnhancedXMLStreamWriter.", e);
        }
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

    private void indent() { 
        try {
            indentEnd = true;
            if (indent > -1) {
                writer.writeCharacters("\n"); 
                if (indent > 0) {
                    writer.writeCharacters(
                            StringUtils.repeat(' ', depth * indent));
                }
            }
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write indent.", e);
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

    public void writeAttributeInteger(String localName, Integer value) {
        writeAttributeInteger(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Integer attribute.
     * @param localName attribute name
     * @param value Integer attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */
    public void writeAttributeInteger(
            String localName, Integer value, boolean writeBlanks) {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeLong(String localName, Long value) {
        writeAttributeLong(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Long attribute.
     * @param localName attribute name
     * @param value Long attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */
    public void writeAttributeLong(
            String localName, Long value, boolean writeBlanks) {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeFloat(String localName, Float value) {
        writeAttributeFloat(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Float attribute.
     * @param localName attribute name
     * @param value Float attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */
    public void writeAttributeFloat(
            String localName, Float value, boolean writeBlanks) {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeDouble(String localName, Double value) {
        writeAttributeDouble(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Double attribute.
     * @param localName attribute name
     * @param value Double attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */
    public void writeAttributeDouble(
            String localName, Double value, boolean writeBlanks) {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeBoolean(String localName, Boolean value) {
        writeAttributeBoolean(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a Boolean attribute.
     * @param localName attribute name
     * @param value Boolean attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */
    public void writeAttributeBoolean(
            String localName, Boolean value, boolean writeBlanks) {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeString(String localName, String value) {
        writeAttributeString(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a String attribute.
     * @param localName attribute name
     * @param value String attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */
    public void writeAttributeString(
            String localName, String value, boolean writeBlanks) {
        writeAttributeObject(localName, value, writeBlanks);
    }
    public void writeAttributeClass(String localName, Class<?> value) {
        writeAttributeClass(localName, value, defaultWriteBlanks);
    }

    /**
     * Write a "class" attribute with the value obtained from
     * getting it by invoking {@link Class#getCanonicalName()}.
     * @param value the class to write
     */
    public void writeAttributeClass(Class<?> value) {
        writeAttributeClass("class", value, defaultWriteBlanks);
    }
    /**
     * Writes an attribute containing a class name, getting it by invoking
     * {@link Class#getCanonicalName()}. 
     * @param localName attribute name
     * @param value Class attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */
    public void writeAttributeClass(
            String localName, Class<?> value, boolean writeBlanks) {
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
     * @since 1.14.0
     */    
    public void writeAttributeObject(String localName, Object value) {
        writeAttributeObject(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes an attribute object by first converting it to string
     * using its "toString()" method.
     * @param localName attribute name
     * @param value attribute value
     * @param writeBlanks whether a blank value should be written as 
     *                    an empty attribute.
     * @since 1.14.0
     */    
    public void writeAttributeObject(
            String localName, Object value, boolean writeBlanks) {
        String strValue = Objects.toString(value, null);
        if (StringUtils.isNotBlank(strValue)) {
            writeAttribute(localName, strValue);
        } else if (writeBlanks) {
            writeAttribute(localName, StringUtils.EMPTY);
        }
    }

    //--- Element methods ------------------------------------------------------
    
    public void writeElementInteger(String localName, Integer value) {
        writeElementInteger(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Integer element.
     * @param localName element (tag) name
     * @param value the Integer value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementInteger(
            String localName, Integer value, boolean writeBlanks) {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementLong(String localName, Long value) {
        writeElementLong(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Long element.
     * @param localName element (tag) name
     * @param value the Long value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementLong(
            String localName, Long value, boolean writeBlanks) {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementFloat(String localName, Float value) {
        writeElementFloat(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Float element.
     * @param localName element (tag) name
     * @param value the Float value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementFloat(
            String localName, Float value, boolean writeBlanks) {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementDouble(String localName, Double value) {
        writeElementDouble(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Double element.
     * @param localName element (tag) name
     * @param value the Double value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementDouble(
            String localName, Double value, boolean writeBlanks) {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementBoolean(String localName, Boolean value) {
        writeElementBoolean(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple Boolean element.
     * @param localName element (tag) name
     * @param value the Boolean value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementBoolean(
            String localName, Boolean value, boolean writeBlanks) {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementString(String localName, String value) {
        writeElementString(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple string element.
     * @param localName element (tag) name
     * @param value the string value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementString(
            String localName, String value, boolean writeBlanks) {
        writeElementObject(localName, value, writeBlanks);
    }
    public void writeElementClass(String localName, Class<?> value) {
        writeElementClass(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple element containing a class name, getting it by invoking
     * {@link Class#getCanonicalName()}.
     * @param localName element (tag) name
     * @param value the class
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementClass(
            String localName, Class<?> value, boolean writeBlanks) {
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
     * @since 1.14.0
     */
    public void writeElementDimension(String localName, Dimension value) {
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
     * @since 1.14.0
     */
    public void writeElementDimension(
            String localName, Dimension value, boolean writeBlanks) {
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
     * @since 1.14.0
     */
    public void writeElementObject(String localName, Object value) {
        writeElementObject(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes a simple element object by first converting it to string
     * using its "toString()" method.
     * @param localName element (tag) name
     * @param value element (tag) value
     * @param writeBlanks 
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     */
    public void writeElementObject(
            String localName, Object value, boolean writeBlanks) {
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
    public void writeStartElement(String localName) {
        indent();
        depth++;
        try {
            writer.writeStartElement(localName);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write start element.", e);
        }
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) {
        indent();
        depth++;
        try {
            writer.writeStartElement(namespaceURI, localName);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write start element.", e);
        }
    }

    @Override
    public void writeStartElement(
            String prefix, String localName, String namespaceURI) {
        indent();
        depth++;
        try {
            writer.writeStartElement(prefix, localName, namespaceURI);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write start element.", e);
        }
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) {
        indent();
        try {
            writer.writeEmptyElement(namespaceURI, localName);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write empty element.", e);
        }
    }

    @Override
    public void writeEmptyElement(
            String prefix, String localName, String namespaceURI) {
        indent();
        try {
            writer.writeEmptyElement(prefix, localName, namespaceURI);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write empty element.", e);
        }
    }

    @Override
    public void writeEmptyElement(String localName) {
        indent();
        try {
            writer.writeEmptyElement(localName);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write empty element.", e);
        }
    }

    @Override
    public void writeEndElement() {
        depth--;
        if (indentEnd) {
            indent();
        }
        indentEnd = true;
        try {
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write end attribute.", e);
        }
    }

    @Override
    public void writeEndDocument() {
        try {
            writer.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write end document.", e);
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not close.", e);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not flush.", e);
        }
    }

    @Override
    public void writeAttribute(String localName, String value) {
        try {
            writer.writeAttribute(localName, value);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write XML attribute.", e);
        }
    }
    @Override
    public void writeAttribute(String prefix, String namespaceURI,
            String localName, String value) {
        try {
            writer.writeAttribute(prefix, namespaceURI, localName, value);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write XML attribute.", e);
        }
    }
    @Override
    public void writeAttribute(
            String namespaceURI, String localName, String value) {
        try {
            writer.writeAttribute(namespaceURI, localName, value);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write XML attribute.", e);
        }
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) {
        try {
            writer.writeNamespace(prefix, namespaceURI);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write namespace.", e);
        }
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) {
        try {
            writer.writeDefaultNamespace(namespaceURI);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write default namespace.", e);
        }
    }

    @Override
    public void writeComment(String data) {
        indent();
        try {
            writer.writeComment(data);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write comment.", e);
        }
    }

    @Override
    public void writeProcessingInstruction(String target) {
        indent();
        try {
            writer.writeProcessingInstruction(target);
        } catch (XMLStreamException e) {
            throw new XMLException(
                    "Could not write processing instruction.", e);
        }
    }

    @Override
    public void writeProcessingInstruction(String target, String data) {
        indent();
        try {
            writer.writeProcessingInstruction(target, data);
        } catch (XMLStreamException e) {
            throw new XMLException(
                    "Could not write processing instruction.", e);
        }
    }

    @Override
    public void writeCData(String data) {
        try {
            writer.writeCData(data);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write CDATA.", e);
        }
    }

    @Override
    public void writeDTD(String dtd) {
        indent();
        try {
            writer.writeDTD(dtd);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write DTD.", e);
        }
    }

    @Override
    public void writeEntityRef(String name) {
        try {
            writer.writeEntityRef(name);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write entity ref.", e);
        }
    }

    @Override
    public void writeStartDocument() {
        try {
            writer.writeStartDocument();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write start document.", e);
        }
    }

    @Override
    public void writeStartDocument(String version) {
        try {
            writer.writeStartDocument(version);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write start document.", e);
        }
    }

    @Override
    public void writeStartDocument(String encoding, String version) {
        try {
            writer.writeStartDocument(encoding, version);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write start document.", e);
        }
    }

    @Override
    public void writeCharacters(String text) {
        try {
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
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write characters.", e);
        }
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) {
        try {
            writer.writeCharacters(text, start, len);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write characters.", e);
        }
    }

    @Override
    public String getPrefix(String uri) {
        try {
            return writer.getPrefix(uri);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not get prefix.", e);
        }
    }

    @Override
    public void setPrefix(String prefix, String uri) {
        try {
            writer.setPrefix(prefix, uri);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not set prefix.", e);
        }
    }

    @Override
    public void setDefaultNamespace(String uri) {
        try {
            writer.setDefaultNamespace(uri);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not set default namespace.", e);
        }
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) {
        try {
            writer.setNamespaceContext(context);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not set namespace context.", e);
        }
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
