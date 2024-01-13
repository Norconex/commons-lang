/* Copyright 2024 Norconex Inc.
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
package com.norconex.commons.lang.bean;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.norconex.commons.lang.bean.BeanMapper.BeanMapperBuilder;
import com.norconex.commons.lang.config.Configurable;

/**
 * This Jackson module is a hack to work around Jackson forcing
 * "ignoreAllUnknown" on {@literal @}JsonUnwrapped components (see
 * <a href="https://github.com/FasterXML/jackson-databind/issues/650">Jackson
 * issue</a>).
 *
 * For Configurable classes, this will fail on unknown properties for the
 * configuration class. Not used for other classes, and not used when
 * {@link BeanMapperBuilder#ignoreUnknownProperties(boolean)}
 * is set to <code>true</code>.
 * @since 3.0.0
 */
class FailOnUnknownConfigurablePropModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public FailOnUnknownConfigurablePropModule(BeanMapper beanMapper) {
        setDeserializerModifier(new FailOnDeserializerModifier(beanMapper));
    }

    static class FailOnDeserializerModifier extends BeanDeserializerModifier {
        private final BeanMapper beanMapper;
        public FailOnDeserializerModifier(BeanMapper beanMapper) {
            this.beanMapper = beanMapper;
        }

        @Override
        public JsonDeserializer<?> modifyDeserializer(
                DeserializationConfig config,
                BeanDescription beanDesc,
                JsonDeserializer<?> deserializer) {
            if ((deserializer instanceof BeanDeserializerBase bdb)
                    && Configurable.class.isAssignableFrom(
                            beanDesc.getClassInfo().getRawType())) {
                return new FailOnBeanDeserializer(bdb, beanMapper);
            }
            return deserializer;
        }
    }

    static class FailOnBeanDeserializer extends BeanDeserializer {
        private static final long serialVersionUID = 1L;

        public FailOnBeanDeserializer(
                BeanDeserializerBase src, BeanMapper beanMapper) {
            super(src);
        }
        @Override
        public Object deserializeFromObject(JsonParser p,
                DeserializationContext ctxt) throws IOException {
            var configurable = (Configurable<?>) getValueInstantiator()
                    .createUsingDefaultOrWithoutArguments(ctxt);
            var configuration = configurable.getConfiguration();
            var node = p.readValueAsTree();
            var mapper = (ObjectMapper) p.getCodec();
            var originalFormatString = mapper.writeValueAsString(node);
            mapper.readerForUpdating(configuration)
                .readValue(originalFormatString);
            return configurable;
        }
    }
}
