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

import java.util.HashMap;
import java.util.Map;

import com.norconex.commons.lang.ClassUtil;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializes a map represented either as a list of "entry" elements each
 * having "key" and "value" child elements (the XML representation produced by
 * {@link JsonXmlMapSerializer}) or as a native JSON/YAML object. The two are
 * distinguished by the shape of each value (an object/array entry vs a scalar),
 * so no format flag is needed - which matters because this deserializer is also
 * applied via {@code @JsonDeserialize} (carried by {@link JsonXmlMap}) to reach
 * {@code @JsonUnwrapped} bean properties, whose values Jackson replays through a
 * buffer that exposes neither the XML parser nor the XML context.
 * @param <T> Map concrete type
 */
public class JsonXmlMapDeserializer<T extends Map<?, ?>>
        extends StdDeserializer<T> {

    private static final long serialVersionUID = 1L;

    private transient BeanProperty currentProperty;
    private JavaType mapType;

    public JsonXmlMapDeserializer() {
        super(Map.class);
    }

    public JsonXmlMapDeserializer(Class<T> vc) {
        super(vc);
    }

    private JsonXmlMapDeserializer(
            BeanProperty currentProperty, JavaType mapType) {
        super(Map.class);
        this.currentProperty = currentProperty;
        this.mapType = mapType;
    }

    @Override
    public StdDeserializer<?> createContextual(
            DeserializationContext ctxt, BeanProperty property) {
        return new JsonXmlMapDeserializer<>(
                property,
                property != null
                        ? property.getType()
                        : ctxt.constructType(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) {
        var map = createMap();
        var keyName = "key";
        var valueName = "value";
        Class<?> keyType = null;
        Class<?> valueType = null;
        if (currentProperty != null) {
            var annot = currentProperty.getAnnotation(JsonXmlMap.class);
            if (annot != null) {
                keyName = defaultIfBlank(annot.keyName(), keyName);
                valueName = defaultIfBlank(annot.valueName(), valueName);
                if (!Void.class.equals(annot.keyType())) {
                    keyType = annot.keyType();
                }
                if (!Void.class.equals(annot.valueType())) {
                    valueType = annot.valueType();
                }
            }
        }

        // p is positioned at the START_OBJECT of the map. Each member is either
        // an "entry" element whose value is an object/array (XML), or a direct
        // key->scalar pair (native JSON/YAML).
        while (p.nextToken() != JsonToken.END_OBJECT) {
            var memberName = p.currentName();
            var valueToken = p.nextToken();
            if (valueToken == JsonToken.START_ARRAY) {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    readEntry(p, ctxt, map, keyName, valueName, keyType,
                            valueType);
                }
            } else if (valueToken == JsonToken.START_OBJECT) {
                readEntry(p, ctxt, map, keyName, valueName, keyType, valueType);
            } else {
                // native key->value pair: member name is the key
                map.put(memberName, ctxt.readValue(p,
                        fieldType(valueType, mapType.containedType(1))));
            }
        }
        return (T) map;
    }

    // reads a single XML "entry" object (positioned at its START_OBJECT)
    private void readEntry(
            JsonParser p, DeserializationContext ctxt, Map<Object, Object> map,
            String keyName, String valueName, Class<?> keyType,
            Class<?> valueType) {
        Object key = null;
        Object value = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            var fieldName = p.currentName();
            p.nextToken(); // Move to the value of the field
            if (keyName.equals(fieldName)) {
                key = ctxt.readValue(p,
                        fieldType(keyType, mapType.containedType(0)));
            } else if (valueName.equals(fieldName)) {
                value = ctxt.readValue(p,
                        fieldType(valueType, mapType.containedType(1)));
            }
        }
        if (key != null) {
            map.put(key, value);
        }
    }

    private Class<?> fieldType(Class<?> annotType, JavaType containedType) {
        if (annotType != null) {
            return annotType;
        }
        if (containedType != null) {
            return containedType.getRawClass();
        }
        return Object.class;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> createMap() {
        // Map type established in this priority order:
        // - specified on annotation
        // - actual type detected and instantiable
        // - Fall-back to HashMap

        // from annotation
        if (currentProperty != null) {
            var annot = currentProperty.getAnnotation(JsonXmlMap.class);
            if (annot != null && !Void.class.equals(annot.mapType())) {
                return (Map<Object, Object>) ClassUtil.newInstance(
                        annot.mapType());
            }
        }

        // from actual type
        try {
            var map = ClassUtil.newInstance(mapType.getRawClass());
            if (map != null) {
                return (Map<Object, Object>) map;
            }
        } catch (Exception e) {
            // swallow
        }

        return ClassUtil.newInstance(HashMap.class);
    }
}
