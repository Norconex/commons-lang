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
package com.norconex.commons.lang.jar;

import static com.norconex.commons.lang.jar.JarTestUtil.createJar;
import static com.norconex.commons.lang.jar.JarTestUtil.createSourceTargetJars;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.jar.JarCopier.OnJarConflict;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.SourceAction;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.TargetAction;
import com.norconex.commons.lang.mock.Mocker;

class JarCopierTest {

    @TempDir
    private Path tempDir;

    private Path sourceDir;
    private Path targetDir;

    @BeforeEach
    void beforeEach() throws IOException {
        sourceDir = Files.createDirectories(tempDir.resolve("sourceDir"));
        targetDir = Files.createDirectories(tempDir.resolve("targetDir"));

        createSourceTargetJars(sourceDir, targetDir, "noversion.jar");
        createSourceTargetJars(sourceDir, targetDir, "a-1.0.0.jar");
        createJar(sourceDir.resolve("b-1.0.0.jar"));
        createJar(sourceDir.resolve("b-1.1.0.jar"));
        createJar(targetDir.resolve("b-1.0.3.jar"));
        createSourceTargetJars(
                sourceDir.resolve("c-2.jar"), targetDir.resolve("c-1.2.3.jar"));
        createSourceTargetJars(
                sourceDir.resolve("d-2.1.jar"), targetDir.resolve("d.jar"));
        createJar(sourceDir.resolve("e-1.0.0.jar"));
        createSourceTargetJars(
                sourceDir.resolve("f-1.0.0-M1.jar"),
                targetDir.resolve("f-1.0.0.jar"));
        createSourceTargetJars(
                sourceDir.resolve("g-1.0.0-RC1.jar"),
                targetDir.resolve("g-1.0.0M1.jar"));
        createSourceTargetJars(
                sourceDir.resolve("h-1.0.0-alpha.jar"),
                targetDir.resolve("h-1.0.0beta.jar"));
        createSourceTargetJars(
                sourceDir.resolve("i-1.0.0-beta2.jar"),
                targetDir.resolve("i-1.0.0beta1.jar"));
        createJar(targetDir.resolve("j-1.0.0-SNAPSHOT.jar"));
        createJar(targetDir.resolve("k-1.0.0-SNAPSHOT.2.jar"));
        createJar(targetDir.resolve("l-1.0.0-RELEASE.jar"));
        createJar(sourceDir.resolve("m-1.1.0.jar"));
        createJar(targetDir.resolve("m-1.2.3.jar"));
        createJar(targetDir.resolve("m-0.2.3-ASDF.jar"));
        createJar(targetDir.resolve("m-0.2.3-QWER.jar"));
        createSourceTargetJars(
                sourceDir.resolve("n-1.0.0-AAA.jar"),
                targetDir.resolve("n-1.0.0-BBB.jar"));
    }

    @Test
    void testCopyIfGreaterOrEquivThenRename() throws IOException {
        JarCopier copier = new JarCopier();  // test with defaults
        copier.copyJarDirectory(sourceDir.toString(), targetDir.toString());

        JarTestUtil.assertDirOnlyHasFiles(targetDir,
            "noversion.jar",
            "noversion.jar.bak",
            "a-1.0.0.jar",
            "a-1.0.0.jar.bak",
            "b-1.0.3.jar.bak",
            "b-1.1.0.jar",
            "c-1.2.3.jar.bak",
            "c-2.jar",
            "d.jar.bak",
            "d-2.1.jar",
            "e-1.0.0.jar",
            "f-1.0.0.jar",
            "g-1.0.0-RC1.jar",
            "g-1.0.0M1.jar.bak",
            "h-1.0.0beta.jar",
            "i-1.0.0beta1.jar.bak",
            "i-1.0.0-beta2.jar",
            "j-1.0.0-SNAPSHOT.jar",
            "k-1.0.0-SNAPSHOT.2.jar",
            "l-1.0.0-RELEASE.jar",
            "m-1.2.3.jar",
            "m-0.2.3-ASDF.jar.bak",
            "m-0.2.3-QWER.jar.bak",
            "n-1.0.0-AAA.jar",
            "n-1.0.0-BBB.jar.bak"
        );
    }

    @Test
    void testCopyIfGreaterThenDelete() throws IOException {
        JarCopier copier = new JarCopier(OnJarConflict.DEFAULT
                .withSourceAction(SourceAction.COPY_IF_GREATER)
                .withTargetAction(TargetAction.DELETE));
        copier.copyJarDirectory(sourceDir.toString(), targetDir.toString());

        JarTestUtil.assertDirOnlyHasFiles(targetDir,
            "noversion.jar",
            "a-1.0.0.jar",
            "b-1.1.0.jar",
            "c-2.jar",
            "d-2.1.jar",
            "e-1.0.0.jar",
            "f-1.0.0.jar",
            "g-1.0.0-RC1.jar",
            "h-1.0.0beta.jar",
            "i-1.0.0-beta2.jar",
            "j-1.0.0-SNAPSHOT.jar",
            "k-1.0.0-SNAPSHOT.2.jar",
            "l-1.0.0-RELEASE.jar",
            "m-1.2.3.jar",
            "n-1.0.0-BBB.jar"
        );
    }

