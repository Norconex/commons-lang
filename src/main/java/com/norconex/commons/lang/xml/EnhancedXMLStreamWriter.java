/* Copyright 2010-2022 Norconex Inc.
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
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import com.ctc.wstx.util.EmptyNamespaceContext;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * A version of {@link XMLStreamWriter} that adds convenience methods
 * for adding simple elements and typed attributes, as well as offering
 * pretty-printing.  Can be used on its own with a Writer,
 * or as a wrapper to an existing <code>XMLStreamWriter</code> instance.
 * Also support XML with no root.
 * </p>
 * <p>
 * Since 2.0.0 checked exceptions are wrapped in an {@link XMLException}.
 * </p>
 *
 * @since 1.5.0
 * @deprecated Supporting XML with no root is not always stable, to do
 *   so, consider using {@link XML}. For stream-writing, use regular
 *   {@link XMLStreamWriter}.
 */
@Deprecated(since = "3.0.0")
@Slf4j
public class EnhancedXMLStreamWriter implements XMLStreamWriter {

    private static final String ATTR_DISABLED = "disabled";
    private static final String ERR_START_ELEM =
            "Could not write start element.";
    private static final String ERR_EMPTY_ELEM =
            "Could not write empty element.";
    private static final String ERR_ATTR = "Could not write XML attribute.";
    private static final String ERR_START_DOC =
            "Could not write start document.";

    private final XMLStreamWriter streamWriter;
    private final Writer writer;

    private final boolean defaultWriteBlanks;
    // -1 = no indent, 0 = new lines only, 1+ = new lines + num of spaces,
    private final int indent;
    private int depth = 0;
    private boolean indentEnd = false;

    public EnhancedXMLStreamWriter(Writer writer) {
        this(writer, false);
    }
    /**
     * Creates a new xml stream writer.
     * @param writer writer used to write XML
     * @param writeBlanks <code>true</code> to write attributes/elements
     *        with no values when invoking methods without the
     *        "writeBlanks" argument. This sets the default behavior which
     *        can be overwritten using methods with "writeBlanks" argument.
     */
    public EnhancedXMLStreamWriter(Writer writer, boolean writeBlanks) {
        this(writer, writeBlanks, -1);
    }
    /**
     * Creates a new xml stream writer.
     * @param writer writer used to write XML
     * @param writeBlanks <code>true</code> to write attributes/elements
     *        with no values when invoking methods without the
     *        "writeBlanks" argument. This sets the default behavior which
     *        can be overwritten using methods with "writeBlanks" argument.
     * @param indent how many spaces to use for indentation (-1=no indent;
     *        0=newline only; 1+=number of spaces after newline)
     * @since 1.13.0
     */
    public EnhancedXMLStreamWriter(
            Writer writer, boolean writeBlanks, int indent) {
        try {
            var factory = createXMLOutputFactory();
            this.writer = writer;
            streamWriter = factory.createXMLStreamWriter(writer);
            defaultWriteBlanks = writeBlanks;
            this.indent = indent;
        } catch (XMLStreamException e) {
            throw new XMLException(
                    "Could not create EnhancedXMLStreamWriter.", e);
        }
    }

    /**
     * Gets the underlying writer, after flushing this XML stream writer.
     * @return the writer
     * @since 2.0.0
     */
    public Writer getWriter() {
        flush();
        return writer;
    }

