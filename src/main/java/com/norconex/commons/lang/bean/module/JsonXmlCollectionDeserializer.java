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
package com.norconex.commons.lang.bean.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.XmlDeserializationContext;
import com.norconex.commons.lang.ClassUtil;

import lombok.RequiredArgsConstructor;

/**
 * Collection deserializer.
 * @param <T> type of collection
 * @see JsonXmlCollection
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class JsonXmlCollectionDeserializer <T extends Collection<?>>
        extends JsonDeserializer<T>
        implements ContextualDeserializer, ResolvableDeserializer {
    private BeanProperty currentProperty;
    private final JsonDeserializer<?> defaultDeserializer;

    @Override
    public JsonDeserializer<?> createContextual(
            DeserializationContext ctx, BeanProperty property)
                    throws JsonMappingException {
        currentProperty = property;
        if (property == null) {
            return defaultDeserializer;
        }
        return Collection.class.isAssignableFrom(
                property.getType().getRawClass())
                        && ctx instanceof XmlDeserializationContext
                                ? this : defaultDeserializer;
    }

    @Override
    public void resolve(DeserializationContext ctxt)
            throws JsonMappingException {
        if (defaultDeserializer != null
                && defaultDeserializer instanceof ResolvableDeserializer rd) {
            rd.resolve(ctxt);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(
            JsonParser p, DeserializationContext ctx) throws IOException {

        var objects = createCollection();
        p.nextToken();  // <outer>

        var isXml = ctx instanceof XmlDeserializationContext;

        var enderToken = isXml ? JsonToken.END_OBJECT : JsonToken.END_ARRAY;

        do {
            // For XML, each collection entries are made of a field name
            // followed by either an object or a scalar.

            if (isXml) {
                p.nextToken();  // <inner>  (field name)
            }
            // for some reason, empty tags come up as empty string
            // We check here to prevent reading a null entry, which may generate
            // the instantiation of an object with nothing set on it.
            if (StringUtils.isNotBlank(p.getText())) {
                var value = p.readValueAs(currentProperty.getType()
                        .getContentType().getRawClass());
                if (!(value instanceof String v) || !StringUtils.isBlank(v)) {
                    objects.add(value);
                }
            }
        } while (p.nextToken() != enderToken);
        return  (T) objects;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> createCollection() {
        // Priority:
        // - specified on annotation
        // - actual type detected and instantiable
        // - HashSet if a Set
        // - Fall-back to ArrayList

        var annot = currentProperty.getAnnotation(JsonXmlCollection.class);

        // from annotation
        if (annot != null && !Void.class.equals(annot.concreteType())) {
            return (Collection<Object>)
                    ClassUtil.newInstance(annot.concreteType());
        }

        // from actual type
        Class<?> actualType = currentProperty.getType().getRawClass();
        try {
            var coll = ClassUtil.newInstance(actualType);
            if (coll != null) {
                return (Collection<Object>) coll;
            }
        } catch (Exception e) {
            // swallow
        }

        // hashset if set
        if (actualType != null && Set.class.isAssignableFrom(actualType)) {
            actualType = HashSet.class;
        } else {
            actualType = ArrayList.class;
        }
        return (Collection<Object>) ClassUtil.newInstance(actualType);
    }
}