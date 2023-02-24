/* Copyright 2014-2022 Norconex Inc.
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
package com.norconex.commons.lang.pipeline;

import java.util.function.Predicate;

/**
 * A logical step in the pipeline.  Typically an action to be executed.
 * @param <T> pipeline context type
 * @since 1.5.0
 * @deprecated Use {@link Predicate} instead.
 */
@Deprecated(since="3.0.0")
@FunctionalInterface
public interface IPipelineStage<T> extends Predicate<T> { // NOSONAR

    @Override
    default boolean test(T t) {
        return execute(t);
    }

    /**
     * Executes this pipeline stage.  Implementors are encouraged to throw
     * {@link PipelineException} upon errors.
     * @param context pipeline context
     * @return whether to continue pipeline execution or stop (<code>true</code>
     *         to continue).
     */
    boolean execute(T context);
}
