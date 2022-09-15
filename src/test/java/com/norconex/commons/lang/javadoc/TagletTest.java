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

    @BeforeAll
    static void beforeAll() throws IOException {
        var classAsPath = MockJavadoc.class.getName().replace('.', '/');

        var src = "./src/test/java/" + classAsPath + ".java";
        ToolProvider.getSystemDocumentationTool().run(null, null, null,
                "-d",
                    tempDir.toString(),
                    //"./target/temp-javadocs",
                "-taglet", XMLTaglet.class.getName(),
                "-taglet", IncludeTaglet.class.getName(),
                "-nohelp",
                "-noindex",
                "-nonavbar",
                "-notree",
                "--allow-script-in-comments",
                src);

// to include source in html:        -linksource

        var html = Files.readString(
                Path.of(tempDir.toString() + "/" + classAsPath + ".html"));
        html = StringUtils.substringAfterLast(html, "<h3>Method Detail</h3>");
        var m = Pattern.compile(
                "<a id=\"(.*?)\\(\\)\">.*?<div class=\"block\">(.*?)</div>",
                Pattern.DOTALL).matcher(html);
        while (m.find()) {
            methodJavadocs.put(m.group(1), m.group(2).replaceAll("\r", ""));
        }
    }

    @Test
    void textXMLTaglet() throws FileNotFoundException, IOException {
        var expected = "<pre><code class=\"language-xml\">\n" + escapeHtml4(
                "<a>\n"
              + "  <b\n"
              + "      attr=\"xyz\">\n"
              + "    123\n"
              + "  </b>\n"
              + "</a>"
        ) + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("xmlTag"));
    }

    @Test
    void textIncludeTaglet() throws FileNotFoundException, IOException {
        var expected = "XML include:\n<pre><code class=\"language-xml\">\n"
                + escapeHtml4(
                "<a>\n"
              + "  <b\n"
              + "      attr=\"xyz\">\n"
              + "    123\n"
              + "  </b>\n"
              + "</a>"
        ) + "</code></pre>";
        assertEquals(expected, methodJavadocs.get("intludeTag"));
    }
}
