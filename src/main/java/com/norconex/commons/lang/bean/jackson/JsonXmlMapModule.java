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
package com.norconex.commons.lang.bean.jackson;

import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.norconex.commons.lang.bean.BeanMapper;

/**
 * Jackson module providing (de)serializers for Map, so they can be
 * written and read as XML without special hacks.
 * Already registered in {@link BeanMapper}.
 * @since 3.0.0
 */
public class JsonXmlMapModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public JsonXmlMapModule() {
        this.addSerializer((Class<Map<?, ?>>) (Class<?>) Map.class,
                new JsonXmlMapSerializer<>());
        this.addDeserializer((Class<Map<?, ?>>) (Class<?>) Properties.class,
                new JsonXmlMapDeserializer<>());
        this.addDeserializer(Map.class, new JsonXmlMapDeserializer<>());
    }
}