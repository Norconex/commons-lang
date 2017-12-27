/* Copyright 2016-2017 Norconex Inc.
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.norconex.commons.lang.file.FileUtil;

/**
 * Performs a version-sensitive copy a Jar file or directory containing Jar 
 * files over to another directory.  When in interactive mode, the user will be
 * prompted to decide how to handle duplicate Jars. Interactive mode requires
 * this application to be run from a command prompt.  
 * @author Pascal Essiembre
 * @since 1.10.0
 */
public class JarCopier {

    private static final Logger LOG = LoggerFactory.getLogger(JarCopier.class);
    
    /**
     * Copy source Jar only if greater or same version as target
     * Jar after renaming target Jar (.bak-[timestamp]).
     */
    public static final int STRATEGY_RENAME_COPY = 1;
    /**
     * Copy source Jar only if greater or same version as target
     * Jar after deleting target Jar.
     */
    public static final int STRATEGY_DELETE_COPY = 2;
    /**
     * Do not copy source Jar (leave target Jar as is).
     */
    public static final int STRATEGY_NO_COPY = 3;
    /**
     * Copy source Jar regardless of target Jar
     * (may overwrite or cause mixed versions).
     */
    public static final int STRATEGY_PLAIN_COPY = 4;
    /**
     * Interactive, let the user chose (requires execution on command prompt).
     */
    public static final int STRATEGY_INTERACTIVE = 5;
    
    private final int strategy;
    private final Scanner scanner;

    /**
     * Constructor.
     * Only source Jars with greater or equal versions than 
     * their existing target will be copied over, and conflicting Jars will be 
     * renamed in the target directory (suffixed with .bak-[timestamp]).
     * Same as invoking {@link JarCopier#JarCopier(int)} with 
     * {@link #STRATEGY_RENAME_COPY}. 
     */
    public JarCopier() {
        this(STRATEGY_RENAME_COPY);
    }
    
    
    /**
     * Constructor.
     * @param strategy the strategy to use when encountering 
     *                 duplicates/conflicts
     */
    public JarCopier(int strategy) {
        super();
        if (strategy < 1 || strategy > 5) {
            throw new IllegalArgumentException("Invalid strategy: " + strategy);
        }
        this.strategy = strategy;
        if (strategy == STRATEGY_INTERACTIVE) {
            scanner = new Scanner(System.in);
        } else {
            scanner = null;
        }
    }

    /**
     * Gets the strategy used when encountering duplicates or version conflicts.
     * @return strategy id
     */
    public int getStrategy() {
        return strategy;
    }
    
    /**
     * Copies Jars from a source directory to a target one taking into 
     * consideration Jar versions.
     * @param fromJarDirectory directory to copy Jars from
     * @param toJarDirectory directory to copy Jars to
     * @throws IOException problem copying files
     */
    public void copyJarDirectory(
            String fromJarDirectory, String toJarDirectory) throws IOException {
        copyJarDirectory(new File(fromJarDirectory), new File(toJarDirectory));
    }
    /**
     * Copies Jars from a source directory to a target one taking into 
     * consideration Jar versions.
     * @param fromJarDirectory directory to copy Jars from
     * @param toJarDirectory directory to copy Jars to
     * @throws IOException problem copying files
     */
    public void copyJarDirectory(
            File fromJarDirectory, File toJarDirectory) throws IOException {

        if (!fromJarDirectory.isDirectory()) {
            error("Invalid source directory: " + fromJarDirectory);
            return;
        }
        if (!toJarDirectory.isDirectory()) {
            error("Invalid target directory: " + toJarDirectory);
            return;
        }
        File[] jarsToCopy = fromJarDirectory.listFiles(JarFile.FILTER);
        if (ArrayUtils.isEmpty(jarsToCopy)) {
            error("No jar files were found in " + fromJarDirectory);
            return;
        }
        
        List<JarDuplicates> dups = JarDuplicateFinder.findJarDuplicates(
                fromJarDirectory, toJarDirectory);

        info(String.format("%d duplicate jar(s) found.", dups.size()));
        int copyStrategy = strategy;
        if (strategy == STRATEGY_INTERACTIVE && !dups.isEmpty()) {
            copyStrategy = getDuplicatesHandlingUserGlobalChoice();
        }
        
        for (File file : jarsToCopy) {
            JarDuplicates dup = getDuplicates(dups, file);
            copyJarFile(file, toJarDirectory, dup, copyStrategy);
        }

        info("---");
        info("DONE");
    }

