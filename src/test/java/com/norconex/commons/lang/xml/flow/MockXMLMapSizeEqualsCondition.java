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

import java.util.Map;
import java.util.function.Predicate;

import com.norconex.commons.lang.xml.XMLConfigurable;
import com.norconex.commons.lang.xml.XML;

// number of map entries must match given size
public class MockXMLMapSizeEqualsCondition
        implements Predicate<Map<?, ?>>, XMLConfigurable {

    private int size;

    @Override
    public boolean test(Map<?, ?> map) {
        return map != null && map.size() == size;
    }

    @Override
    public void loadFromXML(XML xml) {
        size = xml.getInteger("size");
    }

    @Override
    public void saveToXML(XML xml) {
        xml.addElement("size", size);
    }
}
