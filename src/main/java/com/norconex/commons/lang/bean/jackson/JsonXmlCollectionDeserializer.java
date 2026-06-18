/* Copyright 2023-2026 Norconex Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import com.norconex.commons.lang.ClassUtil;

/**
 * <p>
 * XML collection deserializer. Adds support for the {@link JsonXmlCollection}
 * annotation and reads collections back regardless of how the XML mapper
 * surfaces repeated child elements.
 * </p>
 * <p>
 * Registered through the {@link JsonXmlCollectionModule} deserializer modifier
 * for regular bean collection properties, and through {@code @JsonDeserialize}
 * (on the collection type or carried by {@link JsonXmlCollection}) for
 * collection properties of {@code @JsonUnwrapped} beans (such as
 * {@link com.norconex.commons.lang.config.Configurable} configuration objects),
 * which Jackson resolves through a path that bypasses deserializer modifiers.
 * </p>
 * <p>
 * The XML mapper presents the collection as a wrapper object holding the
 * entries. Depending on the entry kind, repeated elements appear either as a
 * JSON array (typically for object entries) or as repeated property names
 * (typically for scalar entries); a single entry appears as a lone value.
 * All of these shapes are handled here, as is the plain {@code START_ARRAY}
 * used by JSON and YAML.
 * </p>
 * @param <T> type of collection
 * @see JsonXmlCollection
 * @since 3.0.0
 */
public class JsonXmlCollectionDeserializer<T extends Collection<?>>
        extends ValueDeserializer<T> {

    private BeanProperty currentProperty;

    @Override
    public ValueDeserializer<?> createContextual(
            DeserializationContext ctx, BeanProperty property) {
        currentProperty = property;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctx) {
        var contentType = currentProperty.getType()
                .getContentType().getRawClass();
        var objects = createCollection();

        var token = p.currentToken();

        // JSON/YAML style array (also defensive for non-XML contexts).
        if (token == JsonToken.START_ARRAY) {
            while (p.nextToken() != JsonToken.END_ARRAY) {
                addIfPresent(objects, p, contentType);
            }
            return (T) objects;
        }

        // Anything other than a wrapper object (e.g. an empty or self-closed
        // element surfaced as a blank string or null) yields an empty list.
        if (token != JsonToken.START_OBJECT) {
            return (T) objects;
        }

        // XML wrapper object. Each entry is an inner element name (a property)
        // followed by either an array (repeated object entries), a single
        // object/scalar value, or - for repeated scalar entries - a repeated
        // property name. Iterate the whole wrapper until its closing token.
        while (p.nextToken() != JsonToken.END_OBJECT) {
            // current token is the inner element name (PROPERTY_NAME)
            if (p.nextToken() == JsonToken.START_ARRAY) {
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    addIfPresent(objects, p, contentType);
                }
            } else {
                addIfPresent(objects, p, contentType);
            }
        }
        return (T) objects;
    }

    private void addIfPresent(
            Collection<Object> objects, JsonParser p, Class<?> contentType) {
        // Empty tags surface as blank strings; skip them so we do not create
        // an empty entry object.
        if (p.currentToken() == JsonToken.VALUE_STRING
                && StringUtils.isBlank(p.getText())) {
            return;
        }
        var value = p.readValueAs(contentType);
        if (!(value instanceof String v) || !StringUtils.isBlank(v)) {
            objects.add(value);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> createCollection() {
        // Collection type established in this priority order:
        // - specified on annotation
        // - actual type detected and instantiable
        // - HashSet if a Set
        // - Fall-back to ArrayList

        var annot = currentProperty.getAnnotation(JsonXmlCollection.class);

        // from annotation
        if (annot != null && !Void.class.equals(annot.concreteType())) {
            return (Collection<Object>) ClassUtil.newInstance(
                    annot.concreteType());
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
