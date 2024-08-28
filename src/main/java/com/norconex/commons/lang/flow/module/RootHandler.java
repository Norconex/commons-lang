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
package com.norconex.commons.lang.flow.module;

import static com.norconex.commons.lang.flow.module.FlowUtil.whileInArrayObjects;
import static com.norconex.commons.lang.flow.module.FlowUtil.whileInObject;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.flow.FlowConsumerAdapter;
import com.norconex.commons.lang.flow.module.FlowDeserializer.FlowDeserContext;
import com.norconex.commons.lang.flow.module.FlowSerializer.FlowSerContext;
import com.norconex.commons.lang.function.Consumers;
import com.norconex.commons.lang.function.PredicatedConsumer;

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

        // Ensure current token is START_ARRAY or START_OBJECT
        if (p.currentToken() == JsonToken.FIELD_NAME) {
            parentName = p.currentName();
            FlowUtil.logOpen(ctx, parentName);
            p.nextToken();
        }
        var consumers = new Consumers<T>();
        if (p.currentToken() == JsonToken.START_ARRAY) {
            whileInArrayObjects(p, () -> readObject(ctx, consumers));
        } else {
            whileInObject(p, () -> readObject(ctx, consumers));
        }

        if (parentName != null) {
            FlowUtil.logClose(ctx, parentName);
        }
        return consumers;
    }

    private void readObject(FlowDeserContext ctx, Consumers<T> consumers)
            throws IOException {
        var p = ctx.getParser();
        var name = p.currentName();
        FlowUtil.logOpen(ctx, name);
        var statement = Statement.of(name);
        if (statement == null) {
            consumers.add(readInputConsumer(ctx));
        } else if (statement.isAnyOf(Statement.IF, Statement.IFNOT)) {
            consumers.add((Consumer<T>) statement.handler().read(ctx));
        } else {
            throw new IOException("<" + statement + "> is misplaced.");
        }
        FlowUtil.logClose(ctx, name);
    }

    private Consumer<T> readInputConsumer(FlowDeserContext ctx)
            throws IOException {
        var p = ctx.getParser();
        var config = ctx.getConfig();

        p.nextToken(); // START_OBJECT

        Class<?> type = config.getConsumerType().getBaseType();
        if (type == null) {
            type = Consumer.class;
        } else if (!Consumer.class.isAssignableFrom(type)
                && config.getPredicateType().getAdapterType() == null) {
            throw new IOException("""
                Cannot have a flow consumer type that does not implement \
                Consumer without a FlowInputConsumerAdapter.""");
        }

        var mapper = (ObjectMapper) p.getCodec();
        var consumer = mapper.readValue(p, type);
        if (config.getConsumerType().getAdapterType() != null) {
            var adapter = (FlowConsumerAdapter<T>) ClassUtil.newInstance(
                    config.getConsumerType().getAdapterType());
            adapter.setConsumerAdaptee(consumer);
            FlowUtil.logBody(ctx, adapter);
            return adapter;
        }
        FlowUtil.logBody(ctx, consumer);
        // at this point it has to be a condition or fail.
        return (Consumer<T>) consumer;
    }

    @Override
    public void write(Consumer<T> obj, FlowSerContext ctx) throws IOException {
        FlowUtil.writeArrayWrap(ctx, () -> {
            // consumer(s)
            if (obj instanceof Consumers<T> arr) {
                for (Consumer<T> c : arr) {
                    writeConsumer(c, ctx);
                }
            } else {
                writeConsumer(obj, ctx);
            }
        });
    }

    private void writeConsumer(Consumer<T> obj, FlowSerContext ctx)
            throws IOException {
        var gen = ctx.getGen();

        // if / ifNot
        if (obj instanceof PredicatedConsumer<T> pred) {
            if (pred.isNegate()) {
                ((IfHandler<T>) Statement.IFNOT.handler()).write(pred, ctx);
            } else {
                ((IfHandler<T>) Statement.IF.handler()).write(pred, ctx);
            }
            return;
        }

        FlowUtil.writeArrayObjectWrap(ctx, () -> {
            gen.writeFieldName(consumerName(obj, ctx));
            if (obj instanceof FlowConsumerAdapter<?> adapter) {
                gen.writeObject(adapter.getConsumerAdaptee());
            } else {
                gen.writeObject(obj);
            }
        });
    }

    private String consumerName(Consumer<T> obj, FlowSerContext ctx) {
        return Optional.ofNullable(ctx.getConfig().getConsumerNameProvider())
                .map(f -> f.apply(obj))
                .orElse("consumer");
    }
}
