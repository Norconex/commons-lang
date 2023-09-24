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

import java.util.function.Consumer;

import com.norconex.commons.lang.bean.BeanMapper.FlowConsumerAdapter;
import com.norconex.commons.lang.map.Properties;

import lombok.Data;

@Data
public final class MockFlowInputConsumerAdapter
        implements FlowConsumerAdapter<Properties> {

    private Consumer<Properties> rawInputConsumer;

    @Override
    public void accept(Properties t) {
        rawInputConsumer.accept(t);
    }

    @Override
    public Object getConsumerAdaptee() {
        return rawInputConsumer;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void setConsumerAdaptee(Object rawInputConsumer) {
        this.rawInputConsumer = (Consumer<Properties>) rawInputConsumer;

    }
}