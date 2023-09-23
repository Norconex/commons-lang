/* Copyright 2021-2023 Norconex Inc.
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

import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.map.Properties;

import lombok.Data;

/**
 * A simple condition matching {@link Properties} keys and values.
 */
@Data
public class MockPropertyMatcherCondition
        implements MockConditionBase,
        Configurable<MockPropertyMatcherConditionConfig> {

    private final MockPropertyMatcherConditionConfig configuration =
            new MockPropertyMatcherConditionConfig();

    @Override
    public boolean test(Properties props) {
        return configuration.getPropertyMatcher().matches(props);
    }
}
