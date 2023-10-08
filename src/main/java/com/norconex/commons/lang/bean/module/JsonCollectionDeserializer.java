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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.dataformat.xml.deser.XmlDeserializationContext;
import com.norconex.commons.lang.ClassUtil;

import lombok.RequiredArgsConstructor;

/**
 * Collection deserializer.
 * @param <T> type of collection
 * @see JsonCollection
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class JsonCollectionDeserializer <T extends Collection<?>>
        extends JsonDeserializer<T>
        implements ContextualDeserializer {
    private BeanProperty currentProperty;
    @Override
    public JsonDeserializer<?> createContextual(
            DeserializationContext ctx, BeanProperty property)
                    throws JsonMappingException {
        currentProperty = property;
        if (property == null
                || !(ctx instanceof XmlDeserializationContext)) {
            return null;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(
            JsonParser p, DeserializationContext ctx) throws IOException {
        var objects = createCollection();
        p.nextToken();  // <outer>
        for (; p.currentToken() != JsonToken.END_OBJECT; p.nextToken()) {
            // Each collection entries are made of a field name followed
            // by either an object or a scalar.
            p.nextToken();  // <inner>  (field name)
            var value = p.readValueAs(currentProperty.getType()
                    .getContentType().getRawClass());
            objects.add(value);
        }
        return  (T) objects;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> createCollection() {
        var annot = currentProperty.getAnnotation(JsonCollection.class);
        Class<?> type =
                currentProperty.getType().getRawClass();
        if (annot != null && !Void.class.equals(annot.concreteType())) {
            type = annot.concreteType();
        } else if (type != null && Set.class.isAssignableFrom(type)) {
            type = HashSet.class;
        } else {
            type = ArrayList.class;
        }
        return (Collection<Object>) ClassUtil.newInstance(type);
    }
}