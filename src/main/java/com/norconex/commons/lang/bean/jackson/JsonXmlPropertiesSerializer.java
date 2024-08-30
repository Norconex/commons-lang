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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.map.Properties;

/**
 * XML serializer for {@link Properties}.
 * Already registered in {@link BeanMapper}.
 */
class JsonXmlPropertiesSerializer extends JsonSerializer<Properties> {

    @Override
    public void serialize(
            Properties props, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        for (String key : props.keySet()) {
            gen.writeFieldName(key);
            gen.writeStartArray();
            for (String val : props.getStrings(key)) {
                gen.writeString(val);
            }
            gen.writeEndArray();

        }
        gen.writeEndObject();
    }
}