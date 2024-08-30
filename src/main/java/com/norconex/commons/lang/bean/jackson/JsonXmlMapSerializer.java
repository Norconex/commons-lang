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

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializes Map as a list of "entry" elements each having a "key" and "value"
 * child elements. Jackson default serialization of Maps stores the key
 * as the tag name, and it prevents special characters to be used as keys.
 * @param <T> Map concrete type
 * @see JsonXmlMap
 */
public class JsonXmlMapSerializer<T extends Map<?, ?>>
        extends StdSerializer<T> implements ContextualSerializer {

    private static final long serialVersionUID = 1L;

    private transient BeanProperty currentProperty;

    @SuppressWarnings("unchecked")
    public JsonXmlMapSerializer() {
        super((Class<T>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private JsonXmlMapSerializer(BeanProperty currentProperty) {
        super((Class<T>) (Class<?>) Map.class);
        this.currentProperty = currentProperty;
    }

    @Override
    public StdSerializer<?> createContextual(
            SerializerProvider prov, BeanProperty property)
                    throws JsonMappingException {
        return new JsonXmlMapSerializer<>(property);
    }

    @Override
    public void serialize(
            T map,
            JsonGenerator gen,
            SerializerProvider provider) throws IOException {

        var entryName = "entry";
        var keyName = "key";
        var valueName = "value";
        var annot = currentProperty.getAnnotation(JsonXmlMap.class);
        if (annot != null) {
            keyName = defaultIfBlank(annot.keyName(), keyName);
            valueName = defaultIfBlank(annot.valueName(), valueName);
            entryName = defaultIfBlank(annot.entryName(), entryName);
        }
        gen.writeStartObject();  // outter name

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            gen.writeFieldName(entryName);

            gen.writeStartObject();  // <entry>

            // Write key
            gen.writeFieldName(keyName);
            provider.defaultSerializeValue(entry.getKey(), gen);

            // Write value
            gen.writeFieldName(valueName);
            provider.defaultSerializeValue(entry.getValue(), gen);

            // Close the entry element manually
            gen.writeEndObject();  // </entry>
        }
        gen.writeEndObject();  // </entry>
    }
}
