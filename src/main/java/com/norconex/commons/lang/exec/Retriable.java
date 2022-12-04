/* Copyright 2010-2022 Norconex Inc.
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
package com.norconex.commons.lang.exec;

/**
 * Upon failure, the <code>run</code> method will get
 * re-executed by a {@link Retrier} until successful or fails according
 * to the {@link Retrier} conditions.
 * @param <T> type of optional return value
 * @see Retrier
 * @since 3.0.0 (renamed from <code>IRetriable</code>)
 */
@FunctionalInterface
public interface Retriable<T> {
    /**
     * Code to be executed until successful (no exception thrown).
     * @throws Exception any exception
     * @return optional return value
     */
    T execute() throws Exception; //NOSONAR
}
