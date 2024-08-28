/* Copyright 2021 Norconex Inc.
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

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;

final class MockXMLFlowConsumerAdapter
        implements XMLFlowConsumerAdapter<Properties> {

    private Consumer<Properties> consumer;

    @Override
    public void accept(Properties t) {
        consumer.accept(t);
    }

    @Override
    public void loadFromXML(XML xml) {
        consumer = xml.toObjectImpl(Consumer.class);
        if (consumer == null) {
            // default if not set in XML
            consumer = new MockXMLUppercaseConsumer();
            xml.populate(consumer);
        }
    }

    @Override
    public void saveToXML(XML xml) {
        xml.replace(new XML(xml.getName(), consumer));
    }
}