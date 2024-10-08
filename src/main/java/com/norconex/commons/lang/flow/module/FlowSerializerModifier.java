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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.norconex.commons.lang.flow.FlowMapperConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FlowSerializerModifier extends BeanSerializerModifier {

    private static final long serialVersionUID = 1L;

    private final FlowMapperConfig flowMapperConfig;

    @Override
    public JsonSerializer<?> modifySerializer(
            SerializationConfig config,
            BeanDescription beanDesc,
            JsonSerializer<?> serializer) {

        Class<?> consumerType =
                flowMapperConfig.getConsumerType().getBaseType();
        if (consumerType != null
                && consumerType.getClass().isAssignableFrom(
                        beanDesc.getBeanClass())
                || Consumer.class.isAssignableFrom(beanDesc.getBeanClass())) {
            return new FlowSerializer<>(flowMapperConfig, serializer);
        }
        return super.modifySerializer(config, beanDesc, serializer);
    }
}
