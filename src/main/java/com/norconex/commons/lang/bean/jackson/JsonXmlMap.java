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
package com.norconex.commons.lang.bean.jackson;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;

import com.norconex.commons.lang.bean.BeanMapper;

/**
 * <p>
 * This annotation must be used with the {@link JsonXmlMapModule}.
 * The module is registered automatically for XML source if you are using
 * {@link BeanMapper}.
 * Use this annotation only when you wish to overwrite default settings
 * via its attributes. It is otherwise of no use.
 * </p>
 * @since 3.0.0
 */
@Retention(RUNTIME)
@Target({ TYPE, FIELD })
public @interface JsonXmlMap {

    /**
     * Field name to use for each map entries when serializing as XML.
     * This name does not affect reading. The default behavior uses
     * basic heuristics to detect whether the collection property name is
     * plural and can make a singular variant of it. It it can't figure it out,
     * it falls back to "entry".  Given the plural detection logic's simplicity,
     * it won't always get it right. Use this attribute to ensure the exact
     * name you want.
     * @return entry field name, when writing XML
     */
    public String entryName() default "";

    /**
     * Field name to use for each map entry keys when serializing as XML.
     * Default is "key".
     * @return entry key field name, when writing XML
     */
    public String keyName() default "key";

    /**
     * Field name to use for each map entry values when serializing as XML.
     * Default is "value".
     * @return entry value field name, when writing XML
     */
    public String valueName() default "value";

    /**
     * Concrete Map type to use when deserializing. Has to be assignable
     * to the type of your Map property.  Default will try to detect and
     * use {@link HashMap} as fallback.
     * @return map concrete type
     */
    public Class<?> mapType() default Void.class;

    /**
     * Concrete key type to use when deserializing. Has to be assignable
     * to the type of your Map entry key property.
     * Default will try to detect.
     * @return map entry key concrete type
     */
    public Class<?> keyType() default Void.class;

    /**
     * Concrete value type to use when deserializing. Has to be assignable
     * to the type of your Map entry value property.
     * Default will try to detect.
     * @return map entry value concrete type
     */
    public Class<?> valueType() default Void.class;
}
