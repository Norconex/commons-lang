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

import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.type.CollectionType;
import com.norconex.commons.lang.bean.BeanMapper;

/**
 * Jackson module providing (de)serializers for Collections, so they can be
 * written and read as JSON, Yaml, and XML without special hacks.
 * <p>
 * The deserializer modifier covers collection properties of regular (not
 * {@code @JsonUnwrapped}) beans. Collection properties of {@code @JsonUnwrapped}
 * beans (such as {@link com.norconex.commons.lang.config.Configurable}
 * configuration objects) are resolved by Jackson through a path that bypasses
 * deserializer modifiers; those are handled via {@code @JsonDeserialize}
 * annotations, either on the collection type itself or carried by
 * {@link JsonXmlCollection}.
 * Already registered in {@link BeanMapper}.
 * @since 3.0.0
 */
public class JsonXmlCollectionModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public JsonXmlCollectionModule() {
        setSerializerModifier(new CollectionSerializerModifier());
        setDeserializerModifier(new CollectionDeserializerModifier());
    }

    public static class CollectionSerializerModifier
            extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public ValueSerializer<?> modifyCollectionSerializer(
                SerializationConfig config,
                CollectionType valueType,
                Supplier beanDesc,
                ValueSerializer<?> serializer) {
            return new JsonXmlCollectionSerializer<>(serializer);
        }
    }

    public static class CollectionDeserializerModifier
            extends ValueDeserializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public ValueDeserializer<?> modifyCollectionDeserializer(
                DeserializationConfig config, CollectionType type,
                Supplier beanDesc, ValueDeserializer<?> deserializer) {
            return new JsonXmlCollectionDeserializer<>();
        }
    }
}