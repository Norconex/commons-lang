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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.bean.BeanMapper.FlowInputConsumerAdapter;
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.flow.FlowInputConsumer;
import com.norconex.commons.lang.function.Consumers;

@SuppressWarnings("unchecked")
class RootHandler<T> implements StatementHandler<Consumer<T>> {

    @Override
    public Consumer<T> read(
            FlowMapperConfig config, JsonParser p, JsonNode node)
                    throws IOException {

        if (node == null) {
            return null;
        }

        List<Consumer<T>> consumers = new ArrayList<>();
        FlowUtil.forEachArrayObjectFields(node, (propName, propValue) -> {
            var statement = Statement.of(propName);
            if (statement == null) {
                consumers.add(readInputConsumer(config, p, propValue ));
            } else if (statement.isAnyOf(Statement.IF, Statement.IFNOT)) {
                consumers.add((Consumer<T>)
                        statement.handler().read(config, p, propValue ));
            } else {
                throw new IOException("<" + statement + "> is misplaced.");
            }
        });

        if (consumers.isEmpty()) {
            return null;
        }
        if (consumers.size() == 1) {
            return consumers.get(0);
        }
        return new Consumers<>(consumers);
    }

    private Consumer<T> readInputConsumer(
            FlowMapperConfig config, JsonParser p, JsonNode node)
                    throws IOException {
        Class<?> type = config.getInputConsumerType();
        if (type == null) {
            type = FlowInputConsumer.class;
        } else if (!FlowInputConsumer.class.isAssignableFrom(type)
                && config.getConditionAdapterType() == null) {
            throw new IOException("""
                Cannot have a flow consumer type that does not implement \
                Consumer without a FlowInputConsumerAdapter.""");
        }

        var mapper = (ObjectMapper) p.getCodec();
        var consumer = mapper.treeToValue(node, type);
        if (config.getInputConsumerAdapterType() != null) {
            var adapter = (FlowInputConsumerAdapter<T>)
                    ClassUtil.newInstance(config.getInputConsumerAdapterType());
            adapter.setRawInputConsumer(consumer);
            return adapter;
        }
        // at this point it has to be a condition or fail.
        return (FlowInputConsumer<T>) consumer;
    }

    @Override
    public void write() throws IOException {
        //TODO
    }
}