    /**
     * Copies a single Jar to a target directory, taking into 
     * consideration Jar versions.
     * @param sourceJarFile the Jar file to copy
     * @param toDirectory directory to copy the jar into
     * @throws IOException problem copying files
     */
    public void copyJarFile(
            String sourceJarFile, String toDirectory) throws IOException {
        copyJarFile(new File(sourceJarFile), new File(toDirectory));
    }
    /**
     * Copies a single Jar to a target directory, taking into 
     * consideration Jar versions.
     * @param sourceJarFile the Jar file to copy
     * @param toDirectory directory to copy the jar into
     * @throws IOException problem copying files
     */
    public void copyJarFile(
            File sourceJarFile, File toDirectory) throws IOException {
        
        if (!sourceJarFile.isFile() 
                || !sourceJarFile.getName().endsWith(".jar")) {
            error("File does not appear to be a Jar: " + sourceJarFile);
            return;
        }
        if (!toDirectory.isDirectory()) {
            error("Invalid target directory: " + toDirectory);
            return;
        }
        

        List<JarDuplicates> dups = JarDuplicateFinder.findJarDuplicates(
                sourceJarFile, toDirectory);

        info(String.format("%d duplicate jar(s) found.", dups.size()));
        
        JarDuplicates dup = getDuplicates(dups, sourceJarFile);
        copyJarFile(sourceJarFile, toDirectory, dup, strategy);

        info("---");
        info("DONE");
    }
    
    private void copyJarFile(
            File file, File targetDir, JarDuplicates dups, int copyStragegy)
                    throws IOException {
        info("---");
        // no duplicate, just copy
        if (dups == null) {
            copy(file, targetDir);
            return;
        }
        
        // duplicate! follow strategy
        JarFile sourceJar = getSourceJarFile(dups, file);
        JarFile targetJar = getTargetJarFile(dups, file);

        info("Duplicate:");
        info("  Source: " + sourceJar.getPath().getName());
        info("  Target: " + targetJar.getPath().getName());

        boolean copyOnlyIfSourceIsGreater = true;
        int finalStrategy = copyStragegy;
        
        if (finalStrategy == STRATEGY_INTERACTIVE) {
            finalStrategy = getDuplicatesHandlingUserFileChoice();
            copyOnlyIfSourceIsGreater = false;
        }
        
        if (finalStrategy == STRATEGY_RENAME_COPY) {
            if (!copyOnlyIfSourceIsGreater
                    || sourceJar.isVersionGreaterThan(targetJar)) {
                renameToBackup(targetJar);
                copy(file, targetDir);
            } else {
                info("No copy: target version is greater for \"" 
                        + file + "\".");
            }
            return;
        }
        if (finalStrategy == STRATEGY_DELETE_COPY) {
            if (!copyOnlyIfSourceIsGreater
                    || sourceJar.isVersionGreaterThan(targetJar)) {
                delete(targetJar);
                copy(file, targetDir);
            } else {
                info("No copy: target version is greater for \"" 
                        + file + "\".");
            }
            return;
        }
        if (finalStrategy == STRATEGY_NO_COPY) {
            info("No copy for \"" + file + "\".");
            return;
        }
        if (finalStrategy == STRATEGY_PLAIN_COPY) {
            copy(file, targetDir);
            return;
        }
    }

    private void copy(File sourceFile, File targetDir) throws IOException {
        info("Copying \"" + sourceFile + "\".");
        FileUtils.copyFileToDirectory(sourceFile, targetDir, true);
    }
    private void delete(JarFile file) throws IOException {
        info("Deleting \"" + file.getPath() + "\".");
        FileUtil.delete(file.getPath());
    }
    private void renameToBackup(JarFile file) throws IOException {
        File renamedFile = new File(file.getPath().getAbsolutePath()
                + ".bak-" + createTimestamp());
        info("Renaming \"" + file.getPath().getName() + "\" to \""
                + renamedFile.getName() + "\".");
        if (!file.getPath().renameTo(renamedFile)) {
            throw new IOException("Could not rename from \""
                    + file + "\" to \"" + renamedFile + "\".");
        }
    }
    private String createTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
    
    private JarFile getSourceJarFile(JarDuplicates dups, File sourceFile) {
        for (JarFile jarFile : dups.getJarFiles()) {
            if (jarFile.getPath().equals(sourceFile)) {
                return jarFile;
            }
        }
        return null;
    }
    private JarFile getTargetJarFile(JarDuplicates dups, File sourceFile) {
        for (JarFile jarFile : dups.getJarFiles()) {
            if (!jarFile.getPath().equals(sourceFile)) {
                return jarFile;
            }
        }
        return null;
    }
    