    @Test
    void testCopyThenNOOP() throws IOException {
        JarCopier copier = new JarCopier(OnJarConflict.DEFAULT
                .withSourceAction(SourceAction.COPY)
                .withTargetAction(TargetAction.NOOP));
        copier.copyJarDirectory(sourceDir.toString(), targetDir.toString());

        JarTestUtil.assertDirOnlyHasFiles(targetDir,
            "noversion.jar",
            "a-1.0.0.jar",
            "b-1.1.0.jar",
            "b-1.0.3.jar",
            "c-1.2.3.jar",
            "c-2.jar",
            "d-2.1.jar",
            "d.jar",
            "e-1.0.0.jar",
            "f-1.0.0-M1.jar",
            "f-1.0.0.jar",
            "g-1.0.0M1.jar",
            "g-1.0.0-RC1.jar",
            "h-1.0.0-alpha.jar",
            "h-1.0.0beta.jar",
            "i-1.0.0-beta2.jar",
            "i-1.0.0beta1.jar",
            "j-1.0.0-SNAPSHOT.jar",
            "k-1.0.0-SNAPSHOT.2.jar",
            "l-1.0.0-RELEASE.jar",
            "m-1.2.3.jar",
            "m-1.1.0.jar",
            "m-0.2.3-ASDF.jar",
            "m-0.2.3-QWER.jar",
            "n-1.0.0-AAA.jar",
            "n-1.0.0-BBB.jar"
        );
    }

    @Test
    void testNOOPThenRENAME() throws IOException {
        // nothing gets copied on conflict, but dup targets are renamed.
        JarCopier copier = new JarCopier(OnJarConflict.DEFAULT
                .withSourceAction(SourceAction.NOOP)
                .withTargetAction(TargetAction.RENAME));
        copier.copyJarDirectory(sourceDir.toString(), targetDir.toString());

        JarTestUtil.assertDirOnlyHasFiles(targetDir,
            "noversion.jar",
            "a-1.0.0.jar",
            "b-1.0.3.jar",
            "c-1.2.3.jar",
            "d.jar",
            "e-1.0.0.jar",
            "f-1.0.0.jar",
            "g-1.0.0M1.jar",
            "h-1.0.0beta.jar",
            "i-1.0.0beta1.jar",
            "j-1.0.0-SNAPSHOT.jar",
            "k-1.0.0-SNAPSHOT.2.jar",
            "l-1.0.0-RELEASE.jar",
            "m-1.2.3.jar",
            "m-0.2.3-ASDF.jar.bak",
            "m-0.2.3-QWER.jar.bak",
            "n-1.0.0-BBB.jar"
        );
    }

    @Test
    void testCmdLine() throws IOException {
        // needs at least 1 argument
        assertThrows(IllegalArgumentException.class, () ->
                Mocker.mockStdInOutErr(
                        JarCopier::doMain));

        assertCmdLineResults("m-1.1.0.jar", "1", "1", "SKIPPED",
                "m-1.2.3.jar",
                "m-0.2.3-ASDF.jar.bak",
                "m-0.2.3-QWER.jar.bak");

        assertCmdLineResults("n-1.0.0-AAA.jar", "2", "1", "SKIPPED",
                "n-1.0.0-BBB.jar");
        assertCmdLineResults("n-1.0.0-AAA.jar", "1", "1", "COPIED",
                "n-1.0.0-AAA.jar",
                "n-1.0.0-BBB.jar.bak");

        assertCmdLineResults("n-1.0.0-AAA.jar", "1", "1", "COPIED",
                "n-1.0.0-AAA.jar",
                "n-1.0.0-BBB.jar.bak");

        assertCmdLineResults("b-1.1.0.jar", "4", "1", "SKIPPED",
                "b-1.0.3.jar");

        assertCmdLineResults("b-1.0.0.jar", "5", "3", "COPIED",
                "b-1.0.0.jar",
                "b-1.0.3.jar");
    }

