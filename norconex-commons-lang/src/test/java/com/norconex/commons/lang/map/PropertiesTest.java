package com.norconex.commons.lang.map;

import static org.junit.Assert.assertEquals;

import java.util.List;

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

}
