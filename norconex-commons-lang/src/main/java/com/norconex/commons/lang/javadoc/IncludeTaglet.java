/* Copyright 2020 Norconex Inc.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * <p>{&#64;nx.include} Include another taglet raw content from another
 * taglet.</p>
 * <p>Example that includes the {@link XMLUsageTaglet} usage example:</p>
 * <pre>
 * &lt;xml&gt;
 *   &lt;sample attr="whatever"&gt;Next line should be replaced.&lt;/sample&gt;
 *   {&#64;nx.include com.norconex.commons.lang.javadoc.XMLUsageTaglet@nx.xml.usage}
 * &lt;/xml&gt;
 * </pre>
 *
 * <p>Results in:</p>
 * {@nx.xml
 * <xml>
 *   <sub attr="whatever">Next line should be replaced.</sub>
 *   {@nx.include com.norconex.commons.lang.javadoc.XMLExampleTaglet@nx.xml.example}
 * </xml>
 * }
 *
 * <p>Can be nested in nx.xml.* taglets.</p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("javadoc")
public class IncludeTaglet extends AbstractInlineTaglet {

    //TODO document: can use @ (to include taglet block) or
    // # to include taglet id (prefixed with @ or not)

    public static final String NAME = "nx.include";

    private static final Path SOURCE_DIR;
    static {
        Path dir = null;
        String prop = System.getProperties().getProperty("basedir");
        if (StringUtils.isNotBlank(prop)) {
            Path baseDir = Paths.get(prop);
            if (baseDir.toFile().exists()) {
                dir = baseDir.resolve("src/main/java");
            }
        }
        if (dir == null) {
            dir = Paths.get("").toAbsolutePath().resolve(
                    "../../../src/main/java").normalize();
        }
        SOURCE_DIR = dir;
    }

    /**
     * Register an instance of this taglet.
     * @param tagletMap registry of taglets
     */
    public static void register(Map<String, Taglet> tagletMap) {
        tagletMap.put(NAME, new IncludeTaglet());
    }

    @Override
    public String getName() {
        return NAME;
    }

    protected Path getSourceDir() {
        return SOURCE_DIR;
    }

    @Override
    public String toString(Tag tag) {
        return include(tag.text());
    }

    public static String include(String includeRef) {
        String ref = includeRef.trim();
        String className = ref.replaceFirst("^(.*?)[\\@\\#].*$", "$1");
        String id = ref.replaceFirst("^.*?([\\@\\#].*)$", "$1");
        if (StringUtils.isAllBlank(className, id)) {
            System.err.println(
                    "Missing @taglet or #id argument to class name for: "
                    + ref);
            System.exit(-1);
            return null;
        }

        String source = getJavaSource(className);
        String block = findInSource(source, id);
        if (StringUtils.isBlank(block)) {
            System.err.println(
                    "ID '" + id + "' not found in source for: " + className);
            System.exit(-1);
        }
        return block;
    }



    private static String findInSource(String source, String id) {
        Matcher m = Pattern.compile(
                "\\/\\*\\*(.*?)\\*\\/", Pattern.DOTALL).matcher(source);
        while (m.find()) {
            String comment = m.group(1).replaceAll("(?m)^ *?\\* ?(.*)$", "$1");
            String snippet = extractFromComment(comment, id);
            if (StringUtils.isNotBlank(snippet)) {
                return snippet.trim();
            }
        }
        return null;
    }

    private static final Pattern BRACES_PATTERN = Pattern.compile(
            "(?=\\{)(?:(?=.*?\\{(?!.*?\\1)(.*\\}"
            + "(?!.*\\2).*))(?=.*?\\}(?!.*?\\2)(.*)).)+?.*?(?=\\1)"
            + "[^{]*(?=\\2$)", Pattern.DOTALL);
    private static String extractFromComment(String comment, String id) {
        String regex = id;
        if (regex.startsWith("@")) {
            regex = regex.replaceFirst("\\s*\\#", "\\s*#");
        } else {
            regex = "@nx\\..*?\\" + regex;
        }
        regex = "(?s)^.*?\\{\\" + regex + "\\b(.*?)\\}.*$";
        Matcher m = BRACES_PATTERN.matcher(comment);
        while (m.find()) {
            String snippet = m.group();
            if (snippet.matches(regex)) {
                return snippet.replaceFirst(regex, "$1");
            }
        }
        return null;
    }

    protected static String getJavaSource(String className) {
        String source = readSourceFromFile(className);
        if (source == null) {
            source = readSourceFromClasspath(className);
        }
        if (source == null) {
            System.err.println("Source not found for: " + className);
            System.exit(-1);
        }
        return source;
    }

    private static String readSourceFromFile(String className) {
        String relativePath = className.replace('.', '/') + ".java";
        if (SOURCE_DIR != null) {
            File resourceFile = SOURCE_DIR.resolve(relativePath).toFile();
            if (resourceFile.exists()) {
                try {
                    return FileUtils.readFileToString(resourceFile, UTF_8);
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        return null;
    }
    private static String readSourceFromClasspath(String className) {
        String fullPath = StringUtils.prependIfMissing(
                className, "/").replace('.', '/') + ".java";
        try (InputStream is =
                IncludeTaglet.class.getResourceAsStream(fullPath)) {
            if (is != null) {
                return IOUtils.toString(is, UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
}
