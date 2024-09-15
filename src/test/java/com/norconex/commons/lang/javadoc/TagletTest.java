/* Copyright 2022-2023 Norconex Inc.
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

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.apache.commons.text.StringEscapeUtils.escapeXml11;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.tools.ToolProvider;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownInlineTagTree;

import jdk.javadoc.doclet.Taglet.Location;

@Disabled
class TagletTest {

    private static String TEST_XML = escapeHtml4("""
    	<a>
    	  <b
    	      attr="xyz">
    	    123
    	  </b>
    	</a>""");

    @TempDir
    static Path tempDir;

    // method name -> javadoc
    static Map<String, String> methodJavadocs = new HashMap<>();

    //--- Life-cycle methods ---------------------------------------------------

    @BeforeAll
    static void beforeAll() throws IOException {
        var classAsPath = MockJavadoc.class.getName().replace('.', '/');

        var javadocDir =
                tempDir.toString();
        //"./target/temp-javadocs";

        var src = "./src/test/java/" + classAsPath + ".java";
        ToolProvider.getSystemDocumentationTool().run(null, null, null,
                "-d", javadocDir,
                "-taglet", BlockTaglet.class.getName(),
                "-taglet", HtmlTaglet.class.getName(),
                "-taglet", IncludeTaglet.class.getName(),
                "-taglet", JsonTaglet.class.getName(),
                "-taglet", XmlTaglet.class.getName(),
                "-taglet", XmlExampleTaglet.class.getName(),
                "-taglet", XmlUsageTaglet.class.getName(),
                "-nohelp",
                "-noindex",
                "-nonavbar",
                "-notree",
                "-linksource",
                "--allow-script-in-comments",
                src);

        var html = Files.readString(
                Path.of(javadocDir + "/" + classAsPath + ".html"));
        html = StringUtils.substringAfterLast(html, "=== METHOD DETAIL ===");
        var m = Pattern.compile(
                "<h2>(.*?)</h2>.*?<div class=\"block\">(.*?)</div>",
                Pattern.DOTALL).matcher(html);
        while (m.find()) {
            methodJavadocs.put(m.group(1), m.group(2).replaceAll("\r", ""));
        }
    }

    //--- XML* -----------------------------------------------------------------

    @Test
    void testXML() {
        var expected =
                "<pre><code class=\"language-xml\">\n"
                        + TEST_XML + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xml"));
    }

    @Test
    void testXMLNoParent() {
        var expected =
                "<pre><code class=\"language-xml\">\n"
                        + escapeHtml4(
                                "<a>123</a>\n"
                                        + "<b>456</b>")
                        + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xmlNoParent"));
    }

    @Test
    void testXMLUsage() {
        var expected =
                "<h2 id=\"nx-xml-usage-heading\">"
                        + "XML configuration usage:"
                        + "</h2>\n\n"
                        + "<pre><code id=\"nx-xml-usage\" class=\"language-xml\">\n"
                        + TEST_XML
                        + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xmlUsage"));
    }

    @Test
    void testXMLExample() {
        var expected =
                "<h4 id=\"nx-xml-example-heading\">"
                        + "XML usage example:"
                        + "</h4>\n\n"
                        + "<pre><code id=\"nx-xml-example\" class=\"language-xml\">\n"
                        + TEST_XML
                        + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xmlExample"));
    }

    //--- JSON -----------------------------------------------------------------

    @Test
    void testJSON() {
        var expected = "<pre><code class=\"language-json\">\n" + escapeXml11(
                "{\n"
                        + "  \"object\": {\n"
                        + "    \"prop1\": \"text\",\n"
                        + "    \"prop2\": 123,\n"
                        + "    \"prop3\": true\n"
                        + "  }\n"
                        + "}")
                + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("json"));
    }

    @Test
    void testJSONPropertiesNoParent() {
        var expected = "<pre><code class=\"language-json\">\n" + escapeXml11(
                "\"object1\": {\n"
                        + "  \"prop1\": \"value1\"\n"
                        + "},\n"
                        + "\"object2\": {\n"
                        + "  \"prop2\": \"value2\"\n"
                        + "},")
                + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("jsonPropertiesNoParent"));
    }

    @Test
    void testJSONObjectsNoParent() {
        var expected = "<pre><code class=\"language-json\">\n" + escapeXml11(
                "{\n"
                        + "  \"prop1\": \"value1\"\n"
                        + "},\n"
                        + "{\n"
                        + "  \"prop2\": \"value2\"\n"
                        + "},")
                + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("jsonObjectsNoParent"));
    }

    @Test
    void testJSONArraysNoParent() {
        var expected = "<pre><code class=\"language-json\">\n" + escapeXml11(
                "[\n"
                        + "  \"value1A\",\n"
                        + "  \"value1B\"\n"
                        + "],\n"
                        + "[\n"
                        + "  \"value2A\",\n"
                        + "  \"value2B\"\n"
                        + "],")
                + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("jsonArraysNoParent"));
    }

    //--- HTML -----------------------------------------------------------------

    @Test
    void testHTML() {
        var expected =
                "<pre><code class=\"language-html\">\n"
                        + escapeHtml4(
                                "<div>\n"
                                        + "  <h1>Title</h1>\n"
                                        + "</div>")
                        + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("html"));
    }

    @Test
    void testHTMLNoParent() {
        var expected =
                "<pre><code class=\"language-html\">\n"
                        + escapeHtml4(
                                "<div>a</div>\n"
                                        + "<div>b</div>")
                        + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("htmlNoParent"));
    }

    //--- Include + Block ------------------------------------------------------

    @Test
    void testInclude() {
        // XML in this case is not formatted since we are simply including it
        var expected = """
        	XML include:
        	 <xml>
        	   <testValue>Space + ID</testValue>
        	 </xml>""";
        assertEquals(expected, methodJavadocs.get("include"));
    }

    @Test
    void testNestedInclude() {
        var expected = """
        	Before block include.
        	 Before include.
        	   Inside NO include.
        	 After include.
        	 After block include.""";
        assertEquals(expected, methodJavadocs.get("includeNested"));
    }

    //--- Errors ---------------------------------------------------------------

    @Test
    void testJsonBadSyntax() {
        assertTrue(methodJavadocs.get("jsonBadSyntax").contains(
                "!!! Documentation error: JSONTaglet could not "
                        + "parse JSON content:"));
    }

    @Test
    void testIncludeNonExisting() {
        assertTrue(methodJavadocs.get("includeNonExisting").contains(
                "!!! Documentation error: Include directive failed as type "
                        + "element could not be resolved:"));
    }

    //--- Misc. ----------------------------------------------------------------

    @Test
    void testMisc() {
        var taglet = new XmlUsageTaglet();

        assertNotNull(taglet.getHeadingProvider());
        assertEquals(
                EnumSet.allOf(Location.class), taglet.getAllowedLocations());
        assertEquals("nx.xml.usage", taglet.getName());
    }

    @Test
    void testTagContent() {
        var tagContent = TagContent.of(new UnknownInlineTagTree() {
            @Override
            public String getTagName() {
                return "SOMENAME";
            }

            @Override
            public Kind getKind() {
                return DocTree.Kind.UNKNOWN_INLINE_TAG;
            }

            @Override
            public <R, D> R accept(DocTreeVisitor<R, D> visitor, D data) {
                return null;
            }

            @Override
            public List<? extends DocTree> getContent() {
                TextTree textTree = new TextTree() {
                    @Override
                    public Kind getKind() {
                        return DocTree.Kind.TEXT;
                    }

                    @Override
                    public <R, D> R accept(
                            DocTreeVisitor<R, D> visitor, D data) {
                        return null;
                    }

                    @Override
                    public String getBody() {
                        return "#someref\nsomebody";
                    }

                    @Override
                    public String toString() {
                        return getBody();
                    }
                };
                return new ArrayList<>(
                        Arrays.asList(textTree)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String toString() {
                        return get(0).toString();
                    }
                };
                /*
                return new ArrayList<>(
                        Arrays.asList(new TextTree() {
                    @Override
                    public Kind getKind() {
                        return DocTree.Kind.TEXT;
                    }
                    @Override
                    public <R, D> R accept(
                            DocTreeVisitor<R, D> visitor, D data) {
                        return null;
                    }
                    @Override
                    public String getBody() {
                        return "#someref\nsomebody";
                    }
                    @Override
                    public String toString() {
                        return getBody();
                    }
                }) ) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public String toString() {
                        return get(0).toString();
                    }
                };
                */
            }
        }).get();

        assertEquals("SOMENAME", tagContent.getName());
        assertEquals("someref", tagContent.getReference());
        assertEquals("somebody", tagContent.getContent());
        assertTrue(tagContent.toString().startsWith("Tag [name=SOMENAME, "));
    }
}
