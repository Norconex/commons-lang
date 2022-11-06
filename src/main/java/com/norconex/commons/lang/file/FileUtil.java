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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.norconex.commons.lang.Sleeper;
import com.norconex.commons.lang.io.ReverseFileInputStream;
import com.norconex.commons.lang.text.StringUtil;

import lombok.NonNull;

/**
 * Utility methods when dealing with files and directories.
 * @author Pascal Essiembre
 */
public final class FileUtil {

    private static final int MAX_FILE_OPERATION_ATTEMPTS = 10;

    /** @since 3.0.0 */
    public static final Path[] EMPTY_PATH_ARRAY = {};
    /** @since 3.0.0 */
    public static final File[] EMPTY_FILE_ARRAY = {};

    private FileUtil() {}


    /**
     * Gets whether a directory is empty of files or directories
     * in an efficient way which does not load all files. The directory
     * must exist and be a valid directory (e.g., not a file).
     * @param dir the directory to check for emptiness
     * @return <code>true</code> if directory exists and is empty
     * @throws IOException if an I/O error occurs
     * @since 2.0.0
     */
    public static boolean dirEmpty(File dir) throws IOException {
        return dirEmpty(dir == null ? null : dir.toPath());
    }

    /**
     * Gets whether a directory is empty of files or directories
     * in an efficient way which does not load all files. The directory
     * must exist and be a valid directory (e.g., not a file).
     * @param dir the directory to check for emptiness
     * @return <code>true</code> if directory exists and is empty
     * @throws IOException if an I/O error occurs
     * @since 3.0.0
     */
    public static boolean dirEmpty(Path dir) throws IOException {
        if (dir == null || !Files.isDirectory(dir)) {
            throw new NotDirectoryException(
                    "Directory must exist and be valid: " + dir);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        }
    }

    /**
     * Recursively gets whether a directory contains at least one file
     * matching the given file filter.
     * @param dir directory to inspect
     * @param filter file or directory filter
     * @return <code>true</code> upon filter matching a file or directory
     * @throws IOException if an I/O error occurs
     */
    public static boolean dirHasFile(File dir, FileFilter filter)
            throws IOException {
        return dirHasFile(toPath(dir), filter);
    }

    /**
     * Recursively gets whether a directory contains at least one file
     * matching the given file filter.
     * @param dir directory to inspect
     * @param filter file or directory filter
     * @return <code>true</code> upon filter matching a file or directory
     * @throws IOException if an I/O error occurs
     * @since 3.0.0
     */
    public static boolean dirHasFile(Path dir, FileFilter filter)
            throws IOException {
        if (!isDirectory(dir)) {
            throw new NotDirectoryException(
                    "Directory does not exist or is otherwise not valid: "
                            + dir);
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .anyMatch(p -> filter.accept(p.toFile()));
        }
    }

    /**
     * Converts any String to a valid file-system file name representation. The
     * valid file name is constructed so it can be written to virtually any
     * operating system.  It will escape every characters that are not
     * alphanumeric, hyphen, or dot.
     * Use {@link #fromSafeFileName(String)} to get back the original name.
     * @param unsafeFileName the file name to make safe.
     * @return valid file name
     */
    public static String toSafeFileName(String unsafeFileName) {
        if (unsafeFileName == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < unsafeFileName.length(); i++){
            char ch = unsafeFileName.charAt(i);
            if (CharUtils.isAsciiAlphanumeric(ch) || ch == '-' || ch == '.') {
                b.append(ch);
            } else {
                b.append('_');
                b.append((int) ch);
                b.append('_');
            }
        }
        return b.toString();
    }

    /**
     * Converts a "safe" file name originally created with
     * {@link #toSafeFileName(String)} into its original string.
     * @param safeFileName the file name to convert to its original form
     * @return original string
     */
    public static String fromSafeFileName(String safeFileName) {
        if (safeFileName == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < safeFileName.length()) {
            char ch = safeFileName.charAt(i);
            if (ch == '_') {
                String intVal = StringUtils.substring(safeFileName, i + 1,
                        StringUtils.indexOf(safeFileName, '_', i + 1));
                b.append((char) NumberUtils.toInt(intVal));
                i += intVal.length() + 1;
            } else {
                b.append(ch);
            }
            i++;
        }
        return b.toString();
    }

