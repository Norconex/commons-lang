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
import java.util.function.Predicate;

import com.fasterxml.jackson.core.JsonToken;
import com.norconex.commons.lang.flow.module.FlowDeserializer.FlowDeserContext;
import com.norconex.commons.lang.flow.module.FlowSerializer.FlowSerContext;
import com.norconex.commons.lang.function.Predicates;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Handles flow condition group matching either "anyOf" or "allOf".
 *
 * @param <T> type object type evaluated by a group of conditions
 * @since 3.0.0
 */
@Data
@RequiredArgsConstructor
class ConditionGroupHandler<T> implements StatementHandler<Predicate<T>> {

    private final boolean any;

    @Override
    public Predicate<T> read(FlowDeserContext ctx) throws IOException {
        var p = ctx.getParser();
        var preds = new Predicates<T>(any);
        FlowUtil.logOpen(ctx, any ? "anyOf" : "allOf");
        p.nextToken(); // START_ARRAY or START_OBJECT

        if (p.currentToken() == JsonToken.START_ARRAY) {
            whileInArrayObjects(p, () -> readObject(ctx, preds));
        } else {
            whileInObject(p, () -> readObject(ctx, preds));
        }

        FlowUtil.logClose(ctx, p.currentName());
        return preds;
    }

    @SuppressWarnings("unchecked")
    private void readObject(FlowDeserContext ctx, Predicates<T> preds)
            throws IOException {
        var p = ctx.getParser();
        FlowUtil.logOpen(ctx, p.currentName());
        var fieldName = p.currentName();
        if (!Statement.isAnyOf(fieldName,
                Statement.CONDITION,
                Statement.ALLOF,
                Statement.ANYOF)) {
            throw new IOException("""
                    Only <condition>, <allOf>, and <anyOf> are \
                    permitted as direct child elements of <if> and
                    <ifNot>. Got instead: "%s"
                    """.formatted(fieldName));
        }
        preds.add((Predicate<T>) Statement.of(fieldName)
                .handler().read(ctx));
        FlowUtil.logClose(ctx, p.currentName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(Predicate<T> predicate, FlowSerContext ctx)
            throws IOException {
        var gen = ctx.getGen();

        gen.writeFieldName(isAny() ? "anyOf" : "allOf");

        var predicateGroup = (Predicates<T>) predicate;
        FlowUtil.writeArrrayOfObjects(predicateGroup, ctx, pred -> {
            if (pred instanceof Predicates<T> chidPredGroup) {
                if (chidPredGroup.isAny()) {
                    // anyOf
                    ((ConditionGroupHandler<T>) Statement.ANYOF
                            .handler()).write(chidPredGroup, ctx);
                } else {
                    // allOf
                    ((ConditionGroupHandler<T>) Statement.ALLOF
                            .handler()).write(chidPredGroup, ctx);
                }
            } else {
                // condition
                ((ConditionHandler<T>) Statement.CONDITION
                        .handler()).write(pred, ctx);
            }
        });
    }
}
