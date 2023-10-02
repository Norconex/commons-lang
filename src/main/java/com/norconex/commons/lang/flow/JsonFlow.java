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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Typing;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.flow.module.FlowModule;

/**
 * When adding the {@link FlowModule} to Jackson, use this annotation
 * on {@link Consumer} properties to support flow (de)serialization.
 * {@link BeanMapper} automatically registers the {@link FlowModule}.
 */
@Retention(RUNTIME)
@Target({ TYPE, FIELD })
@JacksonAnnotationsInside
@JsonSerialize(typing = Typing.STATIC)
public @interface JsonFlow {

}
