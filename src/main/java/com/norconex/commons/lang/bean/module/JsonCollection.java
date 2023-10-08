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
package com.norconex.commons.lang.bean.module;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Use this annotation on your collections to make sure they are written
 * and read consistently across types (XML, JSON, Yaml).
 * This is particularly useful for XML, as Jackson XML mapper
 * is not always able to read what it wrote.
 * @since 3.0.0
 */
@Retention(RUNTIME)
@Target({ FIELD })
@JacksonAnnotationsInside
@JsonSerialize(using = JsonCollectionSerializer.class)
public @interface JsonCollection {

    /**
     * Field name to use for each collection element when serializing as XML.
     * This name does not affect reading. The default behavior tries uses
     * basic heuristics to detect whether the collection property name is
     * plural and can make a singular variant of it. It it can't figure it out,
     * it falls back to "entry".  Given the plural detection logic's simplicity,
     * it won't always get it right. Use this attribute to ensure the exact
     * name you want.
     * @return entry field name, when writing XML
     */
    public String entryName() default "";
}
