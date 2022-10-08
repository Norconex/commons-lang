/* Copyright 2018-2022 Norconex Inc.
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
package com.norconex.commons.lang.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;

class ConfigurationTest {

    //TODO move to XMLTest or equivalent

    @Test
    void testPreserveWhiteSpace() {
        var xml = XML.of(
                  "<test>"
                + "<tagBlank>   </tagBlank>"
                + "<tagPreserve xml:space=\"preserve\">   </tagPreserve>"
                + "<tagNested>"
                + " <nestedBlank>   </nestedBlank>"
                + " <nestedPreserve xml:space=\"preserve\">   </nestedPreserve>"
                + "</tagNested>"
                + "</test>").create();

        Assertions.assertNull(xml.getString("tagNotPresent"));
        Assertions.assertEquals("", xml.getString("tagBlank"));
        Assertions.assertEquals("   ", xml.getString("tagPreserve"));
        Assertions.assertEquals("", xml.getString("tagNested/nestedBlank"));
        Assertions.assertEquals("   ",
                xml.getString("tagNested/nestedPreserve"));
    }
}
