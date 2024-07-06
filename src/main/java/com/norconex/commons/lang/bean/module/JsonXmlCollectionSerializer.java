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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;

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

/**
 * Collection serializer.
 * @param <T> Collection type to be serialized
 * @see JsonXmlCollection
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class JsonXmlCollectionSerializer<T extends Collection<?>> extends JsonSerializer<T>
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
            SerializerProvider sp) throws IOException {

        var innerName = innerName();
        ((ToXmlGenerator) gen).setNextName(QName.valueOf(innerName));



        if (CollectionUtils.isEmpty(objects)) {
            ((ToXmlGenerator) gen).writeNull();
            return;
        }

        var isXml = gen instanceof ToXmlGenerator;
        if (isXml) {
            var first = true;
            for (Object object : objects) {
                // Not sure why, but first field name is already written for us.
                if (!first) {
                    gen.writeFieldName(innerName);
                }
                gen.writeObject(object);
                first = false;
            }
        } else {
            gen.writeStartArray();
            for (Object object : objects) {
                gen.writeObject(object);
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