    //--- Attribute methods ----------------------------------------------------

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
    public void writeAttributeInteger(String localName, Integer value) {
        writeAttributeInteger(localName, value, defaultWriteBlanks);
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
    public void writeAttributeLong(String localName, Long value) {
        writeAttributeLong(localName, value, defaultWriteBlanks);
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
    public void writeAttributeFloat(String localName, Float value) {
        writeAttributeFloat(localName, value, defaultWriteBlanks);
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
    public void writeAttributeDouble(String localName, Double value) {
        writeAttributeDouble(localName, value, defaultWriteBlanks);
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
    public void writeAttributeBoolean(String localName, Boolean value) {
        writeAttributeBoolean(localName, value, defaultWriteBlanks);
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
    public void writeAttributeString(String localName, String value) {
        writeAttributeString(localName, value, defaultWriteBlanks);
    }

    /**
     * Write a "class" attribute with the value obtained from
     * getting it by invoking {@link Class#getCanonicalName()}.
     * @param value the class to write
     * @param writeBlanks whether a blank value should be written as
     *                    an empty attribute.
     * @since 2.0.0
     */
    public void writeAttributeClass(Class<?> value, boolean writeBlanks) {
        writeAttributeClass("class", value, writeBlanks);
    }
    /**
     * Write a "class" attribute with the value obtained from
     * getting it by invoking {@link Class#getCanonicalName()}.
     * @param value the class to write
     * @since 2.0.0
     */
    public void writeAttributeClass(Class<?> value) {
        writeAttributeClass(value, defaultWriteBlanks);
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
    public void writeAttributeClass(String localName, Class<?> value) {
        writeAttributeClass(localName, value, defaultWriteBlanks);
    }

    /**
     * Write a "disabled" attribute.
     * @param value <code>true</code> or <code>false</code>
     * @param writeBlanks whether a blank value should be written as
     *                    an empty attribute.
     * @since 2.0.0
     */
    public void writeAttributeDisabled(Boolean value, boolean writeBlanks) {
        writeAttributeBoolean(ATTR_DISABLED, value, writeBlanks);
    }
    /**
     * Write a "disabled" attribute.
     * @param value <code>true</code> or <code>false</code>
     * @since 2.0.0
     */
    public void writeAttributeDisabled(boolean value) {
        writeAttributeBoolean(ATTR_DISABLED, value, defaultWriteBlanks);
    }
    /**
     * Write a "disabled" attribute set to <code>true</code>.
     * @since 2.0.0
     */
    public void writeAttributeDisabled() {
        writeAttributeBoolean(ATTR_DISABLED, true, defaultWriteBlanks);
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
        var strValue = Objects.toString(value, null);
        if (StringUtils.isNotBlank(strValue)) {
            writeAttribute(localName, strValue);
        } else if (writeBlanks) {
            writeAttribute(localName, StringUtils.EMPTY);
        }
    }

    /**
     * Writes an attribute by converting a list into a comma-separated
     * string.
     * @param localName attribute name
     * @param values the values to write
     * @since 2.0.0
     */
    public void writeAttributeDelimited(
            String localName, List<?> values) {
        writeAttributeDelimited(localName, values, ",");
    }
    /**
     * Writes an attribute by converting a list into a comma-separated
     * string.
     * @param localName attribute name
     * @param values the values to write
     * @param delimiter the string separating each values
     * @since 2.0.0
     */
    public void writeAttributeDelimited(
            String localName, List<?> values, String delimiter) {
        writeAttributeString(localName, StringUtils.join(values, delimiter));
    }


    //--- Element methods ------------------------------------------------------

    /**
     * Writes an empty element with a "disabled" attribute set to
     * <code>true</code>.
     * @param localName element (tag) name
     * @since 2.0.0
     */
    public void writeElementDisabled(String localName) {
        writeStartElement(localName);
        writeAttributeBoolean(ATTR_DISABLED, true);
        writeEndElement();
    }

    /**
     * Writes an empty element with a "disabled" attribute set to
     * <code>true</code> and a "class" attribute matching the class name.
     * @param localName element (tag) name
     * @param clazz the class
     * @since 2.0.0
     */
    public void writeElementDisabled(String localName, Class<?> clazz) {
        writeStartElement(localName);
        writeAttributeClass(clazz);
        writeAttributeBoolean(ATTR_DISABLED, true);
        writeEndElement();
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
    public void writeElementInteger(String localName, Integer value) {
        writeElementInteger(localName, value, defaultWriteBlanks);
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
    public void writeElementLong(String localName, Long value) {
        writeElementLong(localName, value, defaultWriteBlanks);
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
    public void writeElementFloat(String localName, Float value) {
        writeElementFloat(localName, value, defaultWriteBlanks);
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
    public void writeElementDouble(String localName, Double value) {
        writeElementDouble(localName, value, defaultWriteBlanks);
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
    public void writeElementBoolean(String localName, Boolean value) {
        writeElementBoolean(localName, value, defaultWriteBlanks);
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
    public void writeElementString(String localName, String value) {
        writeElementString(localName, value, defaultWriteBlanks);
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
    public void writeElementClass(String localName, Class<?> value) {
        writeElementClass(localName, value, defaultWriteBlanks);
    }

    /**
     * Writes a {@link File} element.
     * @param localName element (tag) name
     * @param value the File
     * @param writeBlanks
     *        whether a blank value should be written as an empty element.
     * @since 2.0.0
     */
    public void writeElementFile(
            String localName, File value, boolean writeBlanks) {
        writeElementObject(localName, value, writeBlanks);
    }
    /**
     * Writes a {@link File} element.
     * @param localName element (tag) name
     * @param value the File
     * @since 2.0.0
     */
    public void writeElementFile(String localName, File value) {
        writeElementFile(localName, value, defaultWriteBlanks);
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
     * Writes a list of objects.
     * @param parentLocalName parent tag wrapping child elements
     *        (set to <code>null</code> for no parent)
     * @param localName element (tag) name
     * @param values the list
     * @param writeBlanks
     *        whether a blank value should be written as an empty element.
     * @since 2.0.0
     */
    public void writeElementObjectList(String parentLocalName,
            String localName, List<?> values, boolean writeBlanks) {
        var hasParent = StringUtils.isNotBlank(parentLocalName);
        if (CollectionUtils.isEmpty(values) && hasParent) {
            writeElementObject(parentLocalName, null, writeBlanks);
        } else {
            if (hasParent) {
                writeStartElement(parentLocalName);
            }
            for (Object value : values) {
                writeElementObject(localName, value, writeBlanks);
            }
            if (hasParent) {
                writeEndElement();
            }
        }
    }
    /**
     * Writes a list of objects.
     * @param parentLocalName parent tag wrapping child elements
     *        (set to <code>null</code> for no parent)
     * @param localName element (tag) name
     * @param values the list
     * @since 2.0.0
     */
    public void writeElementObjectList(String parentLocalName,
            String localName, List<?> values) {
        writeElementObjectList(
                parentLocalName, localName, values, defaultWriteBlanks);
    }

    /**
     * Writes an element object as string.
     * The object is converted to string using its
     * "toString()" method and written as a characters into a single element.
     * @param localName element (tag) name
     * @param value element (tag) value
     * @since 1.14.0
     * @see #writeObject(String, Object)
     */
    public void writeElementObject(String localName, Object value) {
        writeElementObject(localName, value, defaultWriteBlanks);
    }
    /**
     * Writes an element object as string.
     * The object is converted to string using its
     * "toString()" method and written as a characters into a single element.
     * @param localName element (tag) name
     * @param value element (tag) value
     * @param writeBlanks
     *        whether a blank value should be written as an empty element.
     * @since 1.14.0
     * @see #writeObject(String, Object)
     */
    public void writeElementObject(
            String localName, Object value, boolean writeBlanks) {
        var strValue = Objects.toString(value, null);
        if (StringUtils.isNotBlank(strValue)) {
            writeStartElement(localName);
            writeCharacters(strValue);
            writeEndElement();
        } else if (writeBlanks) {
            writeEmptyElement(localName);
        }
    }

    /**
     * Writes an element by converting a list into a comma-separated
     * string.
     * @param localName element name
     * @param values the values to write
     * @since 2.0.0
     */
    public void writeElementDelimited(
            String localName, List<?> values) {
        writeElementDelimited(localName, values, ",");
    }
    /**
     * Writes an element by converting a list into a comma-separated
     * string.
     * @param localName element name
     * @param values the values to write
     * @param delimiter the string separating each values
     * @since 2.0.0
     */
    public void writeElementDelimited(
            String localName, List<?> values, String delimiter) {
        writeElementString(localName, StringUtils.join(values, delimiter));
    }

    /**
     * Writes an object. If the object implements {@link XMLConfigurable},
     * its is responsible for creating its own XML through its
     * {@link XMLConfigurable#saveToXML(XML)} method.
     * Otherwise, an empty element is created with the "class" attribute
     * matching the object class canonical name.
     * @param localName element (tag) name
     * @param value the object to write
     * @since 2.0.0
     * @see #writeElementObject(String, Object)
     */
    public void writeObject(String localName, Object value) {
        writeObject(localName, value, false);
    }
    /**
     * Writes an object. If the object implements {@link XMLConfigurable},
     * its is responsible for creating its own XML through its
     * {@link XMLConfigurable#saveToXML(XML)} method.
     * Otherwise, an empty element is created with the "class" attribute
     * matching the object class canonical name.
     * @param localName element (tag) name
     * @param value the object to write
     * @param disabled whether the object should have the "diabled" attribute
     * @since 2.0.0
     * @see #writeElementObject(String, Object)
     */
    public void writeObject(String localName, Object value, boolean disabled) {
        if (value == null) {
            if (disabled) {
                writeElementDisabled(localName);
            }
            return;
        }
        if (value instanceof XMLConfigurable) {
            flush();
            writeStartElement(localName);
            if (disabled) {
                writeAttributeBoolean(ATTR_DISABLED, disabled);
            }
            var xml = XML.of(localName, value).create();
            setAttributesFromNode(xml.getNode());
            writeCharacters("");
            flush();
            var xmlList = xml.getXMLList("*");
            if (xmlList.isEmpty()) {
                write(xml.getString("."), localName);
            } else {
                xmlList.forEach(x -> write(x.toString(indent), localName));
            }
            writeEndElement();
            flush();
        } else {
            writeStartElement(localName);
            writeAttributeClass(value.getClass());
            writeEndElement();
        }
    }

    /**
     * Writes a list of objects.
     * If an object implements {@link XMLConfigurable},
     * its is responsible for creating its own XML through its
     * {@link XMLConfigurable#saveToXML(XML)} method.
     * Otherwise, an empty element is created with the "class" attribute
     * matching the object class canonical name.
     * @param parentLocalName parent tag wrapping child elements
     *        (set to <code>null</code> for no parent)
     * @param localName element (tag) name
     * @param values the objects to write
     * @since 2.0.0
     * @see #writeElementObjectList(String, String, List)
     */
    public void writeObjectList(String parentLocalName,
            String localName, List<? extends Object> values) {

        var hasParent = StringUtils.isNotBlank(parentLocalName);
        if (CollectionUtils.isEmpty(values) && hasParent) {
            writeElementObject(parentLocalName, null, defaultWriteBlanks);
            return;
        }
        if (hasParent) {
            writeStartElement(parentLocalName);
        }
        for (Object value : values) {
            writeObject(localName, value);
        }
        if (hasParent) {
            writeEndElement();
        }
    }


    //--- Overridden methods ---------------------------------------------------

    @Override
    public void writeStartElement(String localName) {
        indent();
        depth++;
        try {
            streamWriter.writeStartElement(localName);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_START_ELEM, e);
        }
    }

    /**
     * Writes a start element of the supplied name, with a "class" attribute
     * for the given class.
     * @param localName tag name
     * @param clazz name of class, or <code>null</code>
     * @since 2.0.0
     */
    public void writeStartElement(String localName, Class<?> clazz) {
        writeStartElement(localName);
        writeAttributeClass(clazz);
    }

    /**
     * Writes a start element of the supplied name, with a "class" attribute
     * for the given class, and a "disabled" attribute if <code>true</code>.
     * @param localName tag name
     * @param clazz name of class, or <code>null</code>
     * @param disabled <code>true</code> to disable this class
     * @since 2.0.0
     */
    public void writeStartElement(
            String localName, Class<?> clazz, boolean disabled) {
        writeStartElement(localName);
        writeAttributeClass(clazz);
        writeAttributeDisabled(disabled);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) {
        indent();
        depth++;
        try {
            streamWriter.writeStartElement(namespaceURI, localName);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_START_ELEM, e);
        }
    }

    @Override
    public void writeStartElement(
            String prefix, String localName, String namespaceURI) {
        indent();
        depth++;
        try {
            streamWriter.writeStartElement(prefix, localName, namespaceURI);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_START_ELEM, e);
        }
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) {
        indent();
        try {
            // Because we support badly structured XML with no parent,
            // we have to write empty elements this way to avoid issues
            // in some underlying implementations.
            streamWriter.writeStartElement(namespaceURI, localName);
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_EMPTY_ELEM, e);
        }
    }

    @Override
    public void writeEmptyElement(
            String prefix, String localName, String namespaceURI) {
        indent();
        try {
            // Because we support badly structured XML with no parent,
            // we have to write empty elements this way to avoid issues
            // in some underlying implementations.
            streamWriter.writeStartElement(prefix, localName, namespaceURI);
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_EMPTY_ELEM, e);
        }
    }

    @Override
    public void writeEmptyElement(String localName) {
        indent();
        try {
            // Because we support badly structured XML with no parent,
            // we have to write empty elements this way to avoid issues
            // in some underlying implementations.
            streamWriter.writeStartElement(localName);
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_EMPTY_ELEM, e);
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
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write end element.", e);
        }
    }

    @Override
    public void writeEndDocument() {
        try {
            streamWriter.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write end document.", e);
        }
    }

    @Override
    public void close() {
        try {
            streamWriter.close();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not close.", e);
        }
    }

    @Override
    public void flush() {
        try {
            streamWriter.flush();
        } catch (XMLStreamException e) {
            throw new XMLException("Could not flush.", e);
        }
    }

    @Override
    public void writeAttribute(String localName, String value) {
        try {
            streamWriter.writeAttribute(localName, value);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_ATTR, e);
        }
    }
    @Override
    public void writeAttribute(String prefix, String namespaceURI,
            String localName, String value) {
        try {
            streamWriter.writeAttribute(prefix, namespaceURI, localName, value);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_ATTR, e);
        }
    }
    @Override
    public void writeAttribute(
            String namespaceURI, String localName, String value) {
        try {
            streamWriter.writeAttribute(namespaceURI, localName, value);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_ATTR, e);
        }
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) {
        try {
            streamWriter.writeNamespace(prefix, namespaceURI);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write namespace.", e);
        }
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) {
        try {
            streamWriter.writeDefaultNamespace(namespaceURI);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write default namespace.", e);
        }
    }

    @Override
    public void writeComment(String data) {
        indent();
        try {
            streamWriter.writeComment(data);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write comment.", e);
        }
    }

    @Override
    public void writeProcessingInstruction(String target) {
        indent();
        try {
            streamWriter.writeProcessingInstruction(target);
        } catch (XMLStreamException e) {
            throw new XMLException(
                    "Could not write processing instruction.", e);
        }
    }

    @Override
    public void writeProcessingInstruction(String target, String data) {
        indent();
        try {
            streamWriter.writeProcessingInstruction(target, data);
        } catch (XMLStreamException e) {
            throw new XMLException(
                    "Could not write processing instruction.", e);
        }
    }

    @Override
    public void writeCData(String data) {
        try {
            streamWriter.writeCData(data);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write CDATA.", e);
        }
    }

    @Override
    public void writeDTD(String dtd) {
        indent();
        try {
            streamWriter.writeDTD(dtd);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write DTD.", e);
        }
    }

    @Override
    public void writeEntityRef(String name) {
        try {
            streamWriter.writeEntityRef(name);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write entity ref.", e);
        }
    }

    @Override
    public void writeStartDocument() {
        try {
            streamWriter.writeStartDocument();
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_START_DOC, e);
        }
    }

    @Override
    public void writeStartDocument(String version) {
        try {
            streamWriter.writeStartDocument(version);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_START_DOC, e);
        }
    }

    @Override
    public void writeStartDocument(String encoding, String version) {
        try {
            streamWriter.writeStartDocument(encoding, version);
        } catch (XMLStreamException e) {
            throw new XMLException(ERR_START_DOC, e);
        }
    }

    @Override
    public void writeCharacters(String text) {
        try {
            if (indent < 0) {
                streamWriter.writeCharacters(text);
                return;
            }

            // We are indenting...
            if (StringUtils.isNotBlank(text)) {
                var lines = text.split("\n");
                if (lines.length == 1) {
                    streamWriter.writeCharacters(lines[0]);
                    indentEnd = false;
                } else {
                    for (String line : lines) {
                        indent();
                        streamWriter.writeCharacters(line);
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
            streamWriter.writeCharacters(text, start, len);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write characters.", e);
        }
    }

    @Override
    public String getPrefix(String uri) {
        try {
            return streamWriter.getPrefix(uri);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not get prefix.", e);
        }
    }

    @Override
    public void setPrefix(String prefix, String uri) {
        try {
            streamWriter.setPrefix(prefix, uri);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not set prefix.", e);
        }
    }

    @Override
    public void setDefaultNamespace(String uri) {
        try {
            streamWriter.setDefaultNamespace(uri);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not set default namespace.", e);
        }
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) {
        try {

            streamWriter.setNamespaceContext(context == null
                    ? EmptyNamespaceContext.getInstance() : context);
        } catch (XMLStreamException e) {
            throw new XMLException("Could not set namespace context.", e);
        }
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return streamWriter.getNamespaceContext();
    }

    @Override
    public Object getProperty(String name) {
        return streamWriter.getProperty(name);
    }

    //--- Private methods ------------------------------------------------------

    private void indent() {
        try {
            indentEnd = true;
            if (indent > -1) {
                streamWriter.writeCharacters("\n");
                if (indent > 0) {
                    streamWriter.writeCharacters(
                            StringUtils.repeat(' ', depth * indent));
                }
            }
        } catch (XMLStreamException e) {
            throw new XMLException("Could not write indent.", e);
        }
    }

    private static XMLOutputFactory createXMLOutputFactory() {
        var factory = XMLOutputFactory.newFactory();

        // If using Woodstox factory, disable structure validation
        // which can cause issues when you want to use the xml writer on
        // a stream that already has XML written to it (could cause
        // "multiple roots" error).
        if ("com.ctc.wstx.stax.WstxOutputFactory".equals( //NOSONAR
                factory.getClass().getName())) { //NOSONAR
            try {

                var config = factory.getClass().getMethod(
                        "getConfig").invoke(factory);
                config.getClass().getMethod(
                        "doValidateStructure", boolean.class).invoke(
                                config, false);
            } catch (Exception e) {
                LOG.warn("""
                    Could not disable structure validation on\s\
                    WstxOutputFactory. This can cause issues when\s\
                    using EnhancedXMLStreamWriter on an partially\s\
                    written XML stream ("multiple roots" error).""");
            }
        }
        return factory;
    }

    private void setAttributesFromNode(Node node) {
        if (node == null) {
            return;
        }
        var attrs = node.getAttributes();
        for (var i = 0; i < attrs.getLength(); i++) {
            var item = attrs.item(i);
            if (!ATTR_DISABLED.equals(item.getNodeName())) {
                writeAttribute(item.getNodeName(), item.getNodeValue());
            }
        }
    }

    private void write(String str, String context) {
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new XMLException(String.format(
                    "Could not write to XML stream (\"%s\").", context), e);
        }
    }
}
