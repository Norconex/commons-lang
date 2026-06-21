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

import com.norconex.commons.lang.bean.BeanMapper.BeanMapperBuilder;
import com.norconex.commons.lang.config.Configurable;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.bean.BeanDeserializerBase;
import tools.jackson.databind.module.SimpleModule;

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

    static class FailOnDeserializerModifier extends ValueDeserializerModifier {
        private static final long serialVersionUID = 1L;
        private final BeanMapper beanMapper;

        public FailOnDeserializerModifier(BeanMapper beanMapper) {
            this.beanMapper = beanMapper;
        }

        @Override
        public ValueDeserializer<?> modifyDeserializer(
                DeserializationConfig config,
                BeanDescription.Supplier beanDesc,
                ValueDeserializer<?> deserializer) {
            if ((deserializer instanceof BeanDeserializerBase bdb)
                    && Configurable.class.isAssignableFrom(
                            beanDesc.get().getClassInfo().getRawType())) {
                return new FailOnBeanDeserializer(bdb, beanMapper);
            }
            return deserializer;
        }
    }

    static class FailOnBeanDeserializer extends ValueDeserializer<Object> {
        private static final long serialVersionUID = 1L;
        private final BeanDeserializerBase delegate;
        private final BeanMapper beanMapper;

        public FailOnBeanDeserializer(
                BeanDeserializerBase src, BeanMapper beanMapper) {
            this.delegate = src;
            this.beanMapper = beanMapper;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) {
            return deserializeInto(
                    p,
                    (Configurable<?>) delegate.getValueInstantiator()
                            .createUsingDefaultOrWithoutArguments(ctxt));
        }

        // Honor read-into-existing (readerForUpdating) so callers that read
        // into an existing Configurable instance get that instance populated.
        // Without this, the base class falls back to the create variant and
        // the caller's instance is left untouched.
        @Override
        public Object deserialize(
                JsonParser p, DeserializationContext ctxt, Object intoValue) {
            return deserializeInto(p, (Configurable<?>) intoValue);
        }

        private Object deserializeInto(
                JsonParser p, Configurable<?> configurable) {
            var configuration = configurable.getConfiguration();
            var node = p.readValueAsTree();
            // Re-read the config with a JSON-based mapper that carries the XML
            // structure modules, so unannotated collection/map properties are
            // unwrapped (no @JsonXmlCollection/@JsonXmlMap required) while
            // annotated ones keep working.
            var mapper = beanMapper.configReparseMapper();
            mapper.readerForUpdating(configuration).readValue(node.toString());
            return configurable;
        }
    }
}
