/* Copyright 2010-2022 Norconex Inc.
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
package com.norconex.commons.lang.file;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.time.DateUtil;

class FileUtilTest {

    @TempDir
    File tempDir;

    @Test
    void testDirEmpty() throws IOException {
        File nonEmptyParentDir = new File(tempDir, "testDirEmptyParent");
        FileUtils.forceMkdir(nonEmptyParentDir);
        File childDir = new File(nonEmptyParentDir, "childDir");
        FileUtils.forceMkdir(childDir);
        File childFile = new File(childDir, "child.txt");
        writeFile(childFile, "child");

        File emptyDir = new File(tempDir, "testDirEmpty");
        FileUtils.forceMkdir(emptyDir);

        assertThat(FileUtil.dirEmpty(nonEmptyParentDir)).isFalse();
        assertThat(FileUtil.dirEmpty(emptyDir)).isTrue();

        assertThrows(IOException.class, () ->  //NOSONAR
                FileUtil.dirEmpty((File) null));
    }

    @Test
    void testDirHasFile() throws IOException {
        File nonEmptyParentDir = new File(tempDir, "testHasFileDirParent");
        FileUtils.forceMkdir(nonEmptyParentDir);
        File childDir = new File(nonEmptyParentDir, "childDir");
        FileUtils.forceMkdir(childDir);
        File childFile = new File(childDir, "child.txt");
        writeFile(childFile, "child");

        File emptyDir = new File(tempDir, "testHasFileDirEmpty");
        FileUtils.forceMkdir(emptyDir);

        assertThat(FileUtil.dirHasFile(nonEmptyParentDir, f -> true)).isTrue();
        assertThat(FileUtil.dirHasFile(emptyDir, f -> true)).isFalse();

        assertThat(FileUtil.dirHasFile(nonEmptyParentDir,
                f -> f.getName().endsWith("child.txt"))).isTrue();
        assertThat(FileUtil.dirHasFile(nonEmptyParentDir,
                f -> f.getName().endsWith("nope.txt"))).isFalse();
        assertThat(FileUtil.dirHasFile(emptyDir,
                f -> f.getName().endsWith("child.txt"))).isFalse();

        assertThrows(IOException.class, () ->  //NOSONAR
                FileUtil.dirHasFile((File) null, f -> true));
    }


    @Test
    void testSafeFileName() {
        String unsafe = "Voilà, à bientôt! :-)";
        String safe = FileUtil.toSafeFileName(unsafe);
        Assertions.assertEquals(unsafe, FileUtil.fromSafeFileName(safe));

        assertThat(FileUtil.toSafeFileName(null)).isNull();
        assertThat(FileUtil.fromSafeFileName(null)).isNull();
    }

    @Test
    void testMoveFileToDir() throws IOException {
        File sourceDir = new File(tempDir, "moveToDirSourceDir");
        FileUtils.forceMkdir(sourceDir);
        File sourceFile = new File(sourceDir, "source.txt");
        writeFile(sourceFile, "source");

        File targetDir = new File(tempDir, "moveToDirTargetDir");
        File targetFile = FileUtil.moveFileToDir(sourceFile, targetDir);
        assertThat(sourceFile).doesNotExist();
        assertThat(readFile(targetFile)).contains("source");

        // moving dir
        assertThrows(IOException.class, () ->  //NOSONAR
                FileUtil.moveFileToDir(sourceDir, targetDir));
        // moving non existing source
        assertThrows(IOException.class, () ->  //NOSONAR
                FileUtil.moveFileToDir(new File(sourceDir, "bad.bad"),
                        targetDir));
        // moving over existing file instead of dir
        File someFile = new File(tempDir, "someFile.txt");
        writeFile(someFile, "some file");
        assertThrows(IOException.class, () ->  //NOSONAR
                FileUtil.moveFileToDir(targetFile, someFile));
    }

    @Test
    void testMoveFile() throws IOException {
        File sourceDir = new File(tempDir, "moveSourceDir");
        FileUtils.forceMkdir(sourceDir);
        File sourceFile = new File(sourceDir, "source.txt");
        writeFile(sourceFile, "source");

        File targetDir = new File(tempDir, "moveTargetDir");
        FileUtils.forceMkdir(targetDir);
        File targetFile = new File(targetDir, "target.txt");
        writeFile(targetFile, "target");

        // move over existing
        FileUtil.moveFile(sourceFile, targetFile);
        assertThat(sourceFile).doesNotExist();
        assertThat(readFile(targetFile)).contains("source");

        // move over new location
        File fileNewLoc = new File(tempDir, "a/b/c/new.txt");

        FileUtil.moveFile(targetFile, fileNewLoc);
        assertThat(targetFile).doesNotExist();
        assertThat(readFile(fileNewLoc)).contains("source");

        // null/bad files
        assertThrows(NullPointerException.class, () ->  //NOSONAR
                FileUtil.moveFile(null, new File(targetDir, "tblah")));
        assertThrows(NullPointerException.class, () ->  //NOSONAR
                FileUtil.moveFile(new File(sourceDir, "sblah").toPath(), null));
        assertThrows(IOException.class, () ->  //NOSONAR
                FileUtil.moveFile(new File(sourceDir, "bad.bad"), targetFile));

        // Over a non-empty directory
        File nonEmptyDir = new File(tempDir, "nonEmptyDir");
        FileUtils.forceMkdir(nonEmptyDir);
        writeFile(new File(nonEmptyDir, "someFile"), "source");
        assertThrows(IOException.class, () ->  //NOSONAR
                FileUtil.moveFile(fileNewLoc, nonEmptyDir));
    }

    @Test
    void testDelete() throws IOException {
        File parentDir = new File(tempDir, "deleteTest");
        FileUtils.forceMkdir(parentDir);
        File parentFile = new File(parentDir, "parent.txt");
        writeFile(parentFile, "parent");

        File childDir = new File(parentDir, "child");
        FileUtils.forceMkdir(childDir);
        File childFile = new File(childDir, "child.txt");
        writeFile(childFile, "child");

        assertThat(parentFile).isFile();
        assertThat(childFile).isFile();

        FileUtil.delete(parentDir);

        assertThat(parentDir).doesNotExist();
        assertThat(parentFile).doesNotExist();
        assertThat(childDir).doesNotExist();
        assertThat(childFile).doesNotExist();

        assertDoesNotThrow(() -> FileUtil.delete(null));
        assertDoesNotThrow(() -> FileUtil.delete(new File("/badOne.bad")));
    }

    @Test
    void testDeleteEmptyDirs() throws IOException {
        File parentDir = new File(tempDir, "deleteEmptyDirs");
        FileUtils.forceMkdir(parentDir);
        File child1 = new File(parentDir, "child1");
        FileUtils.forceMkdir(child1);
        File child1subA = new File(child1, "child1subA");
        FileUtils.forceMkdir(child1subA);
        File child1subB = new File(child1, "child1subB");
        FileUtils.forceMkdir(child1subB);
        File child1subBsubBB = new File(child1subB, "child1subBsubBB");
        FileUtils.forceMkdir(child1subBsubBB);
        File child2 = new File(parentDir, "child2");
        FileUtils.forceMkdir(child2);
        File child3 = new File(parentDir, "child3");
        FileUtils.forceMkdir(child3);

        // only child1subA and child3 have files
        writeFile(new File(child1subA, "child1subA.txt"), "child1subA");
        writeFile(new File(child3, "child3.txt"), "child3");

        List<String> dirs = new ArrayList<>();
        int deletedCount;

        // with date older than folder creation date, no deletion
        dirs.clear();
        deletedCount = FileUtil.deleteEmptyDirs(
                parentDir, DateUtil.toDate(LocalDate.of(2000, 1, 1)));
        FileUtil.visitAllDirs(parentDir, f -> dirs.add(f.getName()));
        assertThat(deletedCount).isZero();
        assertThat(dirs).containsExactlyInAnyOrder(
                "deleteEmptyDirs", "child1", "child1subA", "child1subB",
                "child1subBsubBB", "child2", "child3");

        // without a date, all empty dirs (recursively) should be deleted
        dirs.clear();
        deletedCount = FileUtil.deleteEmptyDirs(parentDir);
        FileUtil.visitAllDirs(parentDir, f -> dirs.add(f.getName()));
        assertThat(deletedCount).isEqualTo(3);
        assertThat(dirs).containsExactlyInAnyOrder(
                "deleteEmptyDirs", "child1", "child1subA", "child3");

        // null and non-dir test
        assertThat(FileUtil.deleteEmptyDirs((Path) null)).isZero();
        assertThat(FileUtil.deleteEmptyDirs((File) null)).isZero();
        assertThat(FileUtil.deleteEmptyDirs(
                tempDir.toPath().resolve("bad"))).isZero();
        assertThat(FileUtil.deleteEmptyDirs(new File(tempDir, "bad"))).isZero();
    }

    @Test
    void testCreateDirsForFile() throws IOException {
        File file = new File(tempDir, "dirsForFile/deep1/deep2/file.txt");
        FileUtil.createDirsForFile(file);
        assertThat(file).doesNotExist();
        assertThat(file.getParentFile()).isDirectory().exists();
    }

    @Test
    void testVisit() throws IOException {
        File parentDir = new File(tempDir, "parentDir");
        FileUtils.forceMkdir(parentDir);
        writeFile(new File(parentDir, "a-parentFile.txt"), "parent");

        File childDir1 = new File(parentDir, "childDir1");
        FileUtils.forceMkdir(childDir1);
        writeFile(new File(childDir1, "b-childFile1.txt"), "child1");

        File childDir2 = new File(parentDir, "childDir2");
        FileUtils.forceMkdir(childDir2);
        writeFile(new File(childDir2, "c-childFile2.txt"), "child2");

        File childDir3 = new File(parentDir, "childDir3");
        FileUtils.forceMkdir(childDir3); // <-- Empty dir

        List<String> lines = new ArrayList<>();

        // visitAllDirsAndFiles - no filter
        lines.clear();
        FileUtil.visitAllDirsAndFiles(parentDir, f -> lines.add(f.getName()));
        assertThat(lines).containsExactlyInAnyOrder(
                "parentDir", "a-parentFile.txt",
                "childDir1", "b-childFile1.txt",
                "childDir2", "c-childFile2.txt",
                "childDir3");

        // visitAllDirsAndFiles - filter
        lines.clear();
        FileUtil.visitAllDirsAndFiles(
                parentDir,
                f -> lines.add(f.getName()),
                f -> !f.toString().endsWith("childDir1"));
        assertThat(lines).containsExactlyInAnyOrder(
                "parentDir", "a-parentFile.txt",
                "childDir2", "c-childFile2.txt",
                "childDir3");

        // visitEmptyDirs - no filter
        lines.clear();
        FileUtil.visitEmptyDirs(parentDir, f -> lines.add(f.getName()));
        assertThat(lines).containsExactly("childDir3");

        // visitEmptyDirs - filter
        lines.clear();
        FileUtil.visitEmptyDirs(
                parentDir,
                f -> lines.add(f.getName()),
                f -> !f.toString().endsWith("childDir3"));
        assertThat(lines).isEmpty();

        // visitAllDirs - no filter
        lines.clear();
        FileUtil.visitAllDirs(parentDir, f -> lines.add(f.getName()));
        assertThat(lines).containsExactlyInAnyOrder(
                "parentDir", "childDir1", "childDir2", "childDir3");

        // visitAllDirs - filter
        lines.clear();
        FileUtil.visitAllDirs(
                parentDir,
                f -> lines.add(f.getName()),
                f -> !f.toString().endsWith("childDir1"));
        assertThat(lines).containsExactlyInAnyOrder(
                "parentDir", "childDir2", "childDir3");

        // visitAllFiles - no filter
        lines.clear();
        FileUtil.visitAllFiles(parentDir, f -> lines.add(readFile(f)));
        assertThat(lines).containsExactlyInAnyOrder(
                "parent", "child1", "child2");

        // visitAllFiles - filter
        lines.clear();
        FileUtil.visitAllFiles(
                parentDir,
                f -> lines.add(readFile(f)),
                f -> !f.toString().endsWith("childFile1.txt"));
        assertThat(lines).containsExactlyInAnyOrder("parent", "child2");
    }

    @Test
    void testHead() throws IOException {
        File file = new File(tempDir, "head.txt");
        writeFile(file, "one\ntwo\n  \nthree\n\nfour\nfive\n");
        assertThat(file).exists();

        assertThat(FileUtil.head(file, UTF_8.toString(), 6, false))
            .containsExactly("one", "two", "  ", "three", "", "four");
        assertThat(FileUtil.head(file, 4))
            .containsExactly("one", "two", "three", "four");
    }

    @Test
    void testTail() throws IOException {
        File file = new File(tempDir, "tail.txt");
        writeFile(file, "one\ntwo\n\nthree\nfour\n   \nfive\n");
        assertThat(file).exists();

        assertThat(FileUtil.tail(file, UTF_8.toString(), 5, false))
            .containsExactly("three", "four", "   ", "five", "");
        assertThat(FileUtil.tail(file, 3))
            .containsExactly("three", "four", "five");
    }

    @Test
    void testCreateDateTimeDirs() throws IOException {
        LocalDateTime date = LocalDateTime.of(2022, 2, 28, 14, 52, 36);
        File file = FileUtil.createDateTimeDirs(tempDir, DateUtil.toDate(date));
        assertThat(file).exists().isDirectory();
        assertThat(file.getAbsolutePath().replace('\\', '/')).endsWith(
                "/2022/02/28/14/52/36");
    }

    @Test
    void testCreateDateDirs() throws IOException {
        LocalDate date = LocalDate.of(2022, 2, 28);
        File file = FileUtil.createDateDirs(tempDir, DateUtil.toDate(date));
        assertThat(file).exists().isDirectory();
        assertThat(file.getAbsolutePath().replace('\\', '/')).endsWith(
                "/2022/02/28");
    }

    @Test
    void testcreateURLDir() throws IOException {
        File file1 = FileUtil.createURLDirs(
                tempDir, new URL("http://norconex.com/some/test/page.html"));
        assertThat(file1).doesNotExist();
        assertThat(file1.getParentFile()).exists();
        assertThat(file1.getAbsolutePath().replace('\\', '/')).endsWith(
                "/http/norconex.com/some/test/page.html");
        File file2 = FileUtil.createURLDirs(
                tempDir, "http://norconex.com/some/test/page.html");
        assertThat(file1).isEqualTo(file2);
    }

    @Test
    void testToURLDir() throws MalformedURLException {
        File file1 = FileUtil.toURLDir(
                tempDir, "http://norconex.com/some/test/page.html");
        File file2 = FileUtil.toURLDir(
                tempDir, new URL("http://norconex.com/some/test/page.html"));

        assertThat(file1)
            .isEqualTo(file2)
            .doesNotExist();
        assertThat(file1.getAbsolutePath().replace('\\', '/')).endsWith(
                "/http/norconex.com/some/test/page.html");
    }

    private String readFile(File file) {
        try {
            return FileUtils.readFileToString(file, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    private void writeFile(File file, String text) {
        try {
            FileUtils.writeStringToFile(file, text, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
