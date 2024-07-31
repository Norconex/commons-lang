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

        var isXml = ctx instanceof XmlDeserializationContext;
        var contentClass = currentProperty.getType().getContentType().getRawClass();
        var objects = createCollection();

        // For XML, we move the cursor to first child value (i.e., unwrap).
        if (isXml) {           // START_OBJECT (outer)
            p.nextToken();     // FIELD_NAME (outer)
            if (p.nextToken()  // START_OBJECT or VALUE_NULL (inner)
                    == JsonToken.VALUE_NULL) {
                // Self-closed tag which is interpreted as null.
                p.nextToken(); // END_OBJECT (inner)
                return null;
            }
        }

        var hasChild = false;
        while (p.nextToken()
                != JsonToken.END_OBJECT) { // FIELD_NAME or END_OBJECT (inner)
            p.nextToken(); //  move to value (object, scalar, etc.)
            var value = p.readValueAs(contentClass);
            objects.add(value);
            hasChild = true;
        }

        if (isXml && hasChild) {
            // if there were no children, the outer END_OBJECT has already
            // been called at this point.
            p.nextToken();  // END_OBJECT (outer)
        }

        return  (T) objects;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> createCollection() {
        // Type resolution priority:
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