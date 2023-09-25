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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.flow.Flow;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Flow deserializer.
 *
 * @param <T> type type of object being deserialized
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class FlowDeserializer<T> extends JsonDeserializer<Flow<T>> {
    private final FlowMapperConfig config;

    @SuppressWarnings("unchecked")
    private RootHandler<T> rootHandler =
            (RootHandler<T>) Statement.THEN.handler();

    @Override
    public Flow<T> deserialize(JsonParser p, DeserializationContext ctx)
            throws IOException {
        return new Flow<>(rootHandler.read(new FlowDeserContext(config, p)));
    }

    @Data
    static class FlowDeserContext {
        private final FlowMapperConfig config;
        private final JsonParser parser;
        private int depth;
        int incrementDepth() {
            return depth++;
        }
        int decrementDepth() {
            return --depth;
        }
    }
}
