/* Copyright 2022 Norconex Inc.
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
package com.norconex.commons.lang.javadoc;

/**
 * <p>Test javadoc for Taglet test cases.</p>
 *
 * <!-- The below entries are for testing IncludeTaglet, as it only supports
 *    - including blocks defined in class/type javadoc.
 *    -->
 *
 * {@nx.xml #spaceId
 * <xml>
 *   <testValue>Space + ID</testValue>
 * </xml>
 * }
 *
 * {@nx.xml#noSpaceId
 * <xml>
 *   <testValue>No Space + ID</testValue>
 * </xml>
 * }
 *
 * {@nx.block #blockWithNoInclude
 *   Inside NO include.
 * }
 *
 * {@nx.block #blockWithInclude
 *  Before include.
 *   {@nx.include com.norconex.commons.lang.javadoc.MockJavadoc #blockWithNoInclude}
 * After include.
 * }
 *
 * {@nx.block #blockWithBadInclude
 * Before include.
 *   {@nx.include i.do.not.exist #blockWithNoInclude}
 * After include.
 * }
 */
@SuppressWarnings("javadoc")
public class MockJavadoc {

    public static final String SOME_VALUE = "yes";

    /**
     * {@nx.xml
     * <a>
     *   <b attr="xyz">123</b>
     * </a>
     * }
     */
    public void xml() {
    }

    /**
     * {@nx.xml
     *   <a>123</a>
     *   <b>456</b>
     * }
     */
    public void xmlNoParent() {
    }

    /**
     * {@nx.xml.usage
     * <a>
     *   <b attr="xyz">123</b>
     * </a>
     * }
     */
    public void xmlUsage() {
    }

    /**
     * {@nx.xml.example
     * <a>
     *   <b attr="xyz">123</b>
     * </a>
     * }
     */
    public void xmlExample() {
    }

    /**
     * {@nx.json
     *   {
     *     "object": {
     *       "prop1": "text",
     *         "prop2": 123,
     *
     *       "prop3": true
     *     }
     *   }
     * }
     */
    public void json() {
    }

    /**
     * {@nx.json
     *   "object1": { "prop1": "value1" },
     *   "object2": { "prop2": "value2" },
     * }
     */

    public void jsonPropertiesNoParent() {
    }

    /**
     * {@nx.json
     *   { "prop1": "value1" },
     *   { "prop2": "value2" },
     * }
     */

    public void jsonObjectsNoParent() {
    }

    /**
     * {@nx.json
     *   [ "value1A", "value1B" ],
     *   [ "value2A", "value2B" ],
     * }
     */
    public void jsonArraysNoParent() {
    }

    /**
     * {@nx.json
     *   <div>I am not Jason.</div>
     * }
     */
    public void jsonBadSyntax() {
    }

    /**
     * {@nx.html
     *   <div><h1>Title</h1></div>
     * }
     */
    public void html() {
    }

    /**
     * {@nx.html
     *   <div>a</div>
     *   <div>b</div>
     * }
     */
    public void htmlNoParent() {
    }

    /**
     * XML include:
     * {@nx.include com.norconex.commons.lang.javadoc.MockJavadoc#spaceId}
     */
    public void include() {
    }

    /**
     * Before block include.
     * {@nx.include com.norconex.commons.lang.javadoc.MockJavadoc#blockWithInclude}
     * After block include.
     */
    public void includeNested() {
    }

    /**
     * Before block include.
     * {@nx.include com.norconex.commons.lang.javadoc.MockJavadoc#blockWithBadInclude}
     * After block include.
     */
    public void includeNonExisting() {
    }

}
