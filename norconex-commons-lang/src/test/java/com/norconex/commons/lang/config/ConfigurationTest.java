/* Copyright 2018 Norconex Inc.
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

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void testPreserveWhiteSpace() throws ConfigurationException {
        String xml = 
                  "<test>"
                + "<tagNoPreserve>   </tagNoPreserve>"
                + "<tagPreserve xml:space=\"preserve\">   </tagPreserve>"
                + "<tagNested xml:space=\"preserve\">"
                + "  <nested>   </nested>"
                + "</tagNested>"
                + "</test>";
                
        XMLConfiguration c = XMLConfigurationUtil.newXMLConfiguration(xml);
        Assert.assertEquals("", c.getString("tagNoPreserve"));
        Assert.assertEquals("   ", c.getString("tagNested.nested"));
        Assert.assertEquals("   ", c.getString("tagPreserve"));
    }
}
