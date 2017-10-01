/* Copyright 2010-2017 Norconex Inc.
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
import java.util.Date;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.Sleeper;
import com.norconex.commons.lang.StringUtil;
import com.norconex.commons.lang.io.IInputStreamFilter;
import com.norconex.commons.lang.io.ReverseFileInputStream;

/**
 * Utility methods when dealing with files and directories.
 * @author Pascal Essiembre
 */
public final class FileUtil {

    private static final Logger LOG = LogManager.getLogger(FileUtil.class);
    
    private static final int MAX_FILE_OPERATION_ATTEMPTS = 10;
    
    private FileUtil() {
        super();
    }

    /**
     * Converts any String to a valid file-system file name representation. The 
     * valid file name is constructed so it can be written to virtually any 
     * operating system.
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
        for (int i = 0; i < safeFileName.length(); i++){
            char ch = safeFileName.charAt(i);
            if (ch == '_') {
                String intVal = StringUtils.substring(safeFileName, i + 1, 
                        StringUtils.indexOf(safeFileName, '_', i + 1));
                b.append((char) NumberUtils.toInt(intVal));
                i += intVal.length() + 1;
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }
    
    
    /**
     * Moves a file to a directory.   Like {@link #moveFile(File, File)}:
     * <ul>
     *   <li>If the target directory does not exists, it creates it first.</li>
     *   <li>If the target file already exists, it deletes it first.</li>
     *   <li>If target file deletion does not work, it will try 10 times,
     *       waiting 1 second between each try to give a chance to whatever
     *       OS lock on the file to go.</li>
     *   <li>It throws a IOException if the move failed (as opposed to fail
     *       silently).</li>
     * </ul>
     * @param sourceFile source file to move
     * @param targetDir target destination
     * @throws IOException cannot move file.
     */
    public static void moveFileToDir(File sourceFile, File targetDir)
            throws IOException {
        if (sourceFile == null || !sourceFile.isFile()) {
            throw new IOException("Source file is not valid: " + sourceFile);
        }
        if (targetDir == null || 
                targetDir.exists() && !targetDir.isDirectory()) {
            throw new IOException("Target directory is not valid:" + targetDir);
        }
        if (!targetDir.exists()) {
            FileUtils.forceMkdir(targetDir);
        }
        
        String fileName = sourceFile.getName();
        File targetFile = new File(targetDir, fileName);
        moveFile(sourceFile, targetFile);
    }

    /**
     * Moves a file to a new file location.   This method is different from the
     * {@link File#renameTo(File)} method in such that:
     * <ul>
     *   <li>If the target file already exists, it deletes it first.</li>
     *   <li>If target file deletion does not work, it will try 10 times,
     *       waiting 1 second between each try to give a chance to whatever
     *       OS lock on the file to go.</li>
     *   <li>It throws a IOException if the move failed (as opposed to fail
     *       silently).</li>
     * </ul>
     * @param sourceFile source file to move
     * @param targetFile target destination
     * @throws IOException cannot move file.
     */
    public static void moveFile(File sourceFile, File targetFile)
            throws IOException {
        
        if (!isFile(sourceFile)) {
            throw new IOException(
                    "Source file is not a file or is not valid: " + sourceFile);
        }
        if (targetFile == null || targetFile.exists() && !targetFile.isFile()) {
            throw new IOException(
                    "Target file is not a file or is not valid: " + targetFile);
        }
        int failure = 0;
        Exception ex = null;
        while (failure < MAX_FILE_OPERATION_ATTEMPTS) {
            if (targetFile.exists() && !targetFile.delete()
                    || !sourceFile.renameTo(targetFile)) {
                failure++;
                Sleeper.sleepSeconds(1);
            } else {
                break;
            }
        }
        if (failure >= MAX_FILE_OPERATION_ATTEMPTS) {
            throw new IOException(
                    "Could not move \"" + sourceFile + "\" to \"" 
                            + targetFile + "\".", ex);
        }
    }
    
    /**
     * Deletes a file or a directory recursively in a more robust way. 
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
                System.gc();
                Sleeper.sleepSeconds(1);
                continue;
            }
            success = true;
        }
        if (!success) {
            throw new IOException(
                    "Could not delete \"" + file + "\".");
        }
    }
    
    /**
     * Deletes all directories that are empty from a given parent directory.
     * @param parentDir the directory where to start looking for empty 
     *        directories
     * @return the number of deleted directories
     */
    public static int deleteEmptyDirs(File parentDir) {
        return deleteEmptyDirs(parentDir, null);
    }

