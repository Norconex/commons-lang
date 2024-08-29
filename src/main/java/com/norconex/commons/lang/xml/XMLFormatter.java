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

import static java.lang.Character.isWhitespace;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.io.IOUtil;
import com.norconex.commons.lang.text.StringUtil;

/**
 * Simple XML Formatter. This formatter may not fully respect all XML
 * specifications and may impact how the original XML would normally be parsed.
 * If this is important to you to preserve everything
 * (white spaces, CDATA, etc), you should use a different
 * formatter. It is not optimized for memory (affected by very large
 * tag content only).
 * It is intended for formatting simple XML for human consumption. It supports
 * XML with no root or the formatting of attributes only.
 * @since 2.0.0
 */
public class XMLFormatter {

    private static final Logger LOG =
            LoggerFactory.getLogger(XMLFormatter.class);

    private static final int TAG_START = '<';
    private static final char[] CDATA_START = "<![CDATA[".toCharArray();
    private static final char[] COMMENT_START = "<!--".toCharArray();
    private static final char[] CLOSING_TAG_START = "</".toCharArray();
    private static final Pattern ATTRIB_PATTERN =
            Pattern.compile("(.*?)=\\s*\"(.*?)\"", Pattern.DOTALL);

    private final Builder cfg;

    public XMLFormatter() {
        this(null);
    }

    private XMLFormatter(Builder b) {
        cfg = new Builder(b);
    }

    public String format(XML xml) {
        return format(xml.toString());
    }

    public String format(String xml) {
        var w = new StringWriter();
        try {
            format(new StringReader(xml), w);
        } catch (IOException e) {
            // Since we are dealing with String reader/writer, there should
            // not be IO exceptions.
            LOG.error("Could not format XML.", e);
        }
        return w.toString();
    }

    public void format(Reader reader, Writer writer) throws IOException {
        Writer w = IOUtils.buffer(writer);
        var depth = 0;
        var tokenReader = new TokenReader(reader, cfg);
        Token token = null;
        var first = true;
        while ((token = tokenReader.next()) != null) {
            if (!first) {
                w.append('\n');
            }
            if (token instanceof CloserTag) {
                depth--;
            }
            token.write(w, buildMargin(depth));
            if (token instanceof Element && !((Element) token).closed) {
                depth++;
            }
            first = false;
        }
        w.flush();
    }

    //--- Token Reader ---------------------------------------------------------

    static class TokenReader {
        final Reader r;
        final Builder cfg;

        TokenReader(Reader reader, Builder cfg) {
            r = IOUtils.buffer(reader);
            this.cfg = cfg;
        }

        // Return a token until null when nothing left to read.
        Token next() throws IOException {
            skipWhiteSpaces(r);

            r.mark(1);
            var ch = r.read();
            r.reset();
            if (ch == -1) {
                return null;
            }

            Token token = null;
            if (ch == TAG_START) {
                if (nextCharsEquals(r, CDATA_START)) {
                    token = new CData(cfg);
                } else if (nextCharsEquals(r, COMMENT_START)) {
                    token = new Comment(cfg);
                } else if (nextCharsEquals(r, CLOSING_TAG_START)) {
                    token = new CloserTag(cfg);
                } else {
                    token = new Element(cfg);
                }

                //TODO PROLOG + top declarations

            } else {
                token = new FreeContent(cfg);
            }

            token.read(r);
            return token;
        }
    }

    //--- XML Tokens -----------------------------------------------------------

    private abstract static class Token {
        protected final Builder cfg;

        abstract void read(Reader r) throws IOException;

        abstract void write(Appendable w, String margin) throws IOException;

