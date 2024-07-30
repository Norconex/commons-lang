/* Copyright 2023-2024 Norconex Inc.
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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.ser.XmlSerializerProvider;
import com.norconex.commons.lang.text.StringUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Collection serializer. Because it needs to handle the serialization
 * of null/empty differently, it needs to control the wrapper elements.
 * This mean for this seralizer to work properly, make sure to set
 * {@link
 * com.fasterxml.jackson.dataformat.xml.XmlMapper.Builder#defaultUseWrapper(
 * boolean)}
 * to <code>false</code>.
 * @param <T> Collection type to be serialized
 * @see JsonXmlCollection
 * @since 3.0.0
 */
@RequiredArgsConstructor
@Slf4j
public class JsonXmlCollectionSerializer<T extends Collection<?>>
        extends JsonSerializer<T>
        implements ContextualSerializer, ResolvableDeserializer {

    public static final String DEFAULT_INNER_NAME = "entry";

    private BeanProperty currentProperty;
    private final JsonSerializer<?> defaultSerializer;

    @Override
    public JsonSerializer<?> createContextual(
            SerializerProvider prov, BeanProperty property)
                    throws JsonMappingException {
        currentProperty = property;
        if (property == null) {
            return defaultSerializer;
        }
        return Collection.class.isAssignableFrom(
                property.getType().getRawClass())
                        && prov instanceof XmlSerializerProvider
                                ? this : defaultSerializer;
    }

    @Override
    public void resolve(DeserializationContext ctxt)
            throws JsonMappingException {
        if (defaultSerializer != null
                && defaultSerializer instanceof ResolvableDeserializer rd) {
            rd.resolve(ctxt);
        }
    }

    @Override
    public void serialize(
            T objects,
            JsonGenerator gen,
            SerializerProvider serializers) throws IOException {

        LOG.debug("Serializing collection: {}", objects);

        var isXml = gen instanceof ToXmlGenerator;

        if (isXml) {
            var xmlGen = (ToXmlGenerator) gen;
            if (objects == null) {
                // Ensure a self-closing wrapper tag is used for null.
                LOG.debug("Null collection, writing self-closing tag.");
                xmlGen.writeStartObject();
                xmlGen.writeEndObject();
                return;
            }

            if (objects.isEmpty()) {
                // Ensure no self-closing wrapper tag and no empty entry
                // tag are written.
                LOG.debug("Empty collection, writing empty tags.");
                xmlGen.writeStartObject();
                xmlGen.writeRaw("");
                xmlGen.writeEndObject();
                return;
            }

            var innerName = innerName();
            LOG.debug("Using inner name: {}", innerName);

            xmlGen.writeStartObject();
            xmlGen.setNextName(new QName(innerName));
            for (Object object : objects) {
                LOG.debug("Serializing collection object: {}", object);
                xmlGen.writeFieldName(innerName);
                if (object == null) {
                    serializers.defaultSerializeNull(xmlGen);
                } else {
                    serializers.defaultSerializeValue(object, xmlGen);
                }
            }
            xmlGen.writeEndObject();
        } else {
            if (objects == null) {
                gen.writeNull();
                return;
            }
            gen.writeStartArray();
            for (Object object : objects) {
                serializers.defaultSerializeValue(object, gen);
            }
            gen.writeEndArray();
        }
    }

    private String innerName() {
        var outterName = currentProperty.getName();
        String innerName = null;
        var annot = currentProperty.getAnnotation(JsonXmlCollection.class);
        if (annot != null) {
            innerName = annot.entryName();
        }
        if (isBlank(innerName)) {
            innerName = StringUtil.singularOrElse(
                    outterName, DEFAULT_INNER_NAME);
        }
        return innerName;
    }
}
