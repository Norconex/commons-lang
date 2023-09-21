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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.FlowMapperConfig;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.flow.mock.MockFlowConditionAdapter;
import com.norconex.commons.lang.flow.mock.MockFlowInputConsumerAdapter;
import com.norconex.commons.lang.flow.mock.MockPropertyMatcherCondition;
import com.norconex.commons.lang.map.Properties;

import lombok.Data;

class FlowTest {

//    @ParameterizedTest
//    @EnumSource(Format.class)
//    void testFlow(Format format) {
//    }

    @Test
    void testFlow() throws IOException {

        var flowCfg = new FlowMapperConfig();
        flowCfg.setConditionType(MockPropertyMatcherCondition.class);
        flowCfg.setConditionAdapterType(MockFlowConditionAdapter.class);
        flowCfg.setConditionScanFilter(c -> c.startsWith("com.norconex."));

        flowCfg.setInputConsumerType(FlowInputConsumer.class);
        flowCfg.setInputConsumerAdapterType(MockFlowInputConsumerAdapter.class);
        flowCfg.setInputConsumerScanFilter(c -> c.startsWith("com.norconex."));

        var bm = BeanMapper.builder()
            .flowMapperConfig(flowCfg)
            .build();

        //Flow<Properties> c;
        TextFlowObject tfo;
        try (var r = ResourceLoader.getXmlReader(getClass())) {
            tfo = bm.read(TextFlowObject.class, r, Format.XML);
        }

        System.out.println("FLOW TEST: " + tfo);
    }

    @Data
    static class TextFlowObject {
        private Flow<Properties> thisIsTheFlow;
    }
//
//    @ToString
//    @EqualsAndHashCode
//    public static class TestPropertiesConditionAdapter
//            implements FlowConditionAdapter<Properties> {
//
//        private MockPropertyMatcherCondition nativeCondition;
//
//        @Override
//        public boolean test(Properties props) {
//            return nativeCondition.test(props);
//        }
//        @Override
//        public MockPropertyMatcherCondition getNativeCondition() {
//            return nativeCondition;
//        }
//        @Override
//        public void setNativeCondition(Object nativeCondition) {
//            this.nativeCondition = (MockPropertyMatcherCondition) nativeCondition;
//        }
//    }
//
//    @ToString
//    @EqualsAndHashCode
//    public static class TestPropertiesConsumerAdapter
//            implements FlowInputConsumerAdapter<Properties> {
//
//        private MockPropertyMatcherCondition nativeCondition;
//
//        @Override
//        public void accept(Properties t) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public boolean test(Properties props) {
//            return nativeCondition.test(props);
//        }
//        @Override
//        public MockPropertyMatcherCondition getNativeCondition() {
//            return nativeCondition;
//        }
//        @Override
//        public void setNativeCondition(Object nativeCondition) {
//            this.nativeCondition = (MockPropertyMatcherCondition) nativeCondition;
//        }
//    }

//    private void testFlow(XML xml) throws IOException {
//        XMLFlow<Properties> flow = createXMLFlow();
//        Consumer<Properties> c;
//        try (Reader r = ResourceLoader.getXmlReader(getClass())) {
//            c = flow.parse(xml);
//        }
//
//        Properties data1 = new Properties();
//        data1.add("firstName", "John");
//        data1.add("lastName", "Smith");
//        data1.set("car", "volvo");
//        c.accept(data1);
//
//        // first name uppercase
//        Assertions.assertEquals("JOHN", data1.getString("firstName"));
//        // last name unchanged
//        Assertions.assertEquals("Smith", data1.getString("lastName"));
//
//        Properties data2 = new Properties();
//        data2.add("firstName", "John");
//        data2.add("lastName", "Smith");
//        data2.set("car", "toyota");
//        c.accept(data2);
//
//        // first name lowercase
//        Assertions.assertEquals("john", data2.getString("firstName"));
//        // last name uppercase
//        Assertions.assertEquals("SMITH", data2.getString("lastName"));
//    }
//    static XMLFlow<Properties> createXMLFlow() {
//        return new XMLFlow<>(
//                MockXMLFlowConsumerAdapter.class,
//                MockXMLFlowPredicateAdapter.class);
//    }
}