        public Token(Builder cfg) {
            this.cfg = cfg;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(
                    this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }

    private static class Element extends Token {
        String name;
        String directContent; // trimmed
        boolean closed; // either a self-closed <tag/> or has a close </tag>
        boolean selfClosed;
        int flIndent = 0;
        final Map<String, String> attribs = new ListOrderedMap<>();

        public Element(Builder cfg) {
            super(cfg);
        }

        @Override
        void read(Reader r) throws IOException {
            final var b = new StringBuilder();
            skipWhiteSpaces(r);
            r.skip(1); // '<'

            //--- Tag name ---
            IOUtil.consumeUntil(
                    r, c -> isWhitespace(c) || c == '/' || c == '>', b);
            name = b.toString();

            //--- Attributes ---
            b.setLength(0);
            IOUtil.consumeUntil(r, c -> c == '>', b);

            var attribsStr = b.toString().trim();
            if (attribsStr.endsWith("/")) {
                closed = true;
                selfClosed = true;
            }
            attribsStr = StringUtils.removeEnd(attribsStr, "/");
            var m = ATTRIB_PATTERN.matcher(attribsStr);
            while (m.find()) {
                attribs.put(m.group(1).trim(), m.group(2).trim());
            }

            r.skip(1); // '>'

            //--- Content ---
            if (!closed) {
                b.setLength(0);
                IOUtil.consumeUntil(r, c -> c == '<', b);
                var txt = b.toString();
                flIndent = firstLineIndentSize(txt);
                directContent = txt.trim();
            }

            //--- Possible closing Tag ---
            r.mark(name.length() + 3);
            var chars = new char[name.length() + 3];
            r.read(chars);
            if (("</" + name + ">").equals(new String(chars))) {
                closed = true;
            } else {
                r.reset();
            }

            // Consider self-closed if empty element as per config
            if (closed && cfg.selfCloseEmptyElements
                    && StringUtils.isBlank(directContent)) {
                selfClosed = true;
            }
        }

        @Override
        void write(Appendable w, String margin) throws IOException {
            var attribsWrapped = false;

            // name
            w.append(margin).append('<').append(name);

            // attributes
            if (!attribs.isEmpty()) {
                attribsWrapped = writeAttributes(w, margin);
            }

            // closing bracket
            if (cfg.closeWrappingTagOnOwnLine && attribsWrapped) {
                w.append('\n').append(margin);
            }
            if (selfClosed) {
                w.append("/>");
                return;
            }
            w.append(">");

            // possible direct content
            // we "nest" on any the following conditions:
            var nestContent = !closed || attribsWrapped;
            if (StringUtils.isNotBlank(directContent)) {
                var trimmedContent = directContent.trim();
                // line length = margin + '<tag>' + content + '</tag>'
                var lineLength = margin.length() + (name.length() * 2) + 5
                        + trimmedContent.length();
                nestContent = nestContent
                        || StringUtils.containsAny(trimmedContent, "\n\r")
                        || (cfg.maxLineLength > 0
                                && lineLength > cfg.maxLineLength);
                if (nestContent) {
                    w.append('\n').append(margin).append(cfg.elementIndent);
                    nestContent = true;
                }
                writeContent(cfg, w, directContent,
                        margin + cfg.elementIndent, flIndent);
            }

            // If tag not closed yet but was closed in source, close it
            // (else remains open)
            if (closed) {
                if (nestContent) {
                    w.append('\n').append(margin);
                }
                w.append("</" + name + ">");
            }
        }

        // returns whether it wrapped
        private boolean writeAttributes(Appendable w, String margin)
                throws IOException {
            return switch (cfg.attributeWrap) {
                case NONE: {
                    writeAttribsWrapNone(w);
                    yield false;
                }
                case AT_MAX:
                    yield writeAttribsWrapAtMax(w, margin);
                case AT_MAX_ALL:
                    yield writeAttribsWrapAtMaxAll(w, margin);
                default: // ALL
 {
                    writeAttribsWarpAll(w, margin);
                    yield true;
                }
            };
        }

        private void writeAttribsWrapNone(Appendable w)
                throws IOException {
            for (Entry<String, String> en : attribs.entrySet()) {
                w.append(" " + en.getKey() + "=\"" + en.getValue() + "\"");
            }
        }

        private void writeAttribsWarpAll(Appendable w, String margin)
                throws IOException {
            for (Entry<String, String> en : attribs.entrySet()) {
                w.append("\n" + margin + attribIndent())
                        .append(en.getKey() + "=\"" + en.getValue() + "\"");
            }
        }

        private boolean writeAttribsWrapAtMaxAll(Appendable w, String margin)
                throws IOException {

            if (cfg.maxLineLength <= 0) {
                writeAttribsWrapNone(w);
                return false;
            }

            var tagEndLength = 0;
            if (!cfg.closeWrappingTagOnOwnLine) {
                // '/>' vs '>'
                tagEndLength = (selfClosed) ? 2 : 1;
            }

            // Calculate the potential text length:
            //   '<' + name length + all attribs length + '>' or '/>'
            var txtLength = 1 + name.length()
                    + attribs.entrySet().stream()
                            .mapToInt(en -> en.getKey().length()
                                    + en.getValue().length() + 4)
                            .sum()
                    + tagEndLength;
            // wrap only if needed
            if (margin.length() + txtLength > cfg.maxLineLength
                    && txtLength >= cfg.minTextLength) {
                writeAttribsWarpAll(w, margin);
                return true;
            }
            writeAttribsWrapNone(w);
            return false;
        }

        private boolean writeAttribsWrapAtMax(Appendable w, String margin)
                throws IOException {

            var wrapped = false;
            var tagEndLength = 0;
            if (!cfg.closeWrappingTagOnOwnLine) {
                // '/>' vs '>'
                tagEndLength = (selfClosed) ? 2 : 1;
            }
            // we start the text length at '<' + name length
            var txtLength = name.length() + 1;
            for (Entry<String, String> en : attribs.entrySet()) {
                var attribute = en.getKey() + "=\"" + en.getValue() + "\"";
                // ' ' + attribute
                txtLength += attribute.length() + 1;
                var lineLength = margin.length() + txtLength + tagEndLength;
                if (lineLength > cfg.maxLineLength
                        && txtLength >= cfg.minTextLength) {
                    // wrap and indent
                    w.append("\n" + margin + attribIndent());
                    txtLength = attribIndent().length() + attribute.length();
                    wrapped = true;
                } else {
                    w.append(" ");
                }
                w.append(attribute);
            }
            return wrapped;
        }

        private String attribIndent() {
            return cfg.attributeIndent == null ? EMPTY : cfg.attributeIndent;
        }
    }

    private static class FreeContent extends Token {
        String text; // trimmed
        int flIndent = 0;

        public FreeContent(Builder cfg) {
            super(cfg);
        }

        @Override
        void read(Reader r) throws IOException {
            var b = new StringBuilder();
            IOUtil.consumeUntil(r, c -> c == '<', b);
            var txt = b.toString();
            flIndent = firstLineIndentSize(txt);
            text = b.toString().trim();
        }

        @Override
        void write(Appendable w, String margin) throws IOException {
            if (StringUtils.isNotBlank(text)) {
                w.append(margin);
                writeContent(cfg, w, text, margin, flIndent);
            }
        }
    }

    private static class CloserTag extends Token {
        String name = null;

        public CloserTag(Builder cfg) {
            super(cfg);
        }

        @Override
        void read(Reader r) throws IOException {
            var b = new StringBuilder();
            r.skip(2); // '</'
            IOUtil.consumeUntil(r, c -> c == '>', b);
            name = b.toString().trim();
            r.skip(1); // '>'
        }

        @Override
        void write(Appendable w, String margin) throws IOException {
            if (StringUtils.isNotEmpty(name)) {
                w.append(margin).append("</").append(name).append('>');
            }
        }
    }

    private static class CData extends Token {
        String text = null; // trimmed

        public CData(Builder cfg) {
            super(cfg);
        }

        @Override
        void read(Reader r) throws IOException {
            var b = new StringBuilder();
            IOUtil.consumeUntil(r, "]]>", b);
            text = b.toString().trim();
        }

        @Override
        void write(Appendable w, String margin) throws IOException {
            w.append(margin).append(text);
        }
    }

    private static class Comment extends Token {
        String text = null; // trimmed
        int flIndent = 0; // non-zero only if on separate line from tag

        public Comment(Builder cfg) {
            super(cfg);
        }

        @Override
        void read(Reader r) throws IOException {
            var b = new StringBuilder();
            r.skip(4); // '<!--'

            IOUtil.consumeUntil(r, "-->", b);
            b.setLength(b.length() - 3);
            var txt = b.toString();
            flIndent = firstLineIndentSize(txt);
            text = txt.trim();
        }

        @Override
        void write(Appendable w, String margin) throws IOException {
            if (StringUtils.isBlank(text)) {
                return;
            }

            if (cfg.blankLineBeforeComment) {
                w.append('\n');
            }

            w.append(margin).append("<!--");

            // nest comment if contains new line or too big.
            var trimmedText = text.trim();
            var lineLength = margin.length() + 9 + trimmedText.length();
            var nestContent = StringUtils.containsAny(trimmedText, "\n\r")
                    || cfg.maxLineLength > 0 && lineLength > cfg.maxLineLength;
            if (nestContent) {
                w.append('\n').append(margin).append(cfg.elementIndent);
            } else {
                w.append(' ');
            }
            writeContent(cfg, w, text, margin + cfg.elementIndent, flIndent);
            if (nestContent) {
                w.append('\n').append(margin).append("  ");
            } else {
                w.append(' ');
            }
            w.append("-->");
            if (cfg.blankLineAfterComment) {
                w.append('\n');
            }
        }
    }

    //--- Util. Methods --------------------------------------------------------

    private static final Pattern FL_INDENT_PATTERN =
            Pattern.compile("(?s)^[\\s\n\r]*[\n\r](\\s+).*$");

    private static int firstLineIndentSize(String str) {
        var m = FL_INDENT_PATTERN.matcher(str);
        if (m.matches()) {
            return m.group(1).length();
        }
        return 0;
    }

    private static void writeContent(
            Builder cfg, Appendable w, String content, String margin,
            int firstLineIndentSize) throws IOException {

        //--- Analyze each line ---
        // If single-line trim
        // if multi-line AND preserving indent, loop through lines and find
        // smallest indent which will be removed from each line in favor of
        // margin.
        var spacesToCut = -1;
        List<String> paragraphs = Arrays.asList(content.split("\\R"));
        for (var i = 0; i < paragraphs.size(); i++) {
            var paragraph = paragraphs.get(i);
            if (cfg.preserveTextIndent) {
                if (spacesToCut == -1) {
                    // first line, use argument
                    spacesToCut = firstLineIndentSize;
                } else if (StringUtils.isNotBlank(paragraph)) {
                    spacesToCut = Math.min(StringUtil.countMatchesStart(
                            paragraph, " "), spacesToCut);
                }
            } else {
                paragraph = StringUtil.trimStart(paragraph);
            }
            paragraphs.set(i, paragraph);
        }

        //--- Process each lines ---

        var b = new StringBuilder();
        for (String paragraph : paragraphs) {
            var line = paragraph;
            var textIndent = "";
            if (cfg.preserveTextIndent) {
                textIndent = StringUtils.repeat(' ',
                        StringUtil.countMatchesStart(line, " ") - spacesToCut);
                line = line.trim();//StringUtil.trimStart(line);
            }
            var lineLength =
                    margin.length() + textIndent.length() + paragraph.length();
            if (cfg.maxLineLength > 0
                    && lineLength > cfg.maxLineLength
                    && line.length() >= cfg.minTextLength) {
                b.append(breakLongParagraphs(cfg, margin + textIndent, line));
            } else if (StringUtils.isNotBlank(line)) {
                b.append(margin + textIndent + line);
            }
            b.append('\n');
        }
        w.append(b.toString().trim());
    }

    private static String breakLongParagraphs(
            Builder cfg, String margin, String paragraph) {
        var maxTextLength = NumberUtils.max(
                1, cfg.minTextLength, cfg.maxLineLength - margin.length());
        return paragraph.replaceAll(
                "(.{1," + maxTextLength + "})( |$)",
                margin + "$1\n");
    }

    private static boolean nextCharsEquals(Reader r, char[] chars)
            throws IOException {
        return Arrays.equals(chars, IOUtil.borrowCharacters(r, chars.length));
    }

    private static int skipWhiteSpaces(Reader r) throws IOException {
        return IOUtil.consumeWhile(r, Character::isWhitespace);
    }

    private String buildMargin(int depth) {
        if (cfg.elementIndent == null) {
            return EMPTY;
        }
        return StringUtils.repeat(cfg.elementIndent, depth);
    }

    //--- Builder --------------------------------------------------------------

    public static class Builder {
        public enum AttributeWrap {
            /**
             * Wrap ALL attributes (one per line).
             */
            ALL,
            /**
             * Never wrap attributes
             * (ignoring {@link Builder#maxLineLength(int)}).
             */
            NONE,
            /**
             * Wrap attributes only when {@link Builder#maxLineLength(int)}
             * is reached (respecting {@link Builder#minTextLength(int)}),
             * keeping as many attributes as possible on each line.
             */
            AT_MAX,
            /**
             * Wrap attributes only when {@link Builder#maxLineLength(int)}
             * is reached, but putting them all on their own line.
             */
            AT_MAX_ALL
        }

        private String elementIndent = "  ";
        private String attributeIndent = "    ";
        private AttributeWrap attributeWrap = AttributeWrap.ALL;
        private boolean closeWrappingTagOnOwnLine;
        private int maxLineLength = 80;
        private int minTextLength = 40;
        private boolean blankLineBeforeComment;
        private boolean blankLineAfterComment;
        private boolean selfCloseEmptyElements;
        private boolean preserveTextIndent;

        private Builder() {
        }

        // Copy constructor
        private Builder(Builder b) {
            elementIndent = b.elementIndent;
            attributeIndent = b.attributeIndent;
            attributeWrap = b.attributeWrap;
            closeWrappingTagOnOwnLine = b.closeWrappingTagOnOwnLine;
            maxLineLength = b.maxLineLength;
            minTextLength = b.minTextLength;
            blankLineBeforeComment = b.blankLineBeforeComment;
            blankLineAfterComment = b.blankLineAfterComment;
            selfCloseEmptyElements = b.selfCloseEmptyElements;
            preserveTextIndent = b.preserveTextIndent;
        }

        /**
         * String to use for indenting elements.
         * Repeated as needed to match the current hierarchical depth of the
         * element.
         * Defaults to two spaces.
         * @param indent string to use as indent
         * @return this builder
         */
        public Builder elementIndent(String indent) {
            elementIndent = indent;
            return this;
        }

        /**
         * String to use for indenting attributed, when wrapped.
         * Defaults to four spaces.
         * @param indent string to use as indent
         * @return this builder
         */
        public Builder attributeIndent(String indent) {
            attributeIndent = indent;
            return this;
        }

        /**
         * Attribute wrapping strategy.
         * Defaults to {@link AttributeWrap#ALL}.
         * @param attributeWrapping attribute wrapping strategy
         * @return this builder
         */
        public Builder attributeWrapping(AttributeWrap attributeWrapping) {
            attributeWrap = attributeWrapping;
            return this;
        }

        /**
         * Put the closing angle bracket of tags with wrapping
         * attributes on its own line, aligned with opening angle bracket.
         * Defaults to adding it the closing angle bracket at the end of the
         * last attribute.
         * @return this builder
         */
        public Builder closeWrappingTagOnOwnLine() {
            closeWrappingTagOnOwnLine = true;
            return this;
        }

        /**
         * Maximum length a line can have before wrapping is performed.
         * Tries to do smart break when possible. When not possible,
         * (e.g., a very long word) it will not wrap.
         * {@link #minTextLength(int)} and {@link AttributeWrap#NONE}
         * both take precedence over this value.
         * @param charQty maximum number of characters.
         * @return this builder
         */
        public Builder maxLineLength(int charQty) {
            maxLineLength = charQty;
            return this;
        }

        /**
         * Minimum length for text on any line before text can be wrapped.
         * This takes precedence over {@link #maxLineLength(int)}.
         * @param charQty minimum number of characters.
         * @return this builder
         */
        public Builder minTextLength(int charQty) {
            minTextLength = charQty;
            return this;
        }

        /**
         * Inserts a blank line before a comment.
         * @return this builder
         */
        public Builder blankLineBeforeComment() {
            blankLineBeforeComment = true;
            return this;
        }

        /**
         * Inserts a blank line after a comment.
         * @return this builder
         */
        public Builder blankLineAfterComment() {
            blankLineAfterComment = true;
            return this;
        }

        /**
         * Self-close elements with no or blank values.
         * @return this builder
         */
        public Builder selfCloseEmptyElements() {
            selfCloseEmptyElements = true;
            return this;
        }

        /**
         * Preserves indentation found in element or comment text.
         * By default all lines are trimmed. When this option is set,
         * indentations (relative to the shorted indent) are kept.
         * @return this builder
         */
        public Builder preserveTextIndent() {
            preserveTextIndent = true;
            return this;
        }

        /**
         * Creates an immutable, thread-safe XML formatter instance
         * using this builder.
         * @return XML formatter
         */
        public XMLFormatter build() {
            return new XMLFormatter(this);
        }
    }

    /**
     * Gets an XML formatter builder.
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
