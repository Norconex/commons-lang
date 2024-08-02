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
package com.norconex.commons.lang.flow;

import java.util.function.Consumer;
import java.util.function.Function;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FlowMapperConfig {
    private final FlowPolymorphicType<FlowPredicateAdapter<?>> predicateType =
            new FlowPolymorphicType<>();
    private final FlowPolymorphicType<FlowConsumerAdapter<?>> consumerType =
            new FlowPolymorphicType<>();
    /**
     * Provide a custom name to be used when a consumer is being serialized.
     * When not specified or returning <code>null</code>, the default is
     * "consumer".
     * @param consumerNameProvider name provider
     * @return consumer name
     */
    @SuppressWarnings("javadoc")
    private Function<Consumer<?>, String> consumerNameProvider;
}