    /**
     * Deletes all directories that are empty and are <b>older</b> 
     * than the given date.  If the date is <code>null</code>, all empty 
     * directories will be deleted, regardless of their date.
     * @param parentDir the directory where to start looking for empty 
     *        directories
     * @param date the date to compare empty directories against
     * @return the number of deleted directories
     * @since 1.3.0
     */
    public static int deleteEmptyDirs(File parentDir, final Date date) {
        final MutableInt dirCount = new MutableInt(0);
        visitEmptyDirs(parentDir, new IFileVisitor() {
            @Override
            public void visit(File file) {
                if (date == null || FileUtils.isFileOlder(file, date)) {
                    String[] children = file.list();
                    if (file.isDirectory()
                            && (children == null || children.length == 0)) {
                        try {
                            FileUtil.delete(file);
                            dirCount.increment();
                        } catch (IOException e) {
                            LOG.error("Could not be delete directory: "
                                    + file, e);
                        }                        
                    }
                }
            }
        });
        return dirCount.intValue();
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
        if (!dir.exists()) {
            return;
        } else if (dir.isDirectory()) {
            File[] children = dir.listFiles(filter);
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    visitAllDirsAndFiles(children[i], visitor, filter);
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
        } else if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children == null || children.length == 0) {
                visitor.visit(dir);
            } else {
                for (int i=0; i<children.length; i++) {
                    visitAllDirs(new File(dir, children[i]), visitor);
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
        } else if (dir.isDirectory()) {
            File[] children = dir.listFiles(filter);
            if (children == null || children.length == 0) {
                visitor.visit(dir);
            } else {
                for (int i=0; i<children.length; i++) {
                    visitAllDirs(children[i], visitor, filter);
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
        } else if (dir.isDirectory()) {
            visitor.visit(dir);
            String[] children = dir.list();
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    visitAllDirs(new File(dir, children[i]), visitor);
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
        } else if (dir.isDirectory()) {
            visitor.visit(dir);
            File[] children = dir.listFiles(filter);
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    visitAllDirs(children[i], visitor, filter);
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
        } else if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    visitAllFiles(new File(dir, children[i]), visitor, filter);
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
            IInputStreamFilter filter)
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
                 if (!stripBlankLines || StringUtils.isNotBlank(line)) {
                     if (filter != null && filter.accept(line)) {
                         lines.addFirst(line);
                     } else {
                         remainingLinesToRead++;
                     }
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
            IInputStreamFilter filter)
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
                if (!stripBlankLines || StringUtils.isNotBlank(line)) {
                    if (filter != null && filter.accept(newLine)) {
                        lines.addFirst(newLine);
                    } else {
                        remainingLinesToRead++;
                    }
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
        return createDateTimeDirs(parentDir, date, "yyyy/MM/dd");
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
        return createDateTimeDirs(parentDir, dateTime, "yyyy/MM/dd/HH/mm/ss");
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
     * @param parentDir the parent directory where to create date directories
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
     * @param parentDir the parent directory where to create date directories
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
     * @param parentDir the parent directory where to create date directories
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
     * @param parentDir the parent directory where to create date directories
     * @param url the URL to create directories for, with file name.
     * @param truncate whether to truncate the directory to 255 characters max.
     * @return the directory representing the full path created, plus file name
     * @throws IOException if the parent directory is not valid
     */
    public static File createURLDirs(
            File parentDir, String url, boolean truncate) throws IOException {
        if (parentDir == null) {
            throw new IOException("Parent directory cannot be null.");
        }
        if (url == null) {
            throw new IOException("URL cannot be null.");
        }
        if (parentDir.exists() && !parentDir.isDirectory()) {
            throw new IOException("Parent directory \"" + parentDir 
                    + "\" already exists and is not a directory.");
        }
        if (truncate && parentDir.getAbsolutePath().length() > 200) {
            throw new IOException("Parent directory \"" + parentDir 
                    + "\" is too long (must be 200 characters or less).");
        }
        StringBuilder b = new StringBuilder(parentDir.getAbsolutePath());
        String[] segs = url.replaceFirst("://", "/") .split("/");
        for (String seg : segs) {
            b.append("/").append(toSafeFileName(seg));
        }
        String path = b.toString();
        if (truncate) {
            path = StringUtil.truncateWithHash(path, 255, '_');
        }
        File urlFile = new File(path);
        createDirsForFile(urlFile);
        return urlFile;
    }
    
    private static File createDateTimeDirs(
            File parentDir, Date dateTime, String format) throws IOException {
        if (parentDir == null) {
            throw new IOException("Parent directory cannot be null.");
        }
        if (dateTime == null) {
            throw new IOException("Date cannot be null.");
        }
        if (parentDir.exists() && !parentDir.isDirectory()) {
            throw new IOException("Parent directory \"" + parentDir 
                    + "\" already exists and is not a directory.");
        }
        File dateDir = new File(parentDir.getAbsolutePath(),
                DateFormatUtils.format(dateTime, format));
        FileUtils.forceMkdir(dateDir);
        return dateDir;
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

    //TODO make public as a null-safe method, along with similar methods?
    private static boolean isFile(File file) {
        return file != null && file.isFile();
    }

}
