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
import com.norconex.commons.lang.xml.XmlConfigurable;
import com.norconex.commons.lang.xml.Xml;

public class MockXmlUppercaseConsumer
        implements Consumer<Properties>, XmlConfigurable {
    private String field;

    @Override
    public void accept(Properties p) {
        p.set(field, p.getString(field, "").toUpperCase());
    }

    @Override
    public void loadFromXML(Xml xml) {
        field = xml.getString("field");
    }

    @Override
    public void saveToXML(Xml xml) {
        xml.addElement("field", field);
    }
}