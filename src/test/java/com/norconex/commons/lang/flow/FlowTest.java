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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.flow.mock.MockConditionBase;
import com.norconex.commons.lang.flow.mock.MockFlowConditionAdapter;
import com.norconex.commons.lang.flow.mock.MockFlowInputConsumerAdapter;
import com.norconex.commons.lang.map.Properties;

import lombok.Data;

class FlowTest {

//    @ParameterizedTest
//    @EnumSource(Format.class)
//    void testFlow(Format format) {
//    }
    //TODO make it work with a yaml + json as well

    @Test
    void testFlow() throws IOException {

        //TODO move flow mapper config out, and simplify it
        // with having:
        //   FlowMapperConfig
        //      conditionConfig: MapperConfig
        //      inputConsumerConfig: MapperConfig
        // ?

        //TODO like XMLFlow, allow for default/fallback types
        //(so class does not always need to be specified)?

        var flowCfg = new FlowMapperConfig();
        // testing with a base type not being FlowCondition
        flowCfg.setConditionType(MockConditionBase.class);
        flowCfg.setConditionAdapterType(MockFlowConditionAdapter.class);
        flowCfg.setConditionScanFilter(c -> c.startsWith("com.norconex."));

        flowCfg.setInputConsumerType(FlowInputConsumer.class);
        flowCfg.setInputConsumerAdapterType(MockFlowInputConsumerAdapter.class);
        flowCfg.setInputConsumerScanFilter(c -> c.startsWith("com.norconex."));

        var bm = BeanMapper.builder()
            .flowMapperConfig(flowCfg)
            .build();

        //Flow<Properties> c;
        TestFlowConfig cfg;
        try (var r = ResourceLoader.getXmlReader(getClass())) {
            cfg = bm.read(TestFlowConfig.class, r, Format.XML);
        }

        Consumer<Properties> c = cfg.getFlowTest();

        var data1 = new Properties();
        data1.add("firstName", "John");
        data1.add("lastName", "Smith");
        data1.set("car", "volvo");
        c.accept(data1);

        // first name uppercase
        assertThat(data1.getString("firstName")).isEqualTo("JOHN");
        // last name unchanged
        assertThat(data1.getString("lastName")).isEqualTo("Smith");

        var data2 = new Properties();
        data2.add("firstName", "John");
        data2.add("lastName", "Smith");
        data2.set("car", "toyota");
        c.accept(data2);

        // first name lowercase
        assertThat(data2.getString("firstName")).isEqualTo("john");
        // last name uppercase
        assertThat(data2.getString("lastName")).isEqualTo("SMITH");

    }

    @Data
    static class TestFlowConfig {
        private Flow<Properties> flowTest;
    }
}
