package com.norconex.commons.lang.javadoc;


//* {@nx.xml.usage
//* <xml>
//*   <testValue>Usage</testValue>
//* </xml>
//* }

/**
 * <p>Test javadoc for Taglet test cases.</p>
 *
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
 * {@nx.block #blockWithInclude
 * Before XML include.
 *   {@nx.xml
 *     {@nx.include com.norconex.commons.lang.javadoc.MockJavaDoc #spaceId}
 *   }
 * After XML include.
 * }
 *
 */
public class MockJavadoc {

    public static final String SOME_VALUE = "yes";

    /**
     * {@nx.xml
     * <a>
     *   <b attr="xyz">123</b>
     * </a>
     * }
     */
    public void xml() {}

    /**
     * XML include:
     * {@nx.include com.norconex.commons.lang.javadoc.MockJavadoc#spaceId}
     */
    public void include() {}


   /**
    * Before block include.
    * {@nx.include com.norconex.commons.lang.javadoc.MockJavadoc#blockWithInclude}
    * After block include.
    */
   public void includeNested() {}


    /**
     * Some text before.
     *
     * {@nx.xml
     * <a>
     *   <b attr="xyz">{@value #SOME_VALUE}</b>
     * </a>
     * }
     *
     * Some text after.
     */
    public void misc() {}

// test include with ids using block as source (or else)
//    * {@nx.include com.norconex.commons.lang.javadoc.MockJavadoc@nx.xml.usage}

}
