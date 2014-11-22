/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.commons.lang.map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.junit.Test;

public class PropertiesTest {

    @Test
    public void testLoadUsingDefaultDelimiter() throws Exception {
        
        String key = "source";
        String value = "X^2";
        Properties properties = new Properties();
        properties.addString(key, value);
        String stored = properties.storeToString("");
        
        // The default multi-value separator is ^^^. 
        // It should NOT be applied when there is a single ^.
        properties = new Properties();
        properties.loadFromString(stored);
        List<String> values = properties.getStrings(key);
        assertEquals(1, values.size());
        assertEquals(values.get(0), value);
    }
    
    @Test
    public void testRemove() throws Exception {
        Properties properties = new Properties();
        List<String> list = asList("a", "b", "c");
        properties.put("key", list);
        assertEquals(list, properties.remove("key"));
    }
    
    @Test
    public void testRemoveCaseInsensitive() throws Exception {
        Properties properties = new Properties(true);
        List<String> list = asList("a", "b", "c");
        properties.put("key".toUpperCase(), list);
        assertEquals(list, properties.remove("key"));
    }
    
    @Test
    public void testRemoveCaseInsensitiveMultiple() throws Exception {
        Properties properties = new Properties(true);
        List<String> list1 = asList("a", "b", "c");
        List<String> list2 = asList("d", "e", "f");
        properties.put("Key", list1);
        properties.put("KEy", list2);
        assertEquals(ListUtils.sum(list1, list2), 
                properties.remove("key"));
    }
    
    @Test
    public void testRemoveNonExistentKey() throws Exception {
        Properties properties = new Properties();
        assertNull(properties.remove("key"));
    }
    
    @Test
    public void testRemoveNonExistentKeyCaseInsensitive() throws Exception {
        Properties properties = new Properties(true);
        assertNull(properties.remove("key"));
    }

}
