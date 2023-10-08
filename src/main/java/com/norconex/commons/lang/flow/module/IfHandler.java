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
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.JsonToken;
import com.norconex.commons.lang.flow.module.FlowDeserializer.FlowDeserContext;
import com.norconex.commons.lang.flow.module.FlowSerializer.FlowSerContext;
import com.norconex.commons.lang.function.PredicatedConsumer;
import com.norconex.commons.lang.function.Predicates;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Handles flow "if" and "ifNot".
 *
 * @param <T> type object type evaluated
 * @since 3.0.0
 */
@Data
@RequiredArgsConstructor
class IfHandler<T> implements StatementHandler<Consumer<T>> {

    private final boolean negate;

    @Override
    public Consumer<T> read(FlowDeserContext ctx)
            throws IOException {

        var p = ctx.getParser();
        var args = new Args<T>();

        p.nextToken(); // <-- START_OBJECT
        while ((p.nextToken()) != JsonToken.END_OBJECT) { // <-- FIELD_NAME
            var name = p.getCurrentName();

            var st = Statement.of(name);
            if (st == null) {
                badChildren("Invalid element: <%s>".formatted(name));
            }
            switch (st) {
                case CONDITION, ALLOF, ANYOF -> args.setCondition(
                        st.handler().read(ctx), name);
                case THEN -> args.setThenConsumer(
                        st.handler().read(ctx));
                case ELSE -> args.setElseConsumer(
                        st.handler().read(ctx));
                default -> badChildren("Got instead: <%s>".formatted(name));
            }
        }

        if (args.condition == null) {
            badChildren("Missing one of <condition>, <anyOf>, or <allOf>.");
        }
        if (args.thenConsumer == null) {
            badChildren("Missing element: <then>.");
        }

        return new PredicatedConsumer<>(
                args.condition,
                args.thenConsumer,
                args.elseConsumer,
                negate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(Consumer<T> obj, FlowSerContext ctx) throws IOException {

        var gen = ctx.getGen();

        FlowUtil.writeArrayObjectWrap(ctx, () -> {
            gen.writeFieldName(negate ? "ifNot" : "if");
            gen.writeStartObject();

            var predicatedConsumer = (PredicatedConsumer<T>) obj;
            var predicate = predicatedConsumer.getPredicate();

            if (predicate instanceof Predicates<T> predicateGroup) {
                if (predicateGroup.isAny()) {
                    // anyOf
                    ((ConditionGroupHandler<T>) Statement.ANYOF
                            .handler()).write(predicateGroup, ctx);
                } else {
                    // allOf
                    ((ConditionGroupHandler<T>) Statement.ALLOF
                            .handler()).write(predicateGroup, ctx);
                }
            } else {
                // condition
                ((ConditionHandler<T>) Statement.CONDITION.handler()).write(
                        predicatedConsumer.getPredicate(), ctx);
            }

            // then
            gen.writeFieldName(Statement.THEN.toString());
            ((RootHandler<T>) Statement.THEN.handler()).write(
                    predicatedConsumer.getThenConsumer(), ctx);

            // else
            if (predicatedConsumer.getElseConsumer() != null) {
                gen.writeFieldName(Statement.ELSE.toString());
                ((RootHandler<T>) Statement.ELSE.handler()).write(
                        predicatedConsumer.getElseConsumer(), ctx);
            }

            gen.writeEndObject();
        });
    }

    @SuppressWarnings("unchecked")
    static class Args<T> {
        private Predicate<T> condition;
        private Consumer<T> thenConsumer;
        private Consumer<T> elseConsumer;
        public void setCondition(
                Object condition, String name) throws IOException {
            if (this.condition != null) {
                badChildren("Element appearing more than once: <%s>"
                        .formatted(name));
            }
            this.condition = (Predicate<T>) condition;
        }
        public void setThenConsumer(Object thenConsumer) throws IOException {
            if (this.thenConsumer != null) {
                badChildren("Element appearing more than once: <then>");
            }
            this.thenConsumer = (Consumer<T>) thenConsumer;
        }
        public void setElseConsumer(Object elseConsumer) throws IOException {
            if (this.elseConsumer != null) {
                badChildren("Element appearing more than once: <else>");
            }
            this.elseConsumer = (Consumer<T>) elseConsumer;
        }
    }

    static void badChildren(String gotInstead) throws IOException {
        throw new IOException("""
                Exactly one of <condition>, <anyOf>, and <allOf>, \
                exactly one <then>, and zero or one <else> \
                are permitted as direct child elements of <if> and \
                <ifNot>. %s""".formatted(gotInstead));
    }
}
