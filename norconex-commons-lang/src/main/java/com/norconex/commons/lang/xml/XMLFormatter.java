/* Copyright 2020 Norconex Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * Simple XML Formatter. This formatter may not fully respect all XML
 * specifications and may impact how the original XML would normally be parsed.
 * If this is important to you to preserve everything
 * (white spaces, CDATA, etc), you should use a different
 * formatter. It is not optimized for memory (affected by very large
 * tag content only).
 * It is intended for formatting simple XML for human consumption. It supports
 * XML with no root or formatting attributes only.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class XMLFormatter {

    private static final Logger LOG =
            LoggerFactory.getLogger(XMLFormatter.class);
    private static final String WRAP_START = "<__wrapper__>";
    private static final String WRAP_END = "</__wrapper__>";

    private int indentSize;
    private int wrapAttributesAt;
    private int wrapContentAt;
    private boolean blankLineBeforeComment;
    private boolean selfCloseEmptyTags;

    // boolean useCDATA after X reserved characters

    public int getIndentSize() {
        return indentSize;
    }
    public XMLFormatter setIndentSize(int indentSize) {
        this.indentSize = indentSize;
        return this;
    }
    public int getWrapAttributesAt() {
        return wrapAttributesAt;
    }
    public XMLFormatter setWrapAttributesAt(int wrapAt) {
        this.wrapAttributesAt = wrapAt;
        return this;
    }
    public int getWrapContentAt() {
        return wrapContentAt;
    }
    public XMLFormatter setWrapContentAt(int wrapContentAt) {
        this.wrapContentAt = wrapContentAt;
        return this;
    }
    public boolean isBlankLineBeforeComment() {
        return blankLineBeforeComment;
    }
    public XMLFormatter setBlankLineBeforeComment(
            boolean blankLineBeforeComment) {
        this.blankLineBeforeComment = blankLineBeforeComment;
        return this;
    }
    public boolean isSelfCloseEmptyTags() {
        return selfCloseEmptyTags;
    }
    public XMLFormatter setSelfCloseEmptyTags(boolean selfCloseEmptyTags) {
        this.selfCloseEmptyTags = selfCloseEmptyTags;
        return this;
    }

    public String format(XML xml) {
        return format(xml.toString());
    }
    public String format(Reader reader) {
        try {
            return format(IOUtils.toString(reader));
        } catch (IOException e) {
            throw newXMLException(e);
        }
    }
    public String format(InputStream inputStream) {
        try {
            return format(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw newXMLException(e);
        }
    }
    public String format(String xml) {
        if (xml == null) {
            return null;
        }
        String wrappedXML = WRAP_START + xml + WRAP_END;
        try {
            StringWriter out = new StringWriter();
            XmlHandler handler = new XmlHandler(out);
            SAXParserFactory factory = XMLUtil.createSaxParserFactory();
            SAXParser parser = factory.newSAXParser();
            parser.setProperty(
                    "http://xml.org/sax/properties/lexical-handler", handler);
            parser.parse(
                    new InputSource(new StringReader(wrappedXML)), handler);
            out.flush();
            return postFormatCleanups(out.toString().trim());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw newXMLException(e);
        }
    }
    private String postFormatCleanups(String text) {
        String xml = text;
        xml = StringUtils.removeStart(xml, WRAP_START);
        xml = StringUtils.removeEnd(xml, WRAP_END);

        // Remove blank lines between tags.
        xml = xml.replaceAll("(>)\n+( *<)", "$1\n$2");

        if (blankLineBeforeComment) {
            xml = xml.replaceAll("(?m)( *<!--)", "\n$1");
        }

        // Convert empty bodies to self-closing tags
        if (selfCloseEmptyTags) {
            xml = xml.replaceAll("<(\\w+)([^>]*?)>\\s*</\\1>", "<$1$2/>");
        }

        return xml;
    }

    class XmlHandler extends DefaultHandler2 {

        private final Writer out;
        private int depth = -1;

        private int lastTagLineLength = 0;
        private int lastTagLength = 0;
        private boolean bodyHasComment;

        private final StringBuilder body = new StringBuilder();

        public XmlHandler(Writer out) {
            super();
            this.out = out;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {

            StringBuilder b = new StringBuilder();
            if (depth > 0 && lastTagLineLength > 0) {
                write("\n");
            }
            indent(b);
            b.append('<');
            b.append(qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                String attribute = esc(attributes.getQName(i)) + "=\""
                        + esc(attributes.getValue(i)) + "\"";
                if (isWrapAttribute(b, attribute)) {
                    write(b.toString());
                    b.setLength(0);
                    b.append('\n');
                    doubleIndent(b);
                } else {
                    b.append(" ");
                }
                b.append(attribute);
            }
            b.append('>');
            write(b.toString());
            depth++;
            lastTagLength = qName.length();
            lastTagLineLength = b.length();
            bodyHasComment = false;
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            body.append(ch, start, length);
        }

        @Override
        public void comment(char[] ch, int start, int length)
                throws SAXException {

            // if no comment, do nothing
            String comment = new String(ch, start, length);
            if (comment.length() == 0) {
                return;
            }
            bodyHasComment = true;
            if (isWrapComment(comment)) {
                comment = wrapCommentText(comment, indentSize * depth + 2);
                comment = comment.replaceFirst("^\n+", "");
                comment = comment.replaceFirst("\\s+$", "");
                comment = "\n" + indent() + "<!--\n" + comment;
                comment += "\n" + indent() + "  -->\n";
                write(comment);
            } else {
                write("\n");
                write(indent() + "<!--" + comment + "-->\n");
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            depth--;

            String text = body.toString().trim();
            body.setLength(0);

            String closingTag = "</" + esc(qName) + ">\n";

            // if no body, close right away
            if (text.length() == 0) {
                // if last tag length is zero, it means we are starting
                // or body was just printed, so we indent
                // same if the body had a comment.
                if (lastTagLength == 0 || bodyHasComment) {
                    write(indent());
                }
                write(closingTag);
                bodyHasComment = false;
                return;
            }

            if (isWrapContent(text)) {
                write(wrapBodyText(text, indentSize * (depth + 1)));
                write(indent() + closingTag);
            } else {
                write(text);
                write(closingTag);
            }
            lastTagLineLength = 0;
            lastTagLength = 0;
            bodyHasComment = false;
        }

        private String wrapCommentText(String text, int leftPadding) {
            StringBuffer b = new StringBuffer();
            Matcher m = Pattern.compile(".*?([\n\r]+|$)").matcher(text);
            while (m.find()) {
                String group = m.group();
                String lineIndent = group.replaceFirst("(?s)^(\\s*).*", "$1");
                lineIndent = StringUtils.leftPad(lineIndent, leftPadding);
                String line = group.replaceFirst("(?s)^\\s+(.*)", "$1");
                int textMaxChars =
                        Math.max(wrapContentAt - lineIndent.length(), 1);
                if (line.length() < textMaxChars) {
                    m.appendReplacement(b,
                            Matcher.quoteReplacement(lineIndent + line));
                } else {
                    m.appendReplacement(b,
                            Matcher.quoteReplacement(breakLongLines(
                                    lineIndent, line, textMaxChars)));
                }
            }
            String newText = b.toString();
            newText = newText.replaceFirst("(.*)\\s+$", "$1");
            return "\n" + newText + "\n";
        }

        private String wrapBodyText(String text, int leftPadding) {
            String lineIndent = StringUtils.repeat(' ', leftPadding);
            int textMaxChars =
                    Math.max(wrapContentAt - lineIndent.length(), 1);
            StringBuffer b = new StringBuffer();
            Matcher m = Pattern.compile(".*?([\n\r]+|$)").matcher(text);
            while (m.find()) {
                String line = m.group();
                line = line.replaceFirst("(?s)^\\s+(.*)", "$1");

                if (line.length() < textMaxChars) {
                    m.appendReplacement(b,
                            Matcher.quoteReplacement(lineIndent + line));
                } else {
                    m.appendReplacement(b,
                            Matcher.quoteReplacement(breakLongLines(
                                    lineIndent, line, textMaxChars)));
                }
            }
            String newText = b.toString();
            newText = newText.replaceFirst("(.*)\\s+$", "$1");
            return "\n" + newText + "\n";
        }

        private String breakLongLines(
                String lineIndent, String text, int textMaxChars) {
            return text.replaceAll(
                    "(.{1," + textMaxChars + "})( |$)",
                    lineIndent + "$1\n");
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            LOG.warn("XML warning: {}.", e.getMessage(), e);
        }
        @Override
        public void error(SAXParseException e) throws SAXException {
            LOG.error("XML error: {}.", e.getMessage(), e);
        }
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            LOG.error("XML fatal error: {}.", e.getMessage(), e);
        }

        private String indent() {
            return StringUtils.repeat(' ', indentSize * depth);
        }
        private String doubleIndent() {
            return StringUtils.repeat(' ', indentSize * depth + indentSize * 2);
        }
        private void indent(StringBuilder b) {
            if (indentSize > 0) {
                b.append(indent());
            }
        }
        private void doubleIndent(StringBuilder b) {
            if (indentSize > 0) {
                b.append(doubleIndent());
            }
        }
        private boolean isWrapAttribute(StringBuilder b, String newText) {
            return wrapAttributesAt > 0
                    && b.length() + newText.length() > wrapAttributesAt;
        }
        private boolean isWrapContent(String text) {
            if (wrapContentAt < 1) {
                return false;
            }
            return StringUtils.containsAny(text, '\n', '\r')
                    || lastTagLineLength + text.length()
                    + lastTagLength + 3 > wrapContentAt;
        }
        private boolean isWrapComment(String text) {
            if (wrapContentAt < 1) {
                return false;
            }
            return StringUtils.containsAny(text, '\n', '\r')
                    || (indentSize * depth) + 2 + text.length() > wrapContentAt;
        }
        private void write(String txt) {
            try {
                out.write(txt);
            } catch (IOException e) {
                throw newXMLException(e);
            }
        }
        private String esc(String txt) {
            return StringEscapeUtils.escapeXml11(txt);
        }
    }

    private static XMLException newXMLException(Exception e) {
        return new XMLException("Could not format XML.", e);
    }
}
