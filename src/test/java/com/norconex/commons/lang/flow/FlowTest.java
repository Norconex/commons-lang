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
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.flow.mock.MockConsumerBase;
import com.norconex.commons.lang.flow.mock.MockFlowConsumerAdapter;
import com.norconex.commons.lang.flow.mock.MockFlowPredicateAdapter;
import com.norconex.commons.lang.flow.mock.MockLowercaseConsumer;
import com.norconex.commons.lang.flow.mock.MockMapSizeEqualsCondition;
import com.norconex.commons.lang.flow.mock.MockPredicateBase;
import com.norconex.commons.lang.flow.mock.MockPropertyMatcherCondition;
import com.norconex.commons.lang.flow.mock.MockUppercaseConsumer;
import com.norconex.commons.lang.function.Consumers;
import com.norconex.commons.lang.function.PredicatedConsumer;
import com.norconex.commons.lang.function.Predicates;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;

import lombok.Data;

class FlowTest {

    private static BeanMapper beanMapper;

    @BeforeAll
    static void beforeAll() {
        //TODO like XMLFlow, allow for default/fallback types
        //(so class does not always need to be specified)?

        var flowCfg = new FlowMapperConfig();
        // testing with a base type not being FlowCondition
        flowCfg.getPredicateType().setBaseType(MockPredicateBase.class);
        flowCfg.getPredicateType().setAdapterType(
                MockFlowPredicateAdapter.class);
        flowCfg.getPredicateType().setScanFilter(
                c -> c.startsWith("com.norconex."));

        flowCfg.getConsumerType().setBaseType(MockConsumerBase.class);
        flowCfg.getConsumerType().setAdapterType(MockFlowConsumerAdapter.class);
        flowCfg.getConsumerType().setScanFilter(
                c -> c.startsWith("com.norconex."));

        beanMapper = BeanMapper.builder()
            .flowMapperConfig(flowCfg)
            .indent(true)
            .build();
    }


    @ParameterizedTest
    @EnumSource(Format.class)
    void testFlowRead(Format format) throws IOException {

        TestFlowConfig cfg;
        try (var r = ResourceLoader.getReader(
                getClass(), "." + format.toString().toLowerCase())) {
            cfg = beanMapper.read(TestFlowConfig.class, r, format);
        }

        var c = cfg.getFlowTest();
        assertFlowConsumed(c);
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void testFlowWriteRead(Format format) throws IOException {
        var cfg = new TestFlowConfig();
        cfg.setFlowTest(createPropertiesFlowAsRead());

        beanMapper.assertWriteRead(cfg, format);

//        try (var w = new StringWriter()) {
//            beanMapper.write(cfg, w, format);
//
//            //TODO replace with assertion:
//            System.out.println(w);
//        }
    }

    @SuppressWarnings("unchecked")
    private Consumer<Properties> createPropertiesFlowAsRead() {
        return Consumers.of(

            // if
            PredicatedConsumer.ifTrue(
                // condition
                propertyMatcherCondition("car", "volvo"),
                // then
                Consumers.of(
                    adapt(new MockUppercaseConsumer().setField("firstName"))
                ),
                // else
                Consumers.of(
                    adapt(new MockUppercaseConsumer().setField("lastName"))
                )
            ),

            // consumer
            adapt(new MockLowercaseConsumer().setField("IdontExist")),

            // if
            PredicatedConsumer.ifTrue(
                // allOf
                Predicates.allOf(
                    // condition
                    adapt(new MockMapSizeEqualsCondition().setSize(3)),
                    // condition
                    propertyMatcherCondition("car", "toyota")
                ),
                // then
                Consumers.of(
                    // consumer
                    adapt(new MockLowercaseConsumer().setField("firstName")),
                    // ifNot
                    PredicatedConsumer.ifFalse(
                        // condition
                        propertyMatcherCondition("firstName", "john"),
                        // then
                        Consumers.of(
                            // consumer
                            adapt(new MockLowercaseConsumer()
                                    .setField("lastName")),
                            // consumer
                            adapt(new MockLowercaseConsumer()
                                    .setField("DoNothing1"))
                        ),
                        // else
                        Consumers.of(
                            // consumer
                            adapt(new MockUppercaseConsumer()
                                    .setField("lastName")),
                            // consumer
                            adapt(new MockUppercaseConsumer()
                                    .setField("DoNothing2"))
                        )
                    )
                )
            )
        );
    }

    private Predicate<Properties> adapt(MockPredicateBase p) {
        return MockFlowPredicateAdapter.wrap(p);
    }
    private Consumer<Properties> adapt(MockConsumerBase p) {
        return MockFlowConsumerAdapter.wrap(p);
    }

    private Predicate<Properties> propertyMatcherCondition(
            String name, String value) {
        return adapt(Configurable.configure(
                new MockPropertyMatcherCondition(),
                cfg -> cfg.setPropertyMatcher(
                        new PropertyMatcher(basic(name), basic(value)))));
    }

    void assertFlowConsumed(Consumer<Properties>  c) {
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
        @JsonFlow
        private Consumer<Properties> flowTest;
    }
}
