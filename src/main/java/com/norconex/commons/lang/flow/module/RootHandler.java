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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.bean.BeanMapper.FlowConsumerAdapter;
import com.norconex.commons.lang.flow.module.FlowDeserializer.FlowDeserContext;
import com.norconex.commons.lang.function.Consumers;

/**
 * Handles flow conditions and consumers in the order defined.
 *
 * @param <T> type object type evaluated or consumed
 * @since 3.0.0
 */
@SuppressWarnings("unchecked")
class RootHandler<T> implements StatementHandler<Consumer<T>> {

    @Override
    public Consumer<T> read(
            FlowDeserContext ctx) throws IOException {
        var p = ctx.getParser();
        String parentName = null;

        // Ensure current token is START_OBJECT
        if (p.currentToken() == JsonToken.FIELD_NAME) {
            parentName = p.currentName();
            FlowUtil.logOpen(ctx, parentName);
            p.nextToken(); // <-- START_OBJECT
        }

        List<Consumer<T>> consumers = new ArrayList<>();
        while ((p.nextToken()) != JsonToken.END_OBJECT) { // <-- FIELD_NAME
            var name = p.getCurrentName();
            FlowUtil.logOpen(ctx, name);
            var statement = Statement.of(name);
            if (statement == null) {
                consumers.add(readInputConsumer(ctx));
            } else if (statement.isAnyOf(Statement.IF, Statement.IFNOT)) {
                consumers.add((Consumer<T>)
                        statement.handler().read(ctx));
            } else {
                throw new IOException("<" + statement + "> is misplaced.");
            }
            FlowUtil.logClose(ctx, name);
        }

        if (parentName != null) {
            FlowUtil.logClose(ctx, parentName);
        }

        if (consumers.isEmpty()) {
            return null;
        }
        if (consumers.size() == 1) {
            return consumers.get(0);
        }
        return new Consumers<>(consumers);
    }

    private Consumer<T> readInputConsumer(FlowDeserContext ctx)
            throws IOException {
        var p = ctx.getParser();
        var config = ctx.getConfig();

        p.nextToken(); // <-- START_OBJECT

        Class<?> type = config.getInputConsumerType();
        if (type == null) {
            type = Consumer.class;
        } else if (!Consumer.class.isAssignableFrom(type)
                && config.getConditionAdapterType() == null) {
            throw new IOException("""
                Cannot have a flow consumer type that does not implement \
                Consumer without a FlowInputConsumerAdapter.""");
        }

        var mapper = (ObjectMapper) p.getCodec();
        var consumer = mapper.readValue(p, type);
        if (config.getInputConsumerAdapterType() != null) {
            var adapter = (FlowConsumerAdapter<T>)
                    ClassUtil.newInstance(config.getInputConsumerAdapterType());
            adapter.setConsumerAdaptee(consumer);
            FlowUtil.logBody(ctx, adapter);
            return adapter;
        }
        FlowUtil.logBody(ctx, consumer);
        // at this point it has to be a condition or fail.
        return (Consumer<T>) consumer;
    }

    @Override
    public void write() throws IOException {
        //TODO
    }
}

