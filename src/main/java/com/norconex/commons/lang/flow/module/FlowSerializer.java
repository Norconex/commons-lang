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
package com.norconex.commons.lang.flow.module;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.flow.FlowMapperConfig;
import com.norconex.commons.lang.flow.JsonFlow;
import com.norconex.commons.lang.flow.JsonFlow.NoBuilder;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Flow serializer.
 *
 * @param <T> type type of object being serialized
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class FlowSerializer<T> extends JsonSerializer<Consumer<T>>
        implements ContextualSerializer, ResolvableSerializer {

    private final FlowMapperConfig config;
    private final JsonSerializer<?> defaultSerializer;
    private final ThreadLocal<FlowMapperConfig> propCfg = new ThreadLocal<>();

    @SuppressWarnings("unchecked")
    private RootHandler<T> rootHandler =
            (RootHandler<T>) Statement.THEN.handler();

    @Override
    public void serialize(
            Consumer<T> value,
            JsonGenerator gen,
            SerializerProvider sp) throws IOException {
        rootHandler.write(value, new FlowSerContext(
                ofNullable(propCfg.get()).orElse(config), gen));
    }

    @Override
    public JsonSerializer<?> createContextual(
            SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return defaultSerializer;
        }

        var flowAnnot = property.getAnnotation(JsonFlow.class);
        if (flowAnnot == null) {
            return defaultSerializer;
        }

        // If needed, create property-specific flow mapper config
        var supplier = flowAnnot.builder();
        if (supplier.equals(NoBuilder.class)) {
            propCfg.remove();
        } else {
            propCfg.set(ClassUtil.newInstance(supplier).get());
        }
        return this;
    }

    @Override
    public void resolve(SerializerProvider provider)
            throws JsonMappingException {
        if (defaultSerializer != null
                && defaultSerializer instanceof ResolvableSerializer rs) {
            rs.resolve(provider);
        }
    }

    @Data
    static class FlowSerContext {
        private final FlowMapperConfig config;
        private final JsonGenerator gen;
    }
}
