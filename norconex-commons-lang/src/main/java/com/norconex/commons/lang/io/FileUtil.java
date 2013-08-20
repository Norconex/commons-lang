/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.norconex.commons.lang.Sleeper;

/**
 * Utility methods when dealing with files and directories.
 * @author Pascal Essiembre
 */
public final class FileUtil {

    private static final int MAX_FILE_OPERATION_ATTEMPTS = 10;
    
    private FileUtil() {
        super();
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
    		targetDir.mkdirs();
    	}
    	
    	String fileName = sourceFile.getName();
    	File targetFile = new File(targetDir.getAbsolutePath()
    			+ SystemUtils.PATH_SEPARATOR + fileName);
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
        		continue;
            }
            break;
        }
        if (failure >= MAX_FILE_OPERATION_ATTEMPTS) {
        	throw new IOException(
        			"Could not move \"" + sourceFile + "\" to \"" 
        					+ targetFile + "\".", ex);
        }
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
        if (file == null || !file.exists()) {
            return;
        }
        boolean success = false;
        int failure = 0;
        Exception ex = null;
        while (!success && failure < MAX_FILE_OPERATION_ATTEMPTS) {
            if (file.exists() && !FileUtils.deleteQuietly(file)) {
                failure++;
                Sleeper.sleepSeconds(1);
                continue;
            }
            success = true;
        }
        if (!success) {
            throw new IOException(
                    "Could not delete \"" + file + "\".", ex);
        }
    }
    
    /**
     * Deletes all directories that are empty from a given parent directory.
     * @param parentDir the directory where to start looking for empty 
     *        directories
     * @return the number of deleted directories
     */
    public static int deleteEmptyDirs(File parentDir) {
        int count = 0;
        String[] files = parentDir.list(DirectoryFileFilter.INSTANCE);
        if (files == null) {
            return count;
        }
        for (String fileStr : files) {
            File file = new File(parentDir.getAbsolutePath() + "/" + fileStr);
            if (file.list().length == 0) {
                FileUtils.deleteQuietly(file);
                count++;
            } else {
                count += deleteEmptyDirs(file);
                if (file.list().length == 0) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }
        return count;
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
            for (int i=0; i<children.length; i++) {
                visitAllDirsAndFiles(children[i], visitor, filter);
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
            for (int i=0; i<children.length; i++) {
                visitAllDirs(new File(dir, children[i]), visitor);
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
            for (int i=0; i<children.length; i++) {
                visitAllFiles(new File(dir, children[i]), visitor);
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
            for (int i=0; i<children.length; i++) {
                visitAllFiles(children[i], visitor, filter);
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
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
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
        if (parentDir != null && parentDir.exists() 
                && !parentDir.isDirectory()) {
            throw new IOException("Parent directory \"" + parentDir 
                    + "\" already exists and is not a directory.");
        }
        File dateDir = new File(parentDir.getAbsolutePath()
                + "/" + DateFormatUtils.format(date, "yyyy/MM/dd"));

        dateDir.mkdirs();
        return dateDir;
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
        if (parentDir != null && parentDir.exists() 
                && !parentDir.isDirectory()) {
            throw new IOException("Parent directory \"" + parentDir 
                    + "\" already exists and is not a directory.");
        }
        File dateDir = new File(parentDir.getAbsolutePath()
                + "/" + DateFormatUtils.format(
                        dateTime, "yyyy/MM/dd/HH/mm/ss"));
        dateDir.mkdirs();
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
