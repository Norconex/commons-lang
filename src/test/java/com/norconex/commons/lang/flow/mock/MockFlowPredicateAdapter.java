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
package com.norconex.commons.lang.flow.mock;

import java.util.function.Predicate;

import com.norconex.commons.lang.flow.FlowPredicateAdapter;
import com.norconex.commons.lang.map.Properties;

import lombok.Data;

@Data
public final class MockFlowPredicateAdapter
        implements FlowPredicateAdapter<Properties> {

    private MockPredicateBase rawCondition;

    public static Predicate<Properties> wrap(MockPredicateBase rawCondition) {
        var adapter = new MockFlowPredicateAdapter();
        adapter .setPredicateAdaptee(rawCondition);
        return adapter;
    }

    @Override
    public boolean test(Properties props) {
        return rawCondition.isPropertiesConditionMet(props);
    }
    @Override
    public MockPredicateBase getPredicateAdaptee() {
        return rawCondition;
    }
    @Override
    public void setPredicateAdaptee(Object rawCondition) {
        this.rawCondition = (MockPredicateBase) rawCondition;
    }
}