/* Copyright 2024 Norconex Inc.
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
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.StringReader;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.flow.mock.MockConsumerBase;
import com.norconex.commons.lang.flow.mock.MockFlowConsumerAdapter;
import com.norconex.commons.lang.flow.mock.MockFlowPredicateAdapter;
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

class FlowEdgeCaseTest {

    private static BeanMapper beanMapper;

    @BeforeAll
    static void beforeAll() {
        beanMapper = buildMapper(true);
    }

    static BeanMapper buildMapper(boolean withAdapters) {
        var flowCfg = new FlowMapperConfig();
        flowCfg.getPredicateType().setBaseType(MockPredicateBase.class);
        if (withAdapters) {
            flowCfg.getPredicateType()
                    .setAdapterType(MockFlowPredicateAdapter.class);
        }
        flowCfg.getPredicateType()
                .setScanFilter(c -> c.startsWith("com.norconex."));
        flowCfg.getConsumerType().setBaseType(MockConsumerBase.class);
        if (withAdapters) {
            flowCfg.getConsumerType()
                    .setAdapterType(MockFlowConsumerAdapter.class);
        }
        flowCfg.getConsumerType()
                .setScanFilter(c -> c.startsWith("com.norconex."));
        flowCfg.setConsumerNameProvider(c -> "testConsumer");
        return BeanMapper.builder().flowMapperConfig(flowCfg).indent(true)
                .build();
    }

    @Data
    static class TestFlowConfig {
        @JsonFlow
        private Consumer<Properties> flowTest;
    }

    @Data
    static class DirectConsumerFlowConfig {
        @JsonFlow
        private Consumer<Properties> flowTest;
    }

    private Predicate<Properties> adaptPredicate(MockPredicateBase p) {
        return MockFlowPredicateAdapter.wrap(p);
    }

    private Consumer<Properties> adaptConsumer(MockConsumerBase c) {
        return MockFlowConsumerAdapter.wrap(c);
    }

    private MockPropertyMatcherCondition matchCondition(String field,
            String value) {
        return Configurable.configure(
                new MockPropertyMatcherCondition(),
                cfg -> cfg.setPropertyMatcher(
                        new PropertyMatcher(basic(field), basic(value))));
    }

    // Exercises IfHandler.write() anyOf branch
    @Test
    void testWriteReadWithAnyOf() {
        var cfg = new TestFlowConfig();
        cfg.setFlowTest(Consumers.of(
                PredicatedConsumer.ifTrue(
                        Predicates.anyOf(
                                adaptPredicate(matchCondition("car", "volvo")),
                                adaptPredicate(
                                        matchCondition("car", "toyota"))),
                        Consumers.of(adaptConsumer(
                                new MockUppercaseConsumer()
                                        .setField("name"))))));

        assertThatNoException().isThrownBy(
                () -> beanMapper.assertWriteRead(cfg, Format.JSON));
    }

    // Exercises ConditionGroupHandler.write() nested Predicates (anyOf inside allOf)
    @Test
    void testWriteReadWithNestedPredicateGroups() {
        var cfg = new TestFlowConfig();
        cfg.setFlowTest(Consumers.of(
                PredicatedConsumer.ifTrue(
                        Predicates.allOf(
                                Predicates.anyOf(
                                        adaptPredicate(
                                                matchCondition("car", "volvo")),
                                        adaptPredicate(matchCondition("car",
                                                "toyota"))),
                                adaptPredicate(
                                        matchCondition("status", "active"))),
                        Consumers.of(adaptConsumer(
                                new MockUppercaseConsumer()
                                        .setField("name"))))));

        assertThatNoException().isThrownBy(
                () -> beanMapper.assertWriteRead(cfg, Format.JSON));
    }

    // Exercises ConditionGroupHandler.read() whileInObject (single-element XML allOf)
    @Test
    void testXmlSingleConditionInAllOf() {
        var xml = """
                <xml>
                  <flowTest>
                    <if>
                      <allOf>
                        <condition class="com.norconex.commons.lang.flow.mock.MockPropertyMatcherCondition">
                          <propertyMatcher>
                            <fieldMatcher>car</fieldMatcher>
                            <valueMatcher>volvo</valueMatcher>
                          </propertyMatcher>
                        </condition>
                      </allOf>
                      <then>
                        <testConsumer class="com.norconex.commons.lang.flow.mock.MockUppercaseConsumer">
                          <field>firstName</field>
                        </testConsumer>
                      </then>
                    </if>
                  </flowTest>
                </xml>
                """;
        assertThatNoException().isThrownBy(() -> {
            var cfg = beanMapper.read(TestFlowConfig.class,
                    new StringReader(xml), Format.XML);
            assertThat(cfg.getFlowTest()).isNotNull();
        });
    }

    // Exercises ConditionGroupHandler.readObject() invalid-child error path
    @Test
    void testInvalidChildInConditionGroup() {
        var json = """
                {"flowTest":[{"if":{"anyOf":[{"bogusElement":{}}],\
                "then":[{"testConsumer":{"class":\
                "com.norconex.commons.lang.flow.mock.MockUppercaseConsumer","field":"x"}}]}}]}
                """;
        assertThatException().isThrownBy(() -> beanMapper.read(
                TestFlowConfig.class, new StringReader(json), Format.JSON));
    }

    // Exercises IfHandler.read() default badChildren for unrecognized element
    @Test
    void testIfWithInvalidElement() {
        var json = """
                {"flowTest":[{"if":{"condition":{"class":\
                "com.norconex.commons.lang.flow.mock.MockMapSizeEqualsCondition","size":3},\
                "bogus":{},\
                "then":[{"testConsumer":{"class":\
                "com.norconex.commons.lang.flow.mock.MockUppercaseConsumer","field":"x"}}]}}]}
                """;
        assertThatException().isThrownBy(() -> beanMapper.read(
                TestFlowConfig.class, new StringReader(json), Format.JSON));
    }

    // Exercises IfHandler.read() missing-condition error
    @Test
    void testIfWithMissingCondition() {
        var json = """
                {"flowTest":[{"if":{"then":[{"testConsumer":{"class":\
                "com.norconex.commons.lang.flow.mock.MockUppercaseConsumer","field":"x"}}]}}]}
                """;
        assertThatException().isThrownBy(() -> beanMapper.read(
                TestFlowConfig.class, new StringReader(json), Format.JSON));
    }

    // Exercises IfHandler.read() missing-then error
    @Test
    void testIfWithMissingThen() {
        var json = """
                {"flowTest":[{"if":{"condition":{"class":\
                "com.norconex.commons.lang.flow.mock.MockMapSizeEqualsCondition","size":3}}}]}
                """;
        assertThatException().isThrownBy(() -> beanMapper.read(
                TestFlowConfig.class, new StringReader(json), Format.JSON));
    }

    // Exercises Args.setCondition() duplicate-condition error (condition + anyOf in same if)
    @Test
    void testIfWithDuplicateConditionTypes() {
        var json = """
                {"flowTest":[{"if":{\
                "condition":{"class":\
                "com.norconex.commons.lang.flow.mock.MockMapSizeEqualsCondition","size":3},\
                "anyOf":[{"condition":{"class":\
                "com.norconex.commons.lang.flow.mock.MockMapSizeEqualsCondition","size":2}}],\
                "then":[{"testConsumer":{"class":\
                "com.norconex.commons.lang.flow.mock.MockUppercaseConsumer","field":"x"}}]}}]}
                """;
        assertThatException().isThrownBy(() -> beanMapper.read(
                TestFlowConfig.class, new StringReader(json), Format.JSON));
    }

    // Exercises RootHandler.readObject() "misplaced statement" error
    @Test
    void testRootHandlerMisplacedStatement() {
        var json = """
                {"flowTest":[{"then":[{"testConsumer":{"class":\
                "com.norconex.commons.lang.flow.mock.MockUppercaseConsumer","field":"x"}}]}]}
                """;
        assertThatException().isThrownBy(() -> beanMapper.read(
                TestFlowConfig.class, new StringReader(json), Format.JSON));
    }

    // Exercises RootHandler.readInputConsumer() "type == null → Consumer.class" branch
    @Test
    void testFlowWithNullConsumerBaseType() {
        var flowCfg = new FlowMapperConfig();
        // No baseType set → exercises "if (type == null) { type = Consumer.class; }"
        flowCfg.setConsumerNameProvider(c -> "testConsumer");

        var mapper = BeanMapper.builder().flowMapperConfig(flowCfg).indent(true)
                .build();

        var json = """
                {"flowTest":[{"testConsumer":{"field":"name"}}]}
                """;
        // Consumer.class is abstract, so deserialization fails after null-type branch
        assertThatException().isThrownBy(() -> mapper.read(
                DirectConsumerFlowConfig.class, new StringReader(json),
                Format.JSON));
    }

    // Exercises RootHandler.readInputConsumer() non-Consumer-type-without-adapter error path
    @Test
    void testFlowWithNonConsumerTypeWithoutAdapter() {
        var flowCfg = new FlowMapperConfig();
        flowCfg.getConsumerType().setBaseType(String.class); // not a Consumer
        // No predicateType adapter → IllegalStateException thrown
        flowCfg.setConsumerNameProvider(c -> "testConsumer");

        var mapper = BeanMapper.builder().flowMapperConfig(flowCfg).indent(true)
                .build();

        var json = """
                {"flowTest":[{"testConsumer":"hello"}]}
                """;
        assertThatException().isThrownBy(() -> mapper.read(
                DirectConsumerFlowConfig.class, new StringReader(json),
                Format.JSON));
    }

    // Exercises ConditionHandler.read() with null base predicate type (uses Predicate.class)
    @Test
    void testConditionHandlerWithNullBaseType() {
        var flowCfg = new FlowMapperConfig();
        // predicateType baseType is null → ConditionHandler will use Predicate.class
        // This will likely fail at deserialization since Predicate is abstract,
        // but it exercises the type == null code path
        var mapper = BeanMapper.builder().flowMapperConfig(flowCfg).indent(true)
                .build();

        var json = """
                {"flowTest":[{"if":{"condition":\
                {"class":"com.norconex.commons.lang.flow.mock.MockMapSizeEqualsCondition","size":3},\
                "then":[]}}]}
                """;
        // Expected to fail - Predicate.class can't be directly deserialized without type info
        assertThatException().isThrownBy(() -> mapper.read(
                DirectConsumerFlowConfig.class, new StringReader(json),
                Format.JSON));
    }

    // Exercises ConditionHandler.read() error when base type is not Predicate and no adapter
    @Test
    void testConditionHandlerNonPredicateTypeWithoutAdapter() {
        var flowCfg = new FlowMapperConfig();
        flowCfg.getPredicateType().setBaseType(String.class); // Not a Predicate!
        // No adapter configured → should throw IllegalStateException
        var mapper = BeanMapper.builder().flowMapperConfig(flowCfg).indent(true)
                .build();

        var json = """
                {"flowTest":[{"if":{"condition":"hello",\
                "then":[]}}]}
                """;
        assertThatException().isThrownBy(() -> mapper.read(
                DirectConsumerFlowConfig.class, new StringReader(json),
                Format.JSON));
    }
}
