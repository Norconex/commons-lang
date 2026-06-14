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

import java.util.function.Consumer;

import com.norconex.commons.lang.flow.FlowMapperConfig;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.ValueSerializerModifier;

@RequiredArgsConstructor
public class FlowSerializerModifier extends ValueSerializerModifier {

    private static final long serialVersionUID = 1L;

    private final FlowMapperConfig flowMapperConfig;

    @Override
    public ValueSerializer<?> modifySerializer(
            SerializationConfig config,
            Supplier beanDesc,
            ValueSerializer<?> serializer) {
        var beanClass = beanDesc.get().getBeanClass();

        Class<?> consumerType =
                flowMapperConfig.getConsumerType().getBaseType();
        if (consumerType != null
                && consumerType.getClass().isAssignableFrom(
                        beanClass)
                || Consumer.class.isAssignableFrom(beanClass)) {
            return new FlowSerializer<>(flowMapperConfig, serializer);
        }
        return super.modifySerializer(config, beanDesc, serializer);
    }
}
