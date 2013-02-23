package com.norconex.commons.lang.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;

/**
 * Utility methods when dealing with files.
 * @author Pascal Essiembre
 */
public final class FileUtils {

    private FileUtils() {
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
    	if (sourceFile == null || !sourceFile.isFile()) {
    		throw new IOException("Source file is not valid: " + sourceFile);
    	}
    	if (targetFile == null || targetFile.exists() && !targetFile.isFile()) {
    		throw new IOException("Target file is not valid: " + targetFile);
    	}
        boolean success = false;
        int failure = 0;
        Exception ex = null;
        while (!success && failure < 10) {
            if (targetFile.exists() && !targetFile.delete()
            		|| !sourceFile.renameTo(targetFile)) {
        		failure++;
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
		        	ex = e;
				}
        		continue;
            }
            success = true;
        }
        if (!success) {
        	throw new IOException(
        			"Could not move \"" + sourceFile + "\" to \"" 
        					+ targetFile + "\".", ex);
        }
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
    
}