    /**
     * Moves a file to a directory.   Like {@link #moveFile(File, File)}:
     * <ul>
     *   <li>If the target directory does not exists, it creates it first.</li>
     *   <li>If the target file already exists, it deletes it first.</li>
     *   <li>If target file deletion does not work, it will try 10 times,
     *       waiting half a second between each try to give a chance to
     *       whatever OS lock on the file to go.</li>
     *   <li>It throws a IOException if the move failed (as opposed to fail
     *       silently).</li>
     * </ul>
     * @param sourceFile source file to move
     * @param targetDir target destination
     * @return new location of moved file (since 3.0.0)
     * @throws IOException cannot move file.
     */
    public static File moveFileToDir(
            @NonNull File sourceFile, @NonNull File targetDir)
                    throws IOException {
        return moveFileToDir(toPath(sourceFile), toPath(targetDir)).toFile();
    }

    /**
     * Moves a file to a directory.   Like {@link #moveFile(File, File)}:
     * <ul>
     *   <li>If the target directory does not exists, it creates it first.</li>
     *   <li>If the target file already exists, it deletes it first.</li>
     *   <li>If target file deletion does not work, it will try 10 times,
     *       waiting half a second between each try to give a chance to
     *       whatever OS lock on the file to go.</li>
     *   <li>It throws a IOException if the move failed (as opposed to fail
     *       silently).</li>
     * </ul>
     * @param sourceFile source file to move
     * @param targetDir target destination
     * @return new location of moved file
     * @throws IOException cannot move file.
     * @since 3.0.0
     */
    public static Path moveFileToDir(
            @NonNull Path sourceFile, @NonNull Path targetDir)
                    throws IOException {
        if (!isFile(sourceFile)) {
            throw new IOException("Source file does not exist, is not a file, "
                    + "or is otherwise not valid: " + sourceFile);
        }
        boolean targetDirExists = Files.exists(targetDir);
        if (targetDirExists && !Files.isDirectory(targetDir)) {
            throw new IOException("Target directory is not valid:" + targetDir);
        }
        if (!targetDirExists) {
            Files.createDirectories(targetDir);
        }

        Path targetFile = targetDir.resolve(sourceFile.getFileName());
        moveFile(sourceFile, targetFile);
        return targetFile;
    }

    /**
     * Moves a file to a new file location.   This method is different from the
     * {@link File#renameTo(File)} method in such that:
     * <ul>
     *   <li>If the target file already exists, it deletes it first.</li>
     *   <li>If target file deletion does not work, it will try 10 times,
     *       waiting half a second between each try to give a chance to
     *       whatever OS lock on the file to go.</li>
     *   <li>It throws a IOException if the move failed (as opposed to fail
     *       silently).</li>
     *   <li><b>Since 3.0.0</b>, it attempts to create any missing directories
     *       in the target path.</li>
     * </ul>
     * @param sourceFile source file to move
     * @param targetFile target destination
     * @throws IOException cannot move file.
     */
    public static void moveFile(
            @NonNull File sourceFile, @NonNull File targetFile)
                    throws IOException {
        moveFile(toPath(sourceFile), toPath(targetFile));
    }

    /**
     * Moves a file to a new file location. This method is different from the
     * {@link File#renameTo(File)} method in such that:
     * <ul>
     *   <li>If the target file already exists, it deletes it first.</li>
     *   <li>If target file deletion does not work, it will try 10 times,
     *       waiting half a second between each try to give a chance to
     *       whatever OS lock on the file to go.</li>
     *   <li>It throws a IOException if the move failed (as opposed to fail
     *       silently).</li>
     *   <li>It attempts to create any missing directories
     *       in the target path.</li>
     * </ul>
     * @param sourceFile source file to move
     * @param targetFile target destination
     * @throws IOException cannot move file.
     * @since 3.0.0
     */
    public static void moveFile(
            @NonNull Path sourceFile, @NonNull Path targetFile)
                    throws IOException {
        if (!isFile(sourceFile)) {
            throw new IOException("Source file does not exist, is not a file, "
                    + "or is otherwise not valid: " + sourceFile);
        }
        int failure = 0;
        Exception ex = null;
        while (failure < MAX_FILE_OPERATION_ATTEMPTS) {
            try {
                Files.createDirectories(targetFile.getParent());
                Files.deleteIfExists(targetFile);
                if (Files.isRegularFile(Files.move(sourceFile, targetFile))) {
                    break;
                }
            } catch (Exception e) {
                ex = e;
                failure++;
                Sleeper.sleepMillis(500);
            }
        }
        if (failure >= MAX_FILE_OPERATION_ATTEMPTS) {
            throw new IOException(String.format(
                    "Could not move \"%s\" to \"%s\".",
                    sourceFile, targetFile), ex);
        }
    }

