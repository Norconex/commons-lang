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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.flow.FlowCondition;
import com.norconex.commons.lang.flow.If;

class IfHandler<T> implements StatementHandler<If<T>> {

    @Override
    public If<T> read(FlowMapperConfig config, JsonParser p, JsonNode node)
            throws IOException {

        var args = new Args<T>();
        FlowUtil.forEachArrayObjectFields(node, (name, n) -> {
            var st = Statement.of(name);
            if (st == null) {
                badChildren("Invalid element: <%s>".formatted(name));
            }
            switch (st) {
                case CONDITION, ALLOF, ANYOF -> args.setCondition(
                        st.handler().read(config, p, n), name);
                case THEN -> args.setThenConsumer(
                        st.handler().read(config, p, n));
                case ELSE -> args.setElseConsumer(
                        st.handler().read(config, p, n));
                default -> badChildren("Invalid element: <%s>".formatted(name));
            }
        });

        if (args.flowCondition == null) {
            badChildren("Missing one of <condition>, <anyOf>, or <allOf>.");
        }
        if (args.thenConsumer == null) {
            badChildren("Missing element: <then>.");
        }

        return new If<>(args.flowCondition, args.thenConsumer, args.elseConsumer);
    }

    @Override
    public void write() throws IOException {
        //TODO
    }

    @SuppressWarnings("unchecked")
    static class Args<T> {
        private FlowCondition<T> flowCondition;
        private Consumer<T> thenConsumer;
        private Consumer<T> elseConsumer;
        public void setCondition(
                Object condition, String name) throws IOException {
            if (flowCondition != null) {
                badChildren("Element appearing more than once: <%s>"
                        .formatted(name));
            }
            flowCondition = (FlowCondition<T>) condition;
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
