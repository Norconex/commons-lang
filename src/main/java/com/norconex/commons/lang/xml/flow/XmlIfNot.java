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

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>
 * Behaves the same as <code>XMLIf</code>, reversing matching logic.
 * </p>
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
class XmlIfNot<T> extends XmlIf<T> {

    XmlIfNot(XmlFlow<T> flow) {
        super(flow);
    }

    @Override
    protected boolean conditionPasses(T t) {
        return !super.conditionPasses(t);
    }
}