    /**
     * Deletes a file or empty directory recursively, in a more robust way.
     * This method applies the following strategies:
     * <ul>
     *   <li>If file or directory deletion does not work, it will re-try 10
     *       times, waiting 1 second between each try to give a chance to
     *       whatever OS lock on the file to go.</li>
     *   <li>After a first failed attempt, it invokes {@link System#gc()}
     *       in hope of releasing any handles left on files.  This is in
     *       relation to a known Java bug mostly occurring on Windows
     *   (<a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4715154"
     *   >http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4715154</a>).</li>
     *   <li>It throws a IOException if the delete still fails after the 10
     *       attempts (as opposed to fail silently).</li>
     *   <li>If file is <code>null</code> or does not exist, nothing happens.
     * </ul>
     * @param file file or directory to delete
     * @throws IOException cannot delete file.
     * @since 1.4. Renamed from <code>deleteFile(File)</code>
     */
    public static void delete(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        boolean success = false;
        int failure = 0;
        while (!success && failure < MAX_FILE_OPERATION_ATTEMPTS) {
            if (file.exists() && !FileUtils.deleteQuietly(file)) {
                failure++;
                System.gc(); //NOSONAR
                Sleeper.sleepSeconds(1);
                continue;
            }
            success = true;
        }
        if (!success) {
            throw new IOException(String.format(
                    "Could not delete \"%s\".", file));
        }
    }

    /**
     * Recursively deletes all empty directories.
     * The supplied directory itself is also considered for deletion.
     * Directories containing only empty child directories (and no files)
     * are also considered empty and will be deleted.
     * @param parentDir the directory where to start looking for empty
     *        directories
     * @return the number of deleted directories
     * @throws IOException error occurred deleting empty directories
     */
    public static int deleteEmptyDirs(File parentDir) throws IOException {
        return deleteEmptyDirs(parentDir, null);
    }

    /**
     * Recursively deletes all empty directories.
     * The supplied directory itself is also considered for deletion.
     * Directories containing only empty child directories (and no files)
     * are also considered empty and will be deleted.
     * @param parentDir the directory where to start looking for empty
     *        directories
     * @return the number of deleted directories
     * @throws IOException error occurred deleting empty directories
     */
    public static int deleteEmptyDirs(Path parentDir) throws IOException {
        return deleteEmptyDirs(parentDir, null);
    }

    /**
     * <p>
     * Recursively deletes all directories that are empty and are <b>older</b>
     * than the given date.  If the date is <code>null</code>, all empty
     * directories will be deleted, regardless of their date.
     * The supplied directory itself is also considered for deletion.
     * </p>
     * <p>
     * <b>Since 3.0.0</b>, directories containing only empty child directories
     * (and no files) are also considered empty and will be deleted.
     * </p>
     * @param parentDir the directory where to start looking for empty
     *        directories
     * @param date the date to compare empty directories against
     * @return the number of deleted directories
     * @throws IOException error occurred deleting empty directories
     * @since 1.3.0
     */
    public static int deleteEmptyDirs(File parentDir, final Date date)
            throws IOException {
        if (parentDir == null || !parentDir.isDirectory()) {
            return 0;
        }
        return deleteEmptyDirs(parentDir.toPath(), date);
    }

