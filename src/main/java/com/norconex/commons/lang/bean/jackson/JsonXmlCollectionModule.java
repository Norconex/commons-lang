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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.CollectionType;

/**
 * Jackson module providing (de)serializers for Collections, so they can be
 * written and read as JSON, Yaml, and XML without special hacks.
 */
public class JsonXmlCollectionModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public JsonXmlCollectionModule() {
        setSerializerModifier(new CollectionSerializerModifier());
        setDeserializerModifier(new CollectionDeserializerModifier());
    }

    public static class CollectionSerializerModifier
            extends BeanSerializerModifier {
        private static final long serialVersionUID = 1L;
        @Override
        public JsonSerializer<?> modifyCollectionSerializer(
                SerializationConfig config,
                CollectionType valueType,
                BeanDescription beanDesc,
                JsonSerializer<?> serializer) {
            return new JsonXmlCollectionSerializer<>(serializer);
        }
    }

    public static class CollectionDeserializerModifier
            extends BeanDeserializerModifier {
        private static final long serialVersionUID = 1L;
        @Override
        public JsonDeserializer<?> modifyCollectionDeserializer(
                DeserializationConfig config, CollectionType type,
                BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            return new JsonXmlCollectionDeserializer<>(deserializer);
        }
    }
}