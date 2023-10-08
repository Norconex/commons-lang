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
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Collection serializer.
 * @see JsonCollection
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class JsonCollectionSerializer extends JsonSerializer<Collection<?>>
        implements ContextualSerializer {

    private BeanProperty currentProperty;

    @Override
    public JsonSerializer<?> createContextual(
            SerializerProvider prov, BeanProperty property)
                    throws JsonMappingException {
        currentProperty = property;
        return this;
    }

    @Override
    public void serialize(
            Collection<?> objects,
            JsonGenerator gen,
            SerializerProvider sp) throws IOException {

        if (objects == null) {
            return;
        }

        var isXml = gen instanceof ToXmlGenerator;
        if (isXml) {
            var innerName = innerName();
            ((ToXmlGenerator) gen).setNextName(QName.valueOf(innerName));
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
        var annot = currentProperty.getAnnotation(JsonCollection.class);
        if (annot != null) {
            innerName = annot.entryName();
        }
        if (isBlank(innerName)) {
            innerName = singularOrElse(outterName, "entry");
        }
        return innerName;
    }

    private String singularOrElse(String plural, String def) {
        String singular;
        if (plural.endsWith("sses")) {
            singular = removeEnd(plural, "es");
        } else if (plural.endsWith("ies")) {
            singular =  removeEnd(plural, "ies") + "y";
        } else if (plural.endsWith("oes")) {
            singular =  removeEnd(plural, "es");
        } else if (plural.endsWith("s") && !plural.endsWith("ss")) {
            singular = removeEnd(plural, "s");
        } else {
            singular = def;
        }
        return singular;
    }
}
