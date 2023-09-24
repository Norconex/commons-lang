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

import static com.norconex.commons.lang.text.TextMatcher.basic;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.flow.mock.MockConsumerBase;
import com.norconex.commons.lang.flow.mock.MockFlowConditionAdapter;
import com.norconex.commons.lang.flow.mock.MockFlowInputConsumerAdapter;
import com.norconex.commons.lang.flow.mock.MockLowercaseConsumer;
import com.norconex.commons.lang.flow.mock.MockMapSizeEqualsCondition;
import com.norconex.commons.lang.flow.mock.MockPredicateBase;
import com.norconex.commons.lang.flow.mock.MockPropertyMatcherCondition;
import com.norconex.commons.lang.flow.mock.MockUppercaseConsumer;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;

import lombok.Data;

class FlowTest {

//    @ParameterizedTest
//    @EnumSource(Format.class)
//    void testFlow(Format format) {
//    }
    //TODO make it work with a yaml + json as well

    @Test
    void testFlow() throws IOException {


        //TODO like XMLFlow, allow for default/fallback types
        //(so class does not always need to be specified)?

        var flowCfg = new FlowMapperConfig();
        // testing with a base type not being FlowCondition
        flowCfg.setConditionType(MockPredicateBase.class);
        flowCfg.setConditionAdapterType(MockFlowConditionAdapter.class);
        flowCfg.setConditionScanFilter(c -> c.startsWith("com.norconex."));

        flowCfg.setInputConsumerType(MockConsumerBase.class);
        flowCfg.setInputConsumerAdapterType(MockFlowInputConsumerAdapter.class);
        flowCfg.setInputConsumerScanFilter(c -> c.startsWith("com.norconex."));

        var bm = BeanMapper.builder()
            .flowMapperConfig(flowCfg)
            .build();

        TestFlowConfig cfg;
        try (var r = ResourceLoader.getXmlReader(getClass())) {
            cfg = bm.read(TestFlowConfig.class, r, Format.XML);
        }

        Consumer<Properties> c = cfg.getFlowTest();
        assertFlow(c);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testProgrammaticFlow() throws IOException {

        //TODO assert write/read and compare with XML/Yaml/JSON?

        // Build the same config as XML, programatically and do same tests.
        Consumer<Properties> c = Flow.<Properties>of(

            If.<Properties>builder()
                .condition(propertyMatcherCondition("car", "volvo"))
                .thenConsumer(new MockUppercaseConsumer().setField("firstName"))
                .elseConsumer(new MockUppercaseConsumer().setField("lastName"))
                .build(),

            new MockLowercaseConsumer().setField("IdontExist"),

            If.<Properties>builder()
                .condition(Group.ofAll(
                        new MockMapSizeEqualsCondition().setSize(3),
                        propertyMatcherCondition("car", "toyota")))
                .thenConsumer(Flow.of(
                        new MockLowercaseConsumer().setField("firstName"),
                        If.<Properties>builder()
                            .negate(true)
                            .condition(propertyMatcherCondition(
                                    "firstName", "john"))
                            .thenConsumer(new MockLowercaseConsumer()
                                    .setField("lastName"))
                            .elseConsumer(new MockUppercaseConsumer()
                                    .setField("lastName"))
                            .build()
                        ))
                .build()
        );

        assertFlow(c);
    }

    private MockPropertyMatcherCondition propertyMatcherCondition(
            String name, String value) {
        return Configurable.configure(
                new MockPropertyMatcherCondition(),
                cfg -> cfg.setPropertyMatcher(
                        new PropertyMatcher(basic(name), basic(value))));
    }

    void assertFlow(Consumer<Properties>  c) {
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
