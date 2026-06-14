/* Copyright 2024 Norconex Inc.
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
package com.norconex.commons.lang.bean.jackson;

import javax.xml.stream.XMLStreamWriter;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.xml.XmlPrettyPrinter;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.ser.ToXmlGenerator;
import tools.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;

/**
 * Version of {@link XmlFactory} that configures a
 * {@link XmlPrettyPrinter} to write "empty" objects with a closing tag
 * instead of self-closing. Self-closing elements ({@code <e/>}) are treated
 * as {@code null} and a pair of tags with no content ({@code <e></e>}) is
 * treated as empty.
 * @since 3.0.0
 */
public class EmptyWithClosingTagXmlFactory extends XmlFactory {

    private static final long serialVersionUID = 1L;

    public EmptyWithClosingTagXmlFactory() {
        super();
    }

    protected EmptyWithClosingTagXmlFactory(EmptyWithClosingTagXmlFactory src) {
        super(src);
    }

    @Override
    public XmlFactory copy() {
        return new EmptyWithClosingTagXmlFactory(this);
    }

    @Override
    protected ToXmlGenerator _toXmlGenerator(
            ObjectWriteContext writeCtxt, IOContext ioCtxt,
            XMLStreamWriter sw) {
        XmlPrettyPrinter pp = _resolveXmlPrettyPrinter(writeCtxt);
        return new ToXmlGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                writeCtxt.getFormatWriteFeatures(_formatWriteFeatures),
                sw,
                pp,
                _nameProcessor);
    }

    private XmlPrettyPrinter
            _resolveXmlPrettyPrinter(ObjectWriteContext writeCtxt) {
        PrettyPrinter configured = writeCtxt.getPrettyPrinter();
        if (configured == null) {
            // No indent configured: use a non-indenting printer that forces closing tags
            var pp = new ClosingTagXmlPrettyPrinter();
            pp.indentArraysWith(null);
            pp.indentObjectsWith(null);
            return pp;
        }
        if (!(configured instanceof XmlPrettyPrinter)) {
            throw new IllegalStateException(
                    "Configured PrettyPrinter not of type XmlPrettyPrinter but "
                            + configured.getClass().getName());
        }
        // Already has an XmlPrettyPrinter (e.g. for INDENT_OUTPUT=true);
        // wrap it to also force closing tags
        return new ClosingTagXmlPrettyPrinter(
                (DefaultXmlPrettyPrinter) configured);
    }

    static class ClosingTagXmlPrettyPrinter extends DefaultXmlPrettyPrinter {
        private static final long serialVersionUID = 1L;

        public ClosingTagXmlPrettyPrinter() {
            super();
        }

        protected ClosingTagXmlPrettyPrinter(DefaultXmlPrettyPrinter base) {
            super(base);
        }

        @Override
        public ClosingTagXmlPrettyPrinter createInstance() {
            return new ClosingTagXmlPrettyPrinter(this);
        }

        @Override
        public void writeEndObject(JsonGenerator gen, int nrOfEntries) {
            // Force the underlying StAX writer to use an explicit closing tag
            // instead of a self-closing element. Without this, an empty object
            // like new AutomobileConfig() would produce <object/> which is
            // interpreted as null when read back with EMPTY_ELEMENT_AS_NULL.
            gen.writeRaw("");
            super.writeEndObject(gen, nrOfEntries);
        }
    }
}
