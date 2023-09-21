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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.bean.BeanMapper.FlowConditionAdapter;
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.flow.FlowCondition;

class ConditionHandler<T> implements StatementHandler<FlowCondition<T>> {

    @SuppressWarnings("unchecked")
    @Override
    public FlowCondition<T> read(
            FlowMapperConfig config, JsonParser p, JsonNode node)
                    throws IOException {

        Class<?> type = config.getConditionType();
        if (type == null) {
            type = FlowCondition.class;
        } else if (!FlowCondition.class.isAssignableFrom(type)
                && config.getConditionAdapterType() == null) {
            throw new IOException("""
                Cannot have a flow condition type that\s\
                does not implement Condition without a\s\
                FlowConditionAdapter.""");
        }

        var mapper = (ObjectMapper) p.getCodec();
        var condition = mapper.treeToValue(node, type);
        if (config.getConditionAdapterType() != null) {
            var adapter = (FlowConditionAdapter<T>)
                    ClassUtil.newInstance(config.getConditionAdapterType());
            adapter.setRawCondition(condition);
            return adapter;
        }
        // at this point it has to be a condition or fail.
        return (FlowCondition<T>) condition;
    }

    @Override
    public void write() throws IOException {
        //TODO
    }

}