    @Test
    void testErrors() throws IOException {
        JarCopier.commandLine = true;
        Path badDir = tempDir.resolve("badDir");
        Files.createFile(badDir);

        JarCopier jarCopier = new JarCopier();
        String out;

        // invalid source dir
        out = Mocker.mockStdInOutErr(() ->
            jarCopier.copyJarDirectory(badDir.toFile(), targetDir.toFile()));
        assertThat(out).contains("Invalid source directory");

        // invalid target dir
        out = Mocker.mockStdInOutErr(() ->
            jarCopier.copyJarDirectory(sourceDir.toFile(), badDir.toFile()));
        assertThat(out).contains("Invalid target directory");

        out = Mocker.mockStdInOutErr(() ->
            jarCopier.copyJarFile(
                    new JarFile(sourceDir.resolve("e-1.0.0.jar").toFile()),
                            badDir.toFile()));
        assertThat(out).contains("Invalid target directory");

        // source dir empty
        File emptySourceDir =
                Files.createDirectory(sourceDir.resolve("emptyDir")).toFile();
        out = Mocker.mockStdInOutErr(() ->
            jarCopier.copyJarDirectory(emptySourceDir, targetDir.toFile()));
        assertThat(out).contains("No jar files were found in");

        // Illegal argument
        assertThrows(IllegalArgumentException.class,
                () -> JarCopier.toOnJarConflict(6));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testMisc() throws IOException {
        JarCopier jarCopier = new JarCopier();
        jarCopier.copyJarFile(
                sourceDir.resolve("e-1.0.0.jar").toString(),
                targetDir.toString());

        JarTestUtil.assertDirContainsFiles(targetDir, "e-1.0.0.jar");

        assertThat(jarCopier.getStrategy()).isEqualTo(-1);

        assertThat(JarCopier.toOnJarConflict(
                JarCopier.STRATEGY_RENAME_COPY)).isEqualTo(new OnJarConflict(
                        SourceAction.COPY_IF_GREATER_OR_EQUIVALENT,
                        TargetAction.RENAME));
        assertThat(JarCopier.toOnJarConflict(
                JarCopier.STRATEGY_DELETE_COPY)).isEqualTo(new OnJarConflict(
                        SourceAction.COPY_IF_GREATER_OR_EQUIVALENT,
                        TargetAction.DELETE));
        assertThat(JarCopier.toOnJarConflict(
                JarCopier.STRATEGY_NO_COPY)).isEqualTo(new OnJarConflict(
                        SourceAction.NOOP,
                        TargetAction.NOOP));
        assertThat(JarCopier.toOnJarConflict(
                JarCopier.STRATEGY_PLAIN_COPY)).isEqualTo(new OnJarConflict(
                        SourceAction.COPY,
                        TargetAction.NOOP));
        assertThat(JarCopier.toOnJarConflict(
                JarCopier.STRATEGY_INTERACTIVE)).isNull();
    }

    //--- Private methods ------------------------------------------------------
//    private void assertTargetFileNames(String... names) throws IOException {
//        assertThat(Files.list(targetDir)
//                .map(p -> p.getFileName().toString()
//                        .replaceFirst("^(.*\\.bak)-\\d+$", "$1"))
//                .collect(Collectors.toList()))
//            .containsExactlyInAnyOrder(names);
//    }
//    private void assertTargetContains(String... names) throws IOException {
//        assertThat(Files.list(targetDir)
//                .map(p -> p.getFileName().toString()
//                        .replaceFirst("^(.*\\.bak)-\\d+$", "$1"))
//                .collect(Collectors.toList()))
//            .contains(names);
//    }
    private void assertCmdLineResults(
            String sourceFileName, String sourceAction, String targetAction,
            String outContains, String... expectedFiles) throws IOException {
        String out = Mocker.mockStdInOutErr(() ->
            JarCopier.doMain(sourceDir.resolve(sourceFileName).toString()),
        targetDir.toAbsolutePath().toString(), sourceAction, targetAction);
        assertThat(out).contains(outContains);
        JarTestUtil.assertDirContainsFiles(targetDir, expectedFiles);
    }

//    private void jarsSameDate(String sourceAndTarget) throws IOException {
//        jarsSameDate(sourceAndTarget, sourceAndTarget);//, true);
//    }
//    private void jarsSameDate(String source, String target) throws IOException {
//        FileTime lastModified = null;
//        if (source != null) {
//            lastModified = Files.getLastModifiedTime(
//                    Files.createFile(sourceDir.resolve(source)));
//        }
//        if (target != null) {
//            Path file = Files.createFile(targetDir.resolve(target));
//            if (lastModified != null) {
//                Files.setLastModifiedTime(file, lastModified);
//            }
//        }
//    }
//
//    private FileTime sourceJar(String source) throws IOException {
//        return Files.getLastModifiedTime(
//                Files.createFile(sourceDir.resolve(source)));
//    }
//    private void targetJar(String target) throws IOException {
//        targetJar(target, null);
//    }
//    private void targetJar(String target, FileTime time) throws IOException {
//        Path file = Files.createFile(targetDir.resolve(target));
//        if (time != null) {
//            Files.setLastModifiedTime(file, time);
//        }
//    }
}
