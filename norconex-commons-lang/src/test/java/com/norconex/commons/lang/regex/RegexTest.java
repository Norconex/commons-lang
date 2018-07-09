/* Copyright 2017-2018 Norconex Inc.
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
package com.norconex.commons.lang.regex;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.map.Properties;

/**
 * @author Pascal Essiembre
 * @since 3.0.0 (originates from Norconex Importer)
 */
public class RegexTest {

    @Test
    public void testExtractFields()
            throws IOException {

        String xml = ResourceLoader.getXmlString(RegexTest.class);
        Properties fields = null;

        fields = KeyValueExtractor.extractKeyValues(xml,
            //Test 1) no match group, returning whole match as value
            new KeyValueExtractor()
                .setKey("test1")
                .setRegex("<div class=\"value\">(.*?)</div>"),
            //Test 2) 1 match group, returning specified match value
            new KeyValueExtractor()
                .setKey("test2")
                .setRegex("<div class=\"value\">(.*?)</div>")
                .setValueGroup(1),
            //Test 3) 2 match groups, returning field name and values
            new KeyValueExtractor()
                .setRegex("<div class=\"field\">(.*?)</div>.*?"
                        + "<div class=\"value\">(.*?)</div>")
                .setKeyGroup(1)
                .setValueGroup(2)
        );

        //Test 1
        Assert.assertEquals("Wrong test1 value count.", 4,
                fields.getStrings("test1").size());
        Assert.assertEquals("Wrong test1 value.",
                "<div class=\"value\">Suite 456</div>",
                fields.getStrings("test1").get(3));

        //Test 2
        Assert.assertEquals("Wrong test2 value count.", 4,
                fields.getStrings("test2").size());
        Assert.assertEquals("Wrong test2 value.", "Suite 456",
                fields.getStrings("test2").get(3));

        //Test 3
        Assert.assertEquals(1, fields.getStrings("First Name").size());
        Assert.assertEquals("Joe", fields.getString("First Name"));
        Assert.assertEquals(1, fields.getStrings("Last Name").size());
        Assert.assertEquals("Dalton", fields.getString("Last Name"));
        Assert.assertEquals(2, fields.getStrings("Street").size());
        Assert.assertEquals("123 MultiValue St", fields.getString("Street"));
        Assert.assertEquals("Suite 456", fields.getStrings("Street").get(1));

        //Test 4) No field group specified, using default field name
        fields = null;
        fields = KeyValueExtractor.extractKeyValues(xml,
            new KeyValueExtractor()
                .setRegex("<div class=\"field\">(.*?)</div>.*?"
                        + "<div class=\"value\">(.*?)</div>")
                .setKey("test4")
                .setValueGroup(2)
        );
        Assert.assertEquals("Wrong test4 value count.", 4,
                fields.getStrings("test4").size());
        Assert.assertEquals("Wrong test4 value.", "Suite 456",
                fields.getStrings("test4").get(3));

        //Test 5) No field group specified, no default field name
        try {
            fields = null;
            fields = KeyValueExtractor.extractKeyValues(xml,
                    new KeyValueExtractor()
                        .setRegex("<div class=\"field\">(.*?)</div>.*?"
                                + "<div class=\"value\">(.*?)</div>")
                        .setValueGroup(2));
            Assert.fail("Should have thrown an exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertNull("Test5 fields should be null.", fields);
        }

        //Test 6) No value group specified, with field group
        fields = null;
        fields = KeyValueExtractor.extractKeyValues(xml,
            new KeyValueExtractor()
                .setRegex("<DIV class=\"field\">(.*?)</DIV>.*?"
                        + "<DIV class=\"value\">(.*?)</DIV>")
                .setKeyGroup(1)
        );
        Assert.assertEquals("Wrong test6 fields size.", 3, fields.size());
        Assert.assertEquals("Wrong test6 value.",
                "<div class=\"field\">Last Name</div>\n  "
              + "<div class=\"value\">Dalton</div>",
                fields.getString("Last Name"));

        //Test 7) No value or field group
        try {
            fields = null;
            fields = KeyValueExtractor.extractKeyValues(xml,
                new KeyValueExtractor()
                    .setRegex("<div class=\"field\">(.*?)</div>.*?"
                            + "<div class=\"value\">(.*?)</div>"));
            Assert.fail("Should have thrown an exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertNull("Test7 fields should be null.", fields);
        }

        //Test 8) No value group specified, with field group, case sensitive
        fields = null;
        fields = KeyValueExtractor.extractKeyValues(xml,
            new KeyValueExtractor()
                .setRegex("<DIV class=\"field\">(.*?)</DIV>.*?"
                        + "<DIV class=\"value\">(.*?)</DIV>")
                .setKeyGroup(1)
                .setCaseSensitive(true)
        );
        Assert.assertTrue("Test8 fields should be empty.", fields.isEmpty());
    }
}
