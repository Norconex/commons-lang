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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.norconex.commons.lang.flow.FlowMapperConfig;

import lombok.RequiredArgsConstructor;

/**
 * Flow serializer.
 *
 * @param <T> type type of object being serialized
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class FlowSerializer<F extends Consumer<T>, T>
    extends JsonSerializer<F> {

    private final FlowMapperConfig config;

    @Override
    public void serialize(F value,
            JsonGenerator gen, SerializerProvider sp) throws IOException {
     //   gen.writeString(GenericConverter.convert(value));

        //TODO ensure order is preserved when writing

    }
}
