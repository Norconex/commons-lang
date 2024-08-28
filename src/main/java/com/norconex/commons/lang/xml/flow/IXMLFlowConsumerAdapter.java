/* Copyright 2021-2022 Norconex Inc.
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
package com.norconex.commons.lang.xml.flow;

import java.util.function.Consumer;

/**
 * Adapts flow consuming elements so they can be treated as {@link Consumer}.
 * @param <T> type of the object to be submitted to the flow.
 * @since 2.0.0
 * @deprecated Use {@link XMLFlowConsumerAdapter} instead.
 */
@Deprecated(since = "3.0.0")
public interface IXMLFlowConsumerAdapter<T> //NOSONAR
        extends XMLFlowConsumerAdapter<T> {
}