    /**
     * Recursively deletes all directories that are empty and are <b>older</b>
     * than the given date.  If the date is <code>null</code>, all empty
     * directories will be deleted, regardless of their date.
     * The supplied directory itself is also considered for deletion.
     * Directories containing only empty child directories (and no files)
     * are also considered empty and will be deleted.
     * @param parentDir the directory where to start looking for empty
     *        directories
     * @param date the date to compare empty directories against
     * @return the number of deleted directories
     * @throws IOException error occurred deleting empty directories
     * @since 3.0.0
     */
    public static int deleteEmptyDirs(Path parentDir, final Date date)
            throws IOException {
        int deletedCount = 0;
        if (parentDir == null || !Files.isDirectory(parentDir)) {
            return deletedCount;
        }

        // take care of child directories first
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                parentDir, Files::isDirectory)) {
            for (Path childDir : stream) {
                deletedCount += deleteEmptyDirs(childDir, date);
            }
        }

        // now that we took care of child directories, delete the current
        // one if old enough and empty.
        if ((date == null || isOlder(parentDir, date)) && dirEmpty(parentDir)) {
            delete(parentDir.toFile());
            deletedCount++;
        }
        return deletedCount;
    }

    /**
     * Create all parent directories for a file if they do not exists.
     * If they exist already, this method does nothing.  This method assumes
     * the last segment is a file or will be a file.
     * @param file the file to create parent directories for
     * @return The newly created parent directory
     * @throws IOException if something went wrong creating the parent
     * directories
     */
    public static File createDirsForFile(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            FileUtils.forceMkdir(parent);
            return parent;
        }
        return new File("/");
    }

    /**
     * Visits all files and directories under a directory.
     * @param dir the directory
     * @param visitor the visitor
     */
    public static void visitAllDirsAndFiles(File dir, IFileVisitor visitor) {
        visitAllDirsAndFiles(dir, visitor, null);
    }
    /**
     * Visits all files and directories under a directory.
     * @param dir the directory
     * @param visitor the visitor
     * @param filter an optional filter to restrict the files being visited
     */
    public static void visitAllDirsAndFiles(
            File dir, IFileVisitor visitor, FileFilter filter) {
        visitor.visit(dir);
        if (dir.exists() && dir.isDirectory()) {
            File[] children = dir.listFiles(filter);
            if (children != null) {
                for (File child : children) {
                    visitAllDirsAndFiles(child, visitor, filter);
                }
            }
        }
    }

    /**
     * Visits only empty directories under a directory.
     * @param dir the directory
     * @param visitor the visitor
     * @since 1.3.0
     */
    public static void visitEmptyDirs(File dir, IFileVisitor visitor) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (ArrayUtils.isEmpty(children)) {
                visitor.visit(dir);
            } else {
                for (String child : children) {
                    visitEmptyDirs(new File(dir, child), visitor);
                }
            }
        }
    }
    /**
     * Visits only empty directories under a directory.
     * @param dir the directory
     * @param visitor the visitor
     * @param filter an optional filter to restrict the visited directories
     * @since 1.3.0
     */
    public static void visitEmptyDirs(
            File dir, IFileVisitor visitor, FileFilter filter) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            File[] children = dir.listFiles(filter);
            if (children == null || children.length == 0) {
                visitor.visit(dir);
            } else {
                for (File child : children) {
                    visitEmptyDirs(child, visitor, filter);
                }
            }
        }
    }

    /**
     * Visits only directories under a directory.
     * @param dir the directory
     * @param visitor the visitor
     */
    public static void visitAllDirs(File dir, IFileVisitor visitor) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            visitor.visit(dir);
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    visitAllDirs(new File(dir, child), visitor);
                }
            }
        }
    }
    /**
     * Visits only directories under a directory.
     * @param dir the directory
     * @param visitor the visitor
     * @param filter an optional filter to restrict the visited directories
     * @since 1.3.0
     */
    public static void visitAllDirs(
            File dir, IFileVisitor visitor, FileFilter filter) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            visitor.visit(dir);
            File[] children = dir.listFiles(filter);
            if (children != null) {
                for (File child : children) {
                    visitAllDirs(child, visitor, filter);
                }
            }
        }
    }

    /**
     * Visits all files (and only files) under a directory, including
     * sub-directories.
     * @param dir the directory
     * @param visitor the visitor
     */
    public static void visitAllFiles(File dir, IFileVisitor visitor) {
        visitAllFiles(dir, visitor, null);
    }
    /**
     * Visits all files (and only files) under a directory, including
     * sub-directories.
     * @param dir the directory
     * @param visitor the visitor
     * @param filter an optional filter to restrict the files being visited
     */
    public static void visitAllFiles(
            File dir, IFileVisitor visitor, FileFilter filter) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    visitAllFiles(new File(dir, child), visitor, filter);
                }
            }
        } else if (filter == null || filter.accept(dir)) {
            visitor.visit(dir);
        }
    }

    /**
     * Returns the specified number of lines starting from the beginning
     * of a text file.
     * Since 1.5.0, UTF-8 is used as the default encoding.
     * @param file the file to read lines from
     * @param numberOfLinesToRead the number of lines to read
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] head(File file, int numberOfLinesToRead)
            throws IOException {
        return head(file, StandardCharsets.UTF_8.toString(),
                numberOfLinesToRead);
    }

    /**
     * Returns the specified number of lines starting from the beginning
     * of a text file, using the given encoding.
     * @param file the file to read lines from
     * @param encoding the file encoding
     * @param numberOfLinesToRead the number of lines to read
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] head(File file, String encoding,
            int numberOfLinesToRead) throws IOException {
        return head(file, encoding, numberOfLinesToRead, true);
    }
    /**
     * Returns the specified number of lines starting from the beginning
     * of a text file, using the given encoding.
     * @param file the file to read lines from
     * @param encoding the file encoding
     * @param numberOfLinesToRead the number of lines to read
     * @param stripBlankLines whether to return blank lines or not
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] head(File file, String encoding,
            int numberOfLinesToRead, boolean stripBlankLines)
            throws IOException {
        return head(file, encoding, numberOfLinesToRead, stripBlankLines, null);
    }
    /**
     * Returns the specified number of lines starting from the beginning
     * of a text file, using the given encoding.
     * @param file the file to read lines from
     * @param encoding the file encoding
     * @param numberOfLinesToRead the number of lines to read
     * @param stripBlankLines whether to return blank lines or not
     * @param filter InputStream filter
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] head(File file, String encoding,
            final int numberOfLinesToRead, boolean stripBlankLines,
            Predicate<String> filter)
            throws IOException {
        assertFile(file);
        assertNumOfLinesToRead(numberOfLinesToRead);
        LinkedList<String> lines = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), encoding))) {
            int remainingLinesToRead = numberOfLinesToRead;
            String line = StringUtils.EMPTY;
            while(line != null && remainingLinesToRead-- > 0){
                 line = reader.readLine();
                 if ((!stripBlankLines || StringUtils.isNotBlank(line))
                         && (filter == null || filter.test(line))) {
                     lines.add(line);
                 } else {
                     remainingLinesToRead++;
                 }
            }
        }
        return lines.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Returns the specified number of lines starting from the end
     * of a text file.
     * Since 1.5.0, UTF-8 is used as the default encoding.
     * @param file the file to read lines from
     * @param numberOfLinesToRead the number of lines to read
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] tail(File file, int numberOfLinesToRead)
            throws IOException {
        return tail(file,
                StandardCharsets.UTF_8.toString(), numberOfLinesToRead);
    }

    /**
     * Returns the specified number of lines starting from the end
     * of a text file.
     * @param file the file to read lines from
     * @param encoding the file encoding
     * @param numberOfLinesToRead the number of lines to read
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] tail(File file, String encoding,
            int numberOfLinesToRead) throws IOException {
        return tail(file, encoding, numberOfLinesToRead, true);
    }

    /**
     * Returns the specified number of lines starting from the end
     * of a text file.
     * @param file the file to read lines from
     * @param encoding the file encoding
     * @param numberOfLinesToRead the number of lines to read
     * @param stripBlankLines whether to return blank lines or not
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] tail(File file, String encoding,
            int numberOfLinesToRead, boolean stripBlankLines)
            throws IOException {
        return tail(file, encoding, numberOfLinesToRead, stripBlankLines, null);
    }

    /**
     * Returns the specified number of lines starting from the end
     * of a text file.
     * @param file the file to read lines from
     * @param encoding the file encoding
     * @param numberOfLinesToRead the number of lines to read
     * @param stripBlankLines whether to return blank lines or not
     * @param filter InputStream filter
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] tail(File file, String encoding,
            final int numberOfLinesToRead, boolean stripBlankLines,
            Predicate<String> filter)
            throws IOException {
        assertFile(file);
        assertNumOfLinesToRead(numberOfLinesToRead);
        LinkedList<String> lines = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ReverseFileInputStream(file), encoding))) {
            int remainingLinesToRead = numberOfLinesToRead;

            String line;
            while ((line = reader.readLine()) != null) {
                if (remainingLinesToRead-- <= 0) {
                    break;
                }
                String newLine = StringUtils.reverse(line);
                if ((!stripBlankLines || StringUtils.isNotBlank(line))
                        && (filter == null || filter.test(newLine))) {
                    lines.addFirst(newLine);
                } else {
                    remainingLinesToRead++;
                }
            }
        }
        return lines.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Creates (if not already existing) a series of directories reflecting
     * the current date, up to the day unit, under a given parent directory.
     * For example, a date of 2000-12-31 will create the following directory
     * structure:
     * <code>
     *    /&lt;parentDir&gt;/2000/12/31/
     * </code>
     * @param parentDir the parent directory where to create date directories
     * @return the directory representing the full path created
     * @throws IOException if the parent directory is not valid
     */
    public static File createDateDirs(File parentDir) throws IOException {
        return createDateDirs(parentDir, new Date());
    }

    /**
     * Creates (if not already existing) a series of directories reflecting
     * a date, up to the day unit, under a given parent directory.  For example,
     * a date of 2000-12-31 will create the following directory structure:
     * <code>
     *    /&lt;parentDir&gt;/2000/12/31/
     * </code>
     * @param parentDir the parent directory where to create date directories
     * @param date the date to create directories from
     * @return the directory representing the full path created
     * @throws IOException if the parent directory is not valid
     */
    public static File createDateDirs(File parentDir, Date date)
            throws IOException {
        return createDateFormattedDirs(parentDir, date, "yyyy/MM/dd");
    }

    /**
     * Creates (if not already existing) a series of directories reflecting
     * the current date and time, up to the seconds, under a given parent
     * directory.   For example, a date of 2000-12-31T13:34:12 will create the
     * following directory structure:
     * <code>
     *    /&lt;parentDir&gt;/2000/12/31/13/34/12/
     * </code>
     * @param parentDir the parent directory where to create date directories
     * @return the directory representing the full path created
     * @throws IOException if the parent directory is not valid
     */
    public static File createDateTimeDirs(File parentDir) throws IOException {
        return createDateTimeDirs(parentDir, new Date());
    }
    /**
     * Creates (if not already existing) a series of directories reflecting
     * a date and time, up to the seconds, under a given parent directory.
     * For example,
     * a date of 2000-12-31T13:34:12 will create the following directory
     * structure:
     * <code>
     *    /&lt;parentDir&gt;/2000/12/31/13/34/12/
     * </code>
     * @param parentDir the parent directory where to create date directories
     * @param dateTime the date to create directories from
     * @return the directory representing the full path created
     * @throws IOException if the parent directory is not valid
     */
    public static File createDateTimeDirs(File parentDir, Date dateTime)
            throws IOException {
        return createDateFormattedDirs(
                parentDir, dateTime, "yyyy/MM/dd/HH/mm/ss");
    }

    /**
     * Creates (if not already existing) a series of directories reflecting
     * the specified date format (from {@link SimpleDateFormat}),
     * under a given parent directory.
     * Use forward slash in your date format for creating sub-directories.
     * For example,
     * a date of 2000-12-31T13:34:12 with a format of
     * <code>yyyy/MM/dd/HH-mm-ss</code> will create the following directory
     * structure:
     * <code>
     *    /&lt;parentDir&gt;/2000/12/31/13-34-12/
     * </code>
     * @param parentDir the parent directory where to create date directories
     * @param dateTime the date to create directories from
     * @param format the format to use for creating a date-formatted directory
     * @return the directory representing the full path created
     * @throws IOException if the parent directory is not valid
     * @since 2.0.0
     */
    public static File createDateFormattedDirs(
            @NonNull File parentDir, @NonNull Date dateTime, String format)
                    throws IOException {
        if (parentDir.exists() && !parentDir.isDirectory()) {
            throw new IOException(String.format("Parent directory \"%s\" "
                    + "already exists and is not a directory.", parentDir));
        }
        File dir = toDateFormattedDir(parentDir, dateTime, format);
        FileUtils.forceMkdir(dir);
        return dir;
    }

    /**
     * Gets (but does not create) a series of directories reflecting
     * the specified date format (from {@link SimpleDateFormat}),
     * under a given parent directory.
     * Use forward slash in your date format for creating sub-directories.
     * For example,
     * a date of 2000-12-31T13:34:12 with a format of
     * <code>yyyy/MM/dd/HH-mm-ss</code> will create the following directory
     * structure:
     * <code>
     *    /&lt;parentDir&gt;/2000/12/31/13-34-12/
     * </code>
     * @param parentDir the parent directory where to create date directories
     * @param dateTime the date to create directories from
     * @param format the format to use for creating a date-formatted directory
     * @return the directory representing the full path created
     * @since 2.0.0
     */
    public static File toDateFormattedDir(
            @NonNull File parentDir, @NonNull Date dateTime, String format) {
        return new File(parentDir.getAbsolutePath(),
                DateFormatUtils.format(dateTime, format));
    }

    /**
     * <p>Creates (if not already existing) a series of directories
     * matching URL segments, under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name (not created). The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * </p>
     * <p>
     * <b>Warning:</b> the path created may be too long for some file systems.
     * To avoid issues with file names being too long, consider truncating
     * the generated path by using
     * {@link #createURLDirs(File, URL, boolean)} instead.
     * </p>
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @return the directory representing the full path created, plus file name
     * @throws IOException if the parent directory is not valid
     */
    public static File createURLDirs(File parentDir, URL url)
            throws IOException {
        return createURLDirs(parentDir, url, false);
    }
    /**
     * <p>Creates (if not already existing) a series of directories
     * matching URL segments, under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name (not created). The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * </p>
     * <p>
     * <b>Warning:</b> the path created may be too long for some file systems.
     * To avoid issues with file names being too long, consider truncating
     * the generated path by using
     * {@link #createURLDirs(File, URL, boolean)} instead.
     * </p>
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @return the directory representing the full path created, plus file name
     * @throws IOException if the parent directory is not valid
     */
    public static File createURLDirs(File parentDir, String url)
            throws IOException {
        return createURLDirs(parentDir, url, false);
    }
    /**
     * Creates (if not already existing) a series of directories
     * matching URL segments, under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name (not created). The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * For the same reason,
     * the full path created can be truncated with a hash code if more
     * than 255 characters.  When truncating, the full path to the parent
     * directory must be 200 or less characters (to leave some room for the
     * URL path).
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @param truncate whether to truncate the directory to 255 characters max.
     * @return the directory representing the full path created, plus file name
     * @throws IOException if the parent directory is not valid
     */
    public static File createURLDirs(
            File parentDir, URL url, boolean truncate) throws IOException {
        if (url == null) {
            throw new IOException("URL cannot be null.");
        }
        return createURLDirs(parentDir, url.toString(), truncate);
    }
    /**
     * Creates (if not already existing) a series of directories
     * matching URL segments, under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name (not created). The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * For the same reason,
     * the full path created can be truncated with a hash code if more
     * than 255 characters.  When truncating, the full path to the parent
     * directory must be 200 or less characters (to leave some room for the
     * URL path).
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @param truncate whether to truncate the directory to 255 characters max.
     * @return the directory representing the full path created, plus file name
     * @throws IOException if the parent directory is not valid
     */
    public static File createURLDirs(
            @NonNull File parentDir, @NonNull String url, boolean truncate)
                    throws IOException {
        if (parentDir.exists() && !parentDir.isDirectory()) {
            throw new IOException(String.format("Parent directory \"%s\" "
                    + "already exists and is not a directory.", parentDir));
        }
        File dir = toURLDir(parentDir, url, truncate);
        createDirsForFile(dir);
        return dir;
    }

    /**
     * <p>Gets (but does not create) a directory matching URL segments,
     * under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name. The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * </p>
     * <p>
     * <b>Warning:</b> the path created may be too long for some file systems.
     * To avoid issues with file names being too long, consider truncating
     * the generated path by using
     * {@link #toURLDir(File, URL, boolean)} instead.
     * </p>
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @return the directory representing the full path created, plus file name
     * @since 2.0.0
     */
    public static File toURLDir(File parentDir, URL url) {
        return toURLDir(parentDir, url, false);
    }
    /**
     * <p>Gets (but does not create) a directory matching URL segments,
     * under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name. The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * </p>
     * <p>
     * <b>Warning:</b> the path created may be too long for some file systems.
     * To avoid issues with file names being too long, consider truncating
     * the generated path by using
     * {@link #toURLDir(File, URL, boolean)} instead.
     * </p>
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @return the directory representing the full path created, plus file name
     * @since 2.0.0
     */
    public static File toURLDir(File parentDir, String url) {
        return toURLDir(parentDir, url, false);
    }
    /**
     * Gets (but does not create) a directory matching URL segments,
     * under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name. The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * For the same reason,
     * the full path created can be truncated with a hash code if more
     * than 255 characters.  When truncating, the full path to the parent
     * directory must be 200 or less characters (to leave some room for the
     * URL path).
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @param truncate whether to truncate the directory to 255 characters max.
     * @return the directory representing the full path created, plus file name
     * @since 2.0.0
     */
    public static File toURLDir(
            @NonNull File parentDir, @NonNull URL url, boolean truncate) {
        return toURLDir(parentDir, url.toString(), truncate);
    }

    /**
     * Null-safe alternative to {@link File#isFile()}. A <code>null</code> file
     * always returns <code>false</code>.
     * @param file the file to test
     * @return <code>true</code> if the file is not <code>null</code>, exists,
     *     and is a regular file
     * @since 3.0.0
     */
    public static boolean isFile(File file) {
        return file != null && file.isFile();
    }

    /**
     * Null-safe alternative to
     * {@link Files#isRegularFile(Path, java.nio.file.LinkOption...)}.
     * A <code>null</code> file always returns <code>false</code>.
     * @param file the file to test
     * @return <code>true</code> if the file is not <code>null</code>, exists,
     *     and is a regular file
     * @since 3.0.0
     */
    public static boolean isFile(Path file) {
        return file != null && Files.isRegularFile(file);
    }

    /**
     * Null-safe alternative to
     * {@link Files#isDirectory(Path, java.nio.file.LinkOption...)}
     * A <code>null</code> directory always returns <code>false</code>.
     * @param dir the directory to test
     * @return <code>true</code> if the directory is not <code>null</code>,
     *     exists, and is a directory
     * @since 3.0.0
     */
    public static boolean isDirectory(File dir) {
        return dir != null && dir.isDirectory();
    }

    /**
     * Null-safe alternative to
     * {@link Files#isDirectory(Path, java.nio.file.LinkOption...)}
     * A <code>null</code> directory always returns <code>false</code>.
     * @param dir the directory to test
     * @return <code>true</code> if the directory is not <code>null</code>,
     *     exists, and is a directory
     * @since 3.0.0
     */
    public static boolean isDirectory(Path dir) {
        return dir != null && Files.isDirectory(dir);
    }

    /**
     * Null-safe alternative to {@link File#toPath()}. A <code>null</code> file
     * returns <code>null</code>.
     * @param file the file to convert
     * @return a {@link Path} or <code>null</code> if file is <code>null</code>
     * @since 3.0.0
     */
    public static Path toPath(File file) {
        return file != null ? file.toPath() : null;
    }

    /**
     * Converts all supplied files to {@link Path} (<code>null</code> entries
     * remain <code>null</code>).
     * @param files the files to convert
     * @return an array of {@link Path}, never <code>null</code>
     * @since 3.0.0
     */
    public static Path[] toPaths(File[] files) {
        if (ArrayUtils.isEmpty(files)) {
            return EMPTY_PATH_ARRAY;
        }
        return toPaths(Arrays.asList(files)).toArray(EMPTY_PATH_ARRAY);
    }
    /**
     * Converts all supplied files to {@link Path} (<code>null</code> entries
     * remain <code>null</code>).
     * @param files the files to convert
     * @return a list of {@link Path}, never <code>null</code>
     * @since 3.0.0
     */
    public static List<Path> toPaths(Collection<File> files) {
        return CollectionUtils.emptyIfNull(files).stream()
            .filter(Objects::nonNull)
            .map(File::toPath)
            .collect(Collectors.toList());
    }

    /**
     * Gets (but does not create) a directory matching URL segments,
     * under a given parent directory.
     * The returned file contains the full path to the directories,
     * plus the file name. The file name is the last URL
     * segment (including query string and fragment).  Non-alphanumeric
     * characters are escaped to be file-system-friendly.
     * For the same reason,
     * the full path created can be truncated with a hash code if more
     * than 255 characters.  When truncating, the full path to the parent
     * directory must be 200 or less characters (to leave some room for the
     * URL path).
     * @param parentDir the parent directory where to create URL directories
     * @param url the URL to create directories for, with file name.
     * @param truncate whether to truncate the directory to 255 characters max.
     * @return the directory representing the full path created, plus file name
     * @since 2.0.0
     */
    public static File toURLDir(
            @NonNull File parentDir, @NonNull String url, boolean truncate) {
        if (truncate && parentDir.getAbsolutePath().length() > 200) {
            throw new IllegalArgumentException(String.format(
                    "Parent directory \"%s\" is too long (must be 200 "
                    + "characters or less).", parentDir));
        }
        StringBuilder b = new StringBuilder(parentDir.getAbsolutePath());
        String[] segs = url.replaceFirst("://", "/") .split("/");
        for (String seg : segs) {
            b.append("/").append(toSafeFileName(seg));
        }
        String path = b.toString();
        if (truncate) {
            path = StringUtil.truncateWithHash(path, 255, "_");
        }
        return new File(path);
    }

    private static void assertNumOfLinesToRead(int num) {
        if (num <= 0) {
            throw new IllegalArgumentException(
                    "Not a valid number to read: " + num);
        }
    }

    private static void assertFile(File file) throws IOException {
        if (file == null || !file.exists()
                || !file.isFile() || !file.canRead()) {
            throw new IOException("Not a valid file: " + file);
        }
    }

    private static boolean isOlder(
            @NonNull Path file, @NonNull Date date) throws IOException {
        return Files.getLastModifiedTime(file)
                .toInstant().isBefore(date.toInstant());
    }
}
