package com.norconex.commons.lang.javadoc;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
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

    @TempDir
    static Path tempDir;

    // method name -> javadoc
    static Map<String, String> methodJavadocs = new HashMap<>();

    @Test
    void testXMLTaglet() throws FileNotFoundException, IOException {
        var expected = "<pre><code class=\"language-xml\">\n" + escapeHtml4(
                "<a>\n"
              + "  <b\n"
              + "      attr=\"xyz\">\n"
              + "    123\n"
              + "  </b>\n"
              + "</a>"
        ) + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xml"));
    }

    @Test
    void testIncludeTaglet() throws FileNotFoundException, IOException {
        var expected = "XML include:\n"
                + " <xml>\n"
                + "   <testValue>Space + ID</testValue>\n"
                + " </xml>";
        assertEquals(expected, methodJavadocs.get("include"));
    }

    @Test
    void testNestedIncludeTaglet() throws FileNotFoundException, IOException {
        var expected = "Before block include.\n"
              + " Before XML include.\n"
              + "<pre><code class=\"language-xml\">\n" + escapeHtml4(
                    " <xml>\n"
                  + "   <testValue>Space + ID</testValue>\n"
                  + " </xml>"
              ) + "</code></pre>\n"
              + " After XML include.\n"
              + " After block include.";
        assertEquals(expected, methodJavadocs.get("includeNested"));
    }

    @BeforeAll
    static void beforeAll() throws IOException {
        var classAsPath = MockJavadoc.class.getName().replace('.', '/');

        var javadocDir =
            //tempDir.toString();
            "./target/temp-javadocs";

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
