package com.norconex.commons.lang.javadoc;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.tools.ToolProvider;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TagletTest {

    private static String TEST_XML = escapeHtml4(
            "<a>\n"
          + "  <b\n"
          + "      attr=\"xyz\">\n"
          + "    123\n"
          + "  </b>\n"
          + "</a>"
    );

    @TempDir
    static Path tempDir;

    // method name -> javadoc
    static Map<String, String> methodJavadocs = new HashMap<>();

    @Test
    void testXMLTaglet() {
        var expected = "<pre><code class=\"language-xml\">\n"
                + TEST_XML + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xml"));
    }

    @Test
    void testXMLUsageTaglet() {
        var expected =
                "<h3 id=\"nx-xml-usage-heading\">"
                  + "XML configuration usage:"
              + "</h3>\n\n"
              + "<pre><code id=\"nx-xml-usage\" class=\"language-xml\">\n"
              + TEST_XML
              + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xmlUsage"));
    }

    @Test
    void testXMLExampleTaglet() {
        var expected =
                "<h4 id=\"nx-xml-example-heading\">"
                  + "XML usage example:"
              + "</h4>\n\n"
              + "<pre><code id=\"nx-xml-example\" class=\"language-xml\">\n"
              + TEST_XML
              + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xmlExample"));
    }

    @Test
    void testIncludeTaglet() {
        // XML in this case is not formatted since we are simply including it
        var expected = "XML include:\n"
                + " <xml>\n"
                + "   <testValue>Space + ID</testValue>\n"
                + " </xml>";
        assertEquals(expected, methodJavadocs.get("include"));
    }

    @Test
    void testNestedIncludeTaglet() {
        var expected = "Before block include.\n"
              + " Before include.\n"
              + "   Inside NO include.\n"
              + " After include.\n"
              + " After block include.";
        assertEquals(expected, methodJavadocs.get("includeNested"));
    }

    //TODO test with typo in class that we get this error somewhere
    // !!! Documentation error: Include directive failed as type element could not be resolved: ...

    //TODO MAYBE: test that nx.block must have a reference?

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
                "-taglet", HTMLTaglet.class.getName(),
                "-taglet", IncludeTaglet.class.getName(),
                "-taglet", JSONTaglet.class.getName(),
                "-taglet", XMLTaglet.class.getName(),
                "-taglet", XMLExampleTaglet.class.getName(),
                "-taglet", XMLUsageTaglet.class.getName(),
                "-nohelp",
                "-noindex",
                "-nonavbar",
                "-notree",
                "-linksource",
                "--allow-script-in-comments",
                src);

        var html = Files.readString(
                Path.of(javadocDir + "/" + classAsPath + ".html"));
        html = StringUtils.substringAfterLast(html, "<h3>Method Detail</h3>");
        var m = Pattern.compile(
                "<a id=\"(.*?)\\(\\)\">.*?<div class=\"block\">(.*?)</div>",
                Pattern.DOTALL).matcher(html);
        while (m.find()) {
            methodJavadocs.put(m.group(1), m.group(2).replaceAll("\r", ""));
        }
    }
}
