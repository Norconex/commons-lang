package com.norconex.commons.lang.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WebFileTest {

    @TempDir
    private Path tempDir;

    private static final String FILENAME = "webFile.txt";

    private static final URL URL;
    static {
        try {
            URL = Paths.get("src/test/resources/file/" + FILENAME)
                    .toAbsolutePath().toUri().toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }
    private static String URLSTR = URL.toString();

    private static int seq;

    @ParameterizedTest
    @EnumSource(value = WebFileCreator.class)
    void testWebFile(WebFileCreator creator) { //NOSONAR
        WebFile webFile = creator.create(tempDir.resolve("webfileDir" + ++seq));

        // Make sure local file was "downloaded" properly:
        assertThat(webFile.getResolvedFile()).content().contains("SUCCESS");

        assertThat(webFile).endsWith(Paths.get(FILENAME));
        assertThat(webFile.getFileName().toString()).contains(FILENAME);
        assertThat(webFile.getUrl()).isEqualTo(URL);
        assertThat(webFile.getFileSystem()).isNotNull();
        assertThat(webFile.isAbsolute()).isTrue();
        assertThat(webFile.getRoot()).isNotNull();
        assertThat(webFile.getParent()).isNotNull();
        assertThat(webFile.getName(webFile.getNameCount() -1)).isEqualTo(
                Paths.get(FILENAME));
        assertThat(webFile.getResolvedFile()).endsWith(
                webFile.subpath(0, webFile.getNameCount()));
        assertThat(webFile.startsWith(Paths.get("/bad/bad/"))).isFalse();
        assertThat(webFile.startsWith("/bad/bad/")).isFalse();
        assertThat(webFile.endsWith(Paths.get(FILENAME))).isTrue();
        assertThat(webFile.endsWith(FILENAME)).isTrue();
        assertThat(webFile.normalize()).isEqualTo(webFile.getResolvedFile());
        assertThat(webFile.resolve(Paths.get("test"))).isNotNull();
        assertThat(webFile.resolve(Paths.get("test"))).isNotNull();
        assertThat(webFile.resolve("test")).isNotNull();
        assertThat(webFile.resolveSibling(Paths.get("sibling"))).isNotNull();
        assertThat(webFile.resolveSibling("sibling")).isNotNull();
        assertThat(webFile.relativize(webFile.resolve("relat"))).isNotNull();
        assertThat(webFile.toUri()).isEqualTo(
                webFile.getResolvedFile().toUri());
        assertThat(webFile.toAbsolutePath()).isNotNull();
        assertThat(webFile.toFile()).isNotNull();
        assertThat(webFile.iterator()).isNotNull();
        assertThat(webFile.toString()).isNotNull();
        assertThat(webFile)
            .isEqualByComparingTo(webFile)
            .isEqualTo(webFile)
            .hasSameHashCodeAs(webFile.getResolvedFile())
            .isEqualTo(webFile.getResolvedFile());
    }

    enum WebFileCreator {
        CREATE_STRING_PATH(dir -> WebFile.create(URLSTR, dir)),
        CREATE_STRING_PATH_STRING(dir -> WebFile.create(URLSTR, dir, FILENAME)),
        CREATE_STRING_STRING(dir -> WebFile.create(URLSTR, FILENAME)),
        CONSTR_STRING(dir -> new WebFile(URLSTR)),
        CONSTR_STRING_PATH(dir -> new WebFile(URLSTR, dir.resolve(FILENAME))),
        CONSTR_URL(dir -> new WebFile(URL)),
        ;
        private final Function<Path, WebFile> creator;
        WebFileCreator(Function<Path, WebFile> creator) {
            this.creator = creator;
        }
        WebFile create(Path downloadDir) {
            return creator.apply(downloadDir);
        }
    }
}
