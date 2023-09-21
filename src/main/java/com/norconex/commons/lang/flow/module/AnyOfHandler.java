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
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.flow.AnyOf;
import com.norconex.commons.lang.flow.FlowCondition;

class AnyOfHandler<T> implements StatementHandler<AnyOf<T>> {

    @SuppressWarnings("unchecked")
    @Override
    public AnyOf<T> read(FlowMapperConfig config, JsonParser p, JsonNode node)
            throws IOException {

        var anyOf = new AnyOf<T>();
        FlowUtil.forEachArrayObjectFields(node, (propName, propValue) -> {
            if (!Statement.isAnyOf(propName,
                    Statement.CONDITION, Statement.ALLOF, Statement.ANYOF)) {
                throw new IOException("""
                        Only <condition>, <allOf>, and <anyOf> are \
                        permitted as direct child elements of <anyOf>. \
                        Got instead: "%s"
                        """.formatted(propName));
            }

            // propValue is likely an array
            FlowUtil.forEachArrayNodes(propValue, childNode -> anyOf.add(
                    (FlowCondition<T>) Statement.of(propName).handler().read(
                            config, p, childNode))
            );
        });
        return anyOf;
    }

    @Override
    public void write() throws IOException {
        //TODO
    }
}
