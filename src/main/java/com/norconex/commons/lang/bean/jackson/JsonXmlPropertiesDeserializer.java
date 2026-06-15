/* Copyright 2023 Norconex Inc.
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

import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.map.Properties;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * XML deserializer for {@link Properties}.
 * Already registered in {@link BeanMapper}.
 * @since 3.0.0
 */
public class JsonXmlPropertiesDeserializer extends StdDeserializer<Properties> {

    private static final long serialVersionUID = 1L;

    public JsonXmlPropertiesDeserializer() {
        super(Properties.class);
    }

    public JsonXmlPropertiesDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Properties deserialize(
            JsonParser p, DeserializationContext ctxt) {
        var node = (JsonNode) p.readValueAsTree();
        var props = new Properties();

        node.forEachEntry((key, valueNode) -> {
            if (valueNode.isArray()) {
                valueNode.forEach(v -> addString(props, key, v));
            } else {
                addString(props, key, valueNode);
            }
        });
        return props;
    }

    private void addString(Properties props, String key, JsonNode valueNode) {
        props.add(key, valueNode.isTextual()
                ? valueNode.textValue()
                : valueNode.asText());
    }
}
