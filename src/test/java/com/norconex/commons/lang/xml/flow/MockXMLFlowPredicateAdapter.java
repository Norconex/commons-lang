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

import java.util.function.Predicate;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.flow.impl.PropertyMatcherCondition;

final class MockXMLFlowPredicateAdapter
        implements XMLFlowPredicateAdapter<Properties> {

    private Predicate<Properties> predicate;

    @Override
    public boolean test(Properties t) {
        return predicate.test(t);
    }
    @Override
    public void loadFromXML(XML xml) {
        predicate = xml.toObjectImpl(Predicate.class);
        if (predicate == null) {
            // default if not set in XML
            predicate = new PropertyMatcherCondition();
            xml.populate(predicate);
        }
    }
    @Override
    public void saveToXML(XML xml) {
        xml.replace(new XML(xml.getName(), predicate));
    }
}