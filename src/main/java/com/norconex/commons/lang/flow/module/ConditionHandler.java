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

import java.io.IOException;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.flow.FlowPredicateAdapter;
import com.norconex.commons.lang.flow.module.FlowDeserializer.FlowDeserContext;
import com.norconex.commons.lang.flow.module.FlowSerializer.FlowSerContext;

/**
 * Handles flow conditions.
 *
 * @param <T> type object type evaluated by a condition
 * @since 3.0.0
 */
class ConditionHandler<T> implements StatementHandler<Predicate<T>> {

    @SuppressWarnings("unchecked")
    @Override
    public Predicate<T> read(FlowDeserContext ctx) throws IOException {

        var p = ctx.getParser();
        var config = ctx.getConfig();
        FlowUtil.logOpen(ctx, p.currentName());
        p.nextToken(); // <-- START_OBJECT

        Class<?> type = config.getPredicateType().getBaseType();
        if (type == null) {
            type = Predicate.class;
        } else if (!Predicate.class.isAssignableFrom(type)
                && config.getPredicateType().getAdapterType() == null) {
            throw new IOException("""
                Cannot have a flow condition type that\s\
                does not implement Condition without a\s\
                FlowConditionAdapter.""");
        }

        var mapper = (ObjectMapper) p.getCodec();
        var condition = mapper.readValue(p, type);
        if (config.getPredicateType().getAdapterType() != null) {
            var adapter = (FlowPredicateAdapter<T>) ClassUtil.newInstance(
                    config.getPredicateType().getAdapterType());
            adapter.setPredicateAdaptee(condition);
            condition = adapter;
        }
        // at this point it has to be a condition or fail.
        FlowUtil.logBody(ctx, condition);
        FlowUtil.logClose(ctx, p.currentName());

        return (Predicate<T>) condition;
    }

    @Override
    public void write(Predicate<T> obj, FlowSerContext ctx) throws IOException {
        var gen = ctx.getGen();
        gen.writeFieldName(Statement.CONDITION.toString());
        if (obj instanceof FlowPredicateAdapter<?> adapter) {
            gen.writeObject(adapter.getPredicateAdaptee());
        } else {
            gen.writeObject(obj);
        }
    }
}
