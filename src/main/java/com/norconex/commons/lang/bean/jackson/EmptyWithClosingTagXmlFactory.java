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

import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;


public class EmptyWithClosingTagXmlFactory extends XmlFactory {

    private static final long serialVersionUID = 1L;

    @Override
    public ToXmlGenerator createGenerator(Writer out) throws IOException {
        var gen = super.createGenerator(out);
        gen.setPrettyPrinter(new XmlPrettyPrinter());
        return gen;
    }

    class XmlPrettyPrinter extends DefaultXmlPrettyPrinter  {
        private static final long serialVersionUID = 1L;
        @Override
        public void writeEndObject(JsonGenerator gen, int nrOfEntries)
                throws IOException {

            // Required to write something here to prevent the underlying
            // XML stream writer from writing a self-closing tag, which
            // is interpreted as null when reading it back.
            gen.writeRaw("");
            super.writeEndObject(gen, nrOfEntries);
        }
    }
}

