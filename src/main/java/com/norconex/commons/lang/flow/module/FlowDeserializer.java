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

import java.util.function.Consumer;

import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.flow.FlowMapperConfig;
import com.norconex.commons.lang.flow.JsonFlow;
import com.norconex.commons.lang.flow.JsonFlow.NoBuilder;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Flow deserializer.
 *
 * @param <T> type type of object being deserialized
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class FlowDeserializer<T> extends ValueDeserializer<Consumer<T>> {

    private final FlowMapperConfig config;
    private final ValueDeserializer<?> defaultDeserializer;
    private final ThreadLocal<FlowMapperConfig> propCfg = new ThreadLocal<>();

    @SuppressWarnings("unchecked")
    private RootHandler<T> rootHandler =
            (RootHandler<T>) Statement.THEN.handler();

    @Override
    public Consumer<T> deserialize(JsonParser p, DeserializationContext ctx) {
        return rootHandler.read(new FlowDeserContext(
                ofNullable(propCfg.get()).orElse(config), p, ctx));
    }

    @Override
    public ValueDeserializer<?> createContextual(
            DeserializationContext ctxt, BeanProperty property)
            throws DatabindException {
        if (property == null) {
            return defaultDeserializer;
        }
        var flowAnnot = property.getAnnotation(JsonFlow.class);
        if (flowAnnot == null) {
            return defaultDeserializer;
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
    public void resolve(DeserializationContext ctxt)
            throws DatabindException {
        if (defaultDeserializer != null) {
            defaultDeserializer.resolve(ctxt);
        }
    }

    @Data
    static class FlowDeserContext {
        private final FlowMapperConfig config;
        private final JsonParser parser;
        private final DeserializationContext jacksonContext;
        private int depth;

        int incrementDepth() {
            return depth++;
        }

        int decrementDepth() {
            return --depth;
        }
    }
}
