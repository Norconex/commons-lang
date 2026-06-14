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
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.Map;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import com.norconex.commons.lang.text.StringUtil;

/**
 * Serializes Map as a list of "entry" elements each having a "key" and "value"
 * child elements. Jackson default serialization of Maps stores the key
 * as the tag name, and it prevents special characters to be used as keys.
 * @param <T> Map concrete type
 * @see JsonXmlMap
 */
public class JsonXmlMapSerializer<T extends Map<?, ?>>
        extends StdSerializer<T> {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_ENTRY_NAME = "entry";

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
            SerializationContext prov, BeanProperty property)
            throws DatabindException {
        return new JsonXmlMapSerializer<>(property);
    }

    @Override
    public void serialize(
            T map,
            JsonGenerator gen,
            SerializationContext provider) {

        var entryName = DEFAULT_ENTRY_NAME;
        var keyName = "key";
        var valueName = "value";
        var annot = currentProperty.getAnnotation(JsonXmlMap.class);
        if (annot != null) {
            keyName = defaultIfBlank(annot.keyName(), keyName);
            valueName = defaultIfBlank(annot.valueName(), valueName);
            entryName = annot.entryName();
            if (isBlank(entryName)) {
                entryName = StringUtil.singularOrElse(
                        currentProperty.getName(), DEFAULT_ENTRY_NAME);
            }
            if (isBlank(entryName)) {
                entryName = DEFAULT_ENTRY_NAME;
            }
        }
        gen.writeStartObject(); // outter name

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            gen.writeName(entryName);

            gen.writeStartObject(); // <entry>

            // Write key
            provider.defaultSerializeProperty(keyName, entry.getKey(), gen);

            // Write value
            provider.defaultSerializeProperty(valueName, entry.getValue(), gen);

            // Close the entry element manually
            gen.writeEndObject(); // </entry>
        }
        gen.writeEndObject(); // </entry>
    }
}
