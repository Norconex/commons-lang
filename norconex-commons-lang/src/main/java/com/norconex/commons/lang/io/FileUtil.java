/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.commons.lang.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods when dealing with files and directories.
 * @author Pascal Essiembre
 * @deprecated Since 1.4.0, use {@link com.norconex.commons.lang.file.FileUtil}
 */
@Deprecated
public final class FileUtil {

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
        return com.norconex.commons.lang.file.FileUtil.toSafeFileName(
                unsafeFileName);
    }
    /**
     * Converts a "safe" file name originally created with 
     * {@link #toSafeFileName(String)} into its original string.
     * @param safeFileName the file name to convert to its origianl form
     * @return original string
     */
    public static String fromSafeFileName(String safeFileName) {
        return com.norconex.commons.lang.file.FileUtil.fromSafeFileName(
                safeFileName);
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
        com.norconex.commons.lang.file.FileUtil.moveFileToDir(
            sourceFile, targetDir);
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
        com.norconex.commons.lang.file.FileUtil.moveFile(
                sourceFile, targetFile);
    }
    
    /**
     * Deletes a file or a directory recursively. This method does:
     * <ul>
     *   <li>If file or directory deletion does not work, it will try 10 times,
     *       waiting 1 second between each try to give a chance to whatever
     *       OS lock on the file to go.</li>
     *   <li>It throws a IOException if the delete failed (as opposed to fail
     *       silently).</li>
     *   <li>If file is <code>null</code> or does not exist, nothing happens.
     * </ul>
     * @param file file or directory to delete
     * @throws IOException cannot delete file.
     */
    public static void deleteFile(File file) throws IOException {
        com.norconex.commons.lang.file.FileUtil.delete(file);
    }
    
    /**
     * Deletes all directories that are empty from a given parent directory.
     * @param parentDir the directory where to start looking for empty 
     *        directories
     * @return the number of deleted directories
     */
    public static int deleteEmptyDirs(File parentDir) {
        return com.norconex.commons.lang.file.FileUtil.deleteEmptyDirs(
                parentDir);
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
        return com.norconex.commons.lang.file.FileUtil.deleteEmptyDirs(
                parentDir, date);
    }

    /**
     * Create all parent directories for a file if they do not exists.  
     * If they exist already, this method does nothing.  This method assumes
     * the last segment is a file or will be a file.
     * @param file the file to create parent directories for
     * @return The newly created parent directory
     * @throws IOException if somethign went wrong creating the parent 
     * directories
     */
    public static File createDirsForFile(File file) throws IOException {
        return com.norconex.commons.lang.file.FileUtil.createDirsForFile(file);
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
        if (!dir.exists()) {
            return;
        } else if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    visitAllFiles(new File(dir, children[i]), visitor);
                }
            }
        } else {
            visitor.visit(dir);
        }
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
            File[] children = dir.listFiles(filter);
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    visitAllFiles(children[i], visitor, filter);
                }
            }
        } else {
            visitor.visit(dir);
        }
    }

    /**
     * Returns the specified number of lines starting from the beginning
     * of a text file.
     * @param file the file to read lines from
     * @param numberOfLinesToRead the number of lines to read
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] head(File file, int numberOfLinesToRead)
            throws IOException {
        return head(file, Charsets.ISO_8859_1.toString(), numberOfLinesToRead);
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
        LinkedList<String> lines = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), encoding));

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
        reader.close();
        return lines.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Returns the specified number of lines starting from the end
     * of a text file.
     * @param file the file to read lines from
     * @param numberOfLinesToRead the number of lines to read
     * @return array of file lines
     * @throws IOException i/o problem
     */
    public static String[] tail(File file, int numberOfLinesToRead)
            throws IOException {
        return tail(file, Charsets.ISO_8859_1.toString(), numberOfLinesToRead);
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
        LinkedList<String> lines = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ReverseFileInputStream(file), encoding));
        int remainingLinesToRead = numberOfLinesToRead;
        String line = StringUtils.EMPTY;
        while(line != null && remainingLinesToRead-- > 0){
             line = StringUtils.defaultString(reader.readLine());
             char[] chars = line.toCharArray();
             for (int j = 0, k = chars.length - 1; j < k; j++, k--) {
                 char temp = chars[j];
                 chars[j] = chars[k];
                 chars[k] = temp;
             }
             String newLine = new String(chars);
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
        reader.close();
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
        return com.norconex.commons.lang.file.FileUtil.createDateDirs(
                parentDir);
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
        return com.norconex.commons.lang.file.FileUtil.createDateDirs(
                parentDir, date);
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
        return com.norconex.commons.lang.file.FileUtil.createDateTimeDirs(
                parentDir);
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
        return com.norconex.commons.lang.file.FileUtil.createDateTimeDirs(
                parentDir, dateTime);
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
}
