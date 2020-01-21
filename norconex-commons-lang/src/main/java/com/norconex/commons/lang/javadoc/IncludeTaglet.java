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
import java.net.MalformedURLException;
import java.net.URL;
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
 * <p>{&#64;nx.include} Include raw content from another
 * taglet in a different source file.</p>
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
 * <p>Can also use # if the target defines such as id.</p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("javadoc")
public class IncludeTaglet extends AbstractInlineTaglet {

    //TODO document: can use @ (to include taglet block) or
    // # to include taglet id (prefixed with @ or not)

    public static final String NAME = "nx.include";

    private static final String JAVA_FILE_EXT = ".java";
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
        // If not set via system properties, check for a "src" folder assuming
        // we are in the project root folder.
        if (dir == null) {
            dir = Paths.get("").toAbsolutePath().resolve(
                    "./src/main/java").normalize();
        }

        // If still null, we recurse a few times as we may be in the "site"
        // folder not set via system properties, check for a "src" folder
        // assuming
        // we are in the project root folder.
        if (dir == null || !dir.toFile().isDirectory()) {
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
        return include(tag, tag.text());
    }

    public static String include(Tag tag, String includeRef) {
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

        String fullClassName = getFullClassName(tag, className);
        String source = getJavaSource(fullClassName);
        String block = findInSource(source, id);
        if (StringUtils.isBlank(block)) {
            System.err.println("ID '" + id
                    + "' not found in source for: " + fullClassName);
            return "!! DOCUMENTATION ERROR !! Refer to " + fullClassName
                    + " class documentation for additional information.";
        }
        return block;
    }

    private static String getFullClassName(Tag tag, String className) {
        // If it has a doc, assume package is supplied so we are good.
        if (className.contains(".")) {
            return className;
        }

        File sourceFile = tag.holder().position().file();
        if (sourceFile != null) {
            // Otherwise, check if the class is in current package
            File f = new File(
                    sourceFile.getParentFile(), className + JAVA_FILE_EXT);
            if (f.exists()) {
                String c = f.getAbsolutePath();
                c = c.replaceAll("[\\\\\\/]", ".");
                c = StringUtils.substringAfterLast(c, ".src.main.java.");
                c = StringUtils.removeEnd(c, JAVA_FILE_EXT);
                return c;
            }

            // Finally, see if package is defined in imports for the class.
            try {
                Matcher m = Pattern.compile(
                        "(?m)^\\s*import (.*\\." + className  + ");$").matcher(
                                FileUtils.readFileToString(sourceFile, UTF_8));
                if (m.find()) {
                    return m.group(1);
                }
            } catch (IOException e) {
                //NOOP
            }
        }
        return className;
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
            return "!! DOCUMENTATION ERROR !! Refer to " + className
                    + " class documentation for additional information.";
        }
        return source;
    }

    private static String readSourceFromFile(String className) {
        String relativePath = className.replace('.', '/') + JAVA_FILE_EXT;
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
                className, "/").replace('.', '/')  + JAVA_FILE_EXT;

        String source = null;
        URL url = IncludeTaglet.class.getResource(fullPath);

        if (url != null) {
            try {
                source = readSourceFromURL(url);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        if (source == null) {
            // For some reason even if sources.jar is included as a dependency,
            // it may not be found by the class loader.
            // Here we try to force it to look at the source.
            try {
                source = readSourceFromURL(toSourceURL(className));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace(System.err);
            }
        }
        return source;
    }

    private static String readSourceFromURL(URL url) throws IOException {
        if (url == null) {
            return null;
        }
        try (InputStream is = url.openStream()) {
            if (is != null) {
                return IOUtils.toString(is, UTF_8);
            }
        }
        return null;
    }

    private static URL toSourceURL(String className)
            throws MalformedURLException, ClassNotFoundException {
        Class<?> cls = Class.forName(className);
        URL classURL = cls.getResource(cls.getSimpleName() + ".class");
        if (classURL == null) {
            return null;
        }
        String origPath = classURL.toExternalForm();
        String sourcePath = origPath.replace(".jar!", "-sources.jar!");
        sourcePath = sourcePath.replaceFirst("\\.class$", ".java");
        if (origPath.equals(sourcePath)) {
            return null;
        }
        return new URL(sourcePath);
    }
}