    private int getDuplicatesHandlingUserGlobalChoice() {
        info("");
        info("How do you want to handle duplicates? For each Jar...");
        info("");
        info("  1) Copy source Jar only if greater or same version as target");
        info("     Jar after renaming target Jar (preferred option).");
        info("");
        info("  2) Copy source Jar only if greater or same version as target");
        info("     Jar after deleting target Jar.");
        info("");
        info("  3) Do not copy source Jar (leave target Jar as is).");
        info("");
        info("  4) Copy source Jar regardless of target Jar");
        info("     (may overwrite or cause mixed versions).");
        info("");
        info("  5) Let me choose for each files.");

        while (true) {
            info("");
            System.out.print("Your choice (default = 1): ");
            String choiceStr = scanner.nextLine();
            if (StringUtils.isEmpty(choiceStr)) {
                return 1;
            }
            
            int choice = NumberUtils.toInt(choiceStr);
            if (choice < 1 || choice > 5) {
                info("");
                info("Wrong selection! Try again.");
            } else {
                return choice;
            }
        }
    }
    private int getDuplicatesHandlingUserFileChoice() {
        info("");
        info("Your action:");
        info("");
        info("  1) Copy source Jar after renaming target Jar.");
        info("");
        info("  2) Copy source Jar after deleting target Jar.");
        info("");
        info("  3) Do not copy source Jar (leave target Jar as is).");
        info("");
        info("  4) Copy source Jar regardless of target Jar (may overwrite");
        info("     or cause mixed versions, usually not recommended).");
        while (true) {
            info("");
            System.out.print("Your choice (default = 1): ");
            String choiceStr = scanner.nextLine();
            if (StringUtils.isEmpty(choiceStr)) {
                return 1;
            }
            
            int choice = NumberUtils.toInt(choiceStr);
            if (choice < 1 || choice > 4) {
                info("");
                info("Wrong selection! Try again.");
            } else {
                return choice;
            }
        }
    }

    
    private JarDuplicates getDuplicates(List<JarDuplicates> dups, File jar) {
        for (JarDuplicates dup : dups) {
            if (dup.contains(jar)) {
                return dup;
            }
        }
        return null;
    }

    private void error(String error) {
        if (strategy == STRATEGY_INTERACTIVE) {
            System.err.println(error);
        } else {
            LOG.error(error);
        }
    }
    private void info(String info) {
        if (strategy == STRATEGY_INTERACTIVE) {
            System.out.println(info);
        } else {
            LOG.info(info);
        }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Missing argument(s).");
            System.out.println("");
            System.out.println("Usage:");
            System.out.println("");
            System.out.println("  <app> path1 [path2]");
            System.out.println("");
            System.out.println("Where path1 can either point to a Jar or "
                    + "directory, and path2 must be a directory.");
            System.out.println("\"path2\" is optional: if not provided, you "
                    + "will be prompted for it.");
            System.exit(-1);
        }

        //--- Resolve source and target ---
        File source = new File(args[0]);
        boolean isSourceDirectory = source.isDirectory();
        if (!isSourceDirectory) {
            validateCommandLineJarPath(source);
        }
        File target;
        if (args.length == 2) {
            target = new File(args[1]);
        } else {
            target = getDirectoryFromCommandLinePrompt();
        }
        if (!target.exists()) {
            System.out.println(
                    "The target directory does not exist and will be created.");
            target.mkdirs();
        }

        //--- Invoke the proper copy method ---
        JarCopier jarCopier = new JarCopier(STRATEGY_INTERACTIVE);
        if (isSourceDirectory) {
            jarCopier.copyJarDirectory(source, target);
        } else {
            jarCopier.copyJarFile(source, target);
        }
    }
    
    private static File getDirectoryFromCommandLinePrompt() {
        System.out.println("Please enter a target directory:");
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        File file = new File(scanner.nextLine());
        if (file.exists() && !file.isDirectory()) {
            System.err.println(
                    "Path already exists and is not a directory: " + file);
            System.exit(-1);
        }
        return file;
    }
    
    private static void validateCommandLineJarPath(File path) {
        if (path.isFile() && path.getName().endsWith(".jar")) {
            return;
        }
        System.err.println("Path not a valid/existing Jar or directory: "
                + path.getAbsolutePath());
        System.exit(-1);
    }
}
