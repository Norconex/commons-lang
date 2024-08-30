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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.norconex.commons.lang.ClassUtil;

/**
 * Deserializes a list of "entry" elements each having a "key" and "value"
 * child elements into a Map.
 * @param <T> Map concrete type
 */
public class JsonXmlMapDeserializer<T extends Map<?, ?>>
        extends StdDeserializer<T>
        implements ContextualDeserializer {

    private static final long serialVersionUID = 1L;

    private transient BeanProperty currentProperty;
    private JavaType mapType;

    public JsonXmlMapDeserializer() {
        this((Class<T>) null);
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
    public T deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
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

        while (p.nextToken() != JsonToken.END_OBJECT) {

            if (p.getCurrentToken() == JsonToken.START_OBJECT) {
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
        }
        return (T) map;
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
