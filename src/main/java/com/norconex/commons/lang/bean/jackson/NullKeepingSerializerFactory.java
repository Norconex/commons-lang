/* Copyright 2026 Norconex Inc.
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

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.cfg.SerializerFactoryConfig;
import tools.jackson.databind.ser.BeanSerializerFactory;
import tools.jackson.databind.ser.PropertyBuilder;
import tools.jackson.databind.ser.SerializerFactory;

/**
 * Jackson's bean serializer factory, building its properties with a
 * {@link NullKeepingPropertyBuilder}. Install it on a mapper builder with
 * <code>serializerFactory(NullKeepingSerializerFactory.INSTANCE)</code>,
 * <em>before</em> adding modules (modules that add serializers derive a new
 * factory from the one installed, which this class makes keep its type).
 */
public class NullKeepingSerializerFactory extends BeanSerializerFactory {

    private static final long serialVersionUID = 1L;

    /** The factory to install. */
    public static final NullKeepingSerializerFactory INSTANCE =
            new NullKeepingSerializerFactory(null);

    protected NullKeepingSerializerFactory(SerializerFactoryConfig config) {
        super(config);
    }

    @Override
    public SerializerFactory withConfig(SerializerFactoryConfig config) {
        if (_factoryConfig == config) {
            return this;
        }
        // Required of any subclass: the base class would otherwise return a
        // plain BeanSerializerFactory and quietly drop our property builder
        // whenever a module registers serializers.
        return new NullKeepingSerializerFactory(config);
    }

    @Override
    protected PropertyBuilder constructPropertyBuilder(
            SerializationConfig config,
            BeanDescription.Supplier beanDescRef) {
        return new NullKeepingPropertyBuilder(config, beanDescRef.get());
    }
}
