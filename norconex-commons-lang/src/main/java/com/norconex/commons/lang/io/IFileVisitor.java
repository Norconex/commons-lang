package com.norconex.commons.lang.io;

import java.io.File;

/**
 * Visitor to be used with <code>FileUtil.visit*</code> methods.
 * @see FileUtil
 * @author Pascal Essiembre
 */
public interface IFileVisitor {
    /**
     * Visits a file or directory.
     * @param file the file or directory being visited
     */
    void visit(File file);
}
