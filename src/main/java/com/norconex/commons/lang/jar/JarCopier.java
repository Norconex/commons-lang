/* Copyright 2016-2022 Norconex Inc.
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

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.stream.Streams;

import com.norconex.commons.lang.ExceptionUtil;
import com.norconex.commons.lang.file.FileUtil;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.SourceAction;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.TargetAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs a version-sensitive copy a Jar file or directory containing Jar
 * files over to another directory.  When in interactive mode, the user will be
 * prompted to decide how to handle duplicate Jars. Interactive mode requires
 * this application to be run from a command prompt.
 * @since 1.10.0
 */
@Slf4j
public class JarCopier {

    //MAYBE option to do a dry-run (show table-like findings + expected result)

    static boolean commandLine = false;

    private static final String OUTCOME_COPIED = "COPIED";
    private static final String OUTCOME_SKIPPED = "SKIPPED";

    private final BiFunction<JarFile, JarFile, OnJarConflict>
            onJarConflictSupplier;

    //--- Constructors ---------------------------------------------------------

    /**
     * Constructor.
     * Only source Jars with greater or equal versions than
     * their existing target will be copied over, and conflicting Jars will be
     * renamed in the target directory (suffixed with .bak-[timestamp]).
     */
    public JarCopier() {
        this((BiFunction<JarFile, JarFile, OnJarConflict>) null);
    }

    /**
     * Constructor with custom behavior for jar conflict resolution.
     * @param onJarConflict conflict resolution options
     * @since 3.0.0
     */
    public JarCopier(@NonNull OnJarConflict onJarConflict) {
        this((s, t) -> onJarConflict);
    }

    // at this point, treat null as interactive
    JarCopier(
            BiFunction<JarFile, JarFile, OnJarConflict> onJarConflictSupplier) {
        if (onJarConflictSupplier == null) {
            var ojc = new OnJarConflict();
            this.onJarConflictSupplier = (s, t) -> ojc;
        } else {
            this.onJarConflictSupplier = onJarConflictSupplier;
        }
    }

    //--- Copy Jar Directory ---------------------------------------------------

    /**
     * Copies jars from a source directory to a target one taking into
     * consideration potential jar duplicates.
     * @param sourceDirectory directory to copy Jars from
     * @param targetDirectory directory to copy Jars to
     * @throws IOException problem copying files
     */
    public void copyJarDirectory(
            @NonNull String sourceDirectory,
            @NonNull String targetDirectory) throws IOException {
        copyJarDirectory(new File(sourceDirectory), new File(targetDirectory));
    }
    /**
     * Copies jars from a source directory to a target one taking into
     * consideration potential jar duplicates.
     * @param sourceDirectory directory to copy Jars from
     * @param targetDirectory directory to copy Jars to
     * @throws IOException problem copying files
     */
    public void copyJarDirectory(
            @NonNull File sourceDirectory,
            @NonNull File targetDirectory) throws IOException {

        info("Copying jar(s)...");
        info("=================");
        info("Source directory: %s", sourceDirectory.getAbsolutePath());
        info("Target directory: %s", targetDirectory.getAbsolutePath());
        if (!sourceDirectory.isDirectory()) {
            error("Invalid source directory: " + sourceDirectory);
            return;
        }
        if (!targetDirectory.isDirectory()) {
            error("Invalid target directory: " + sourceDirectory);
            return;
        }

        // list all jars in source directory and identify/remove jar duplicates
        // before processing them for copy, one by one.
        List<File> sourceFiles = new ArrayList<>(
                Arrays.asList(sourceDirectory.listFiles(JarFile.FILTER)));
        if (sourceFiles.isEmpty()) {
            error("    No jar files were found in " + sourceDirectory);
            return;
        }
        Map<JarFile, List<DupResult>> resultsBySource = new TreeMap<>();
        JarDuplicateFinder.findJarDuplicates(sourceFiles).forEach(dups -> {
            resultsBySource.put(
                    dups.getGreatest(),
                    new ArrayList<>(
                        dups.getAllButGreatest().stream()
                            .map(jf -> new DupResult(jf, "source ignored"))
                            .toList()));
            sourceFiles.removeAll(new ArrayList<>(
                    dups.getJarFiles().stream()
                    .map(JarFile::toFile)
                    .toList()));
        });
        // add remaining sources with no dups
        sourceFiles.stream().forEach(f -> resultsBySource.put(
                new JarFile(f), new ArrayList<>()));


        info("");
        info("File(s)...");
        info("=================");

        // copy each source jars one by one
        resultsBySource.forEach((jf, results) -> {
            try {
                doCopyJarFile(jf, targetDirectory, results);
            } catch (IOException e) {
                error("Could not copy file: \"%s\". Error: %s",
                        jf.getFullName(),
                        ExceptionUtil.getFormattedMessages(e));
            }
        });
    }

    //--- Copy Jar File --------------------------------------------------------

    /**
     * Copies a single Jar to a target directory, taking into
     * consideration Jar versions.
     * @param sourceJarFile the Jar file to copy
     * @param toDirectory directory to copy the jar into
     * @throws IOException problem copying files
     */
    public void copyJarFile(
            @NonNull String sourceJarFile, @NonNull String toDirectory)
                    throws IOException {
        copyJarFile(new File(sourceJarFile), new File(toDirectory));
    }
    /**
     * Copies a single Jar to a target directory, taking into
     * consideration Jar versions.
     * @param sourceJarFile the Jar file to copy
     * @param targetDirectory directory to copy the jar into
     * @throws IOException problem copying files
     */
    public void copyJarFile(
            @NonNull File sourceJarFile,  File targetDirectory)
                    throws IOException {
        copyJarFile(new JarFile(sourceJarFile), targetDirectory);
    }
    /**
     * Copies a single Jar to a target directory, taking into
     * consideration Jar versions.
     * @param sourceJarFile the Jar file to copy
     * @param targetDirectory directory to copy the jar into
     * @throws IOException problem copying files
     * @since 3.0.0
     */
    public void copyJarFile(
            @NonNull JarFile sourceJarFile, @NonNull File targetDirectory)
                    throws IOException {
        if (!targetDirectory.isDirectory()) {
            error("Invalid target directory: " + targetDirectory);
            return;
        }
        doCopyJarFile(sourceJarFile, targetDirectory, new ArrayList<>());


    }

    private void doCopyJarFile(
            JarFile sourceJar, File targetDir, List<DupResult> dupResults)
                    throws IOException {

        var targetDups = JarDuplicateFinder.findJarDuplicatesOf(
                sourceJar.toFile(), Arrays.asList(targetDir));


        // if there are no duplicates, just copy
        if (targetDups.isEmpty()) {
            copy(sourceJar, targetDir);
            infoFileOutcome(sourceJar, dupResults, OUTCOME_COPIED);
            return;
        }

        var onConflict = ofNullable(
                onJarConflictSupplier.apply(sourceJar, targetDups.get(0)))
                .orElse(OnJarConflict.DEFAULT);
        var sourceAction = onConflict.sourceAction();

        // source action: always copy source upon conflict
        if (sourceAction == OnJarConflict.SourceAction.COPY) {
            performTargetAction(
                    onConflict.targetAction(), targetDups, dupResults);
            copy(sourceJar, targetDir);
            infoFileOutcome(sourceJar, dupResults, OUTCOME_COPIED);
            return;
        }

        // source action: always keep target upon conflict (no copy)
        if (sourceAction == OnJarConflict.SourceAction.NOOP) {
            if (targetDups.size() > 1) {
                performTargetAction(  // target action only on non-greatest
                        onConflict.targetAction(),
                        targetDups.subList(1, targetDups.size()),
                        dupResults);
            }
            targetDups.forEach(jf ->
                    dupResults.add(new DupResult(jf, "target kept")));
            infoFileOutcome(sourceJar, dupResults, OUTCOME_SKIPPED);
            return;
        }

        // source action: copy if greatest, or if greatest or equiv.
        var greatestTarget = targetDups.get(0);
        if (sourceNeedsCopy(sourceAction, sourceJar, greatestTarget)) {
            performTargetAction(
                    onConflict.targetAction(), targetDups, dupResults);
            copy(sourceJar, targetDir);
            infoFileOutcome(sourceJar, dupResults, OUTCOME_COPIED);
            return;
        }
        dupResults.add(new DupResult(greatestTarget, "target kept"));
        if (targetDups.size() > 1) {
            performTargetAction(
                    onConflict.targetAction(),
                    targetDups.subList(1,  targetDups.size()),
                    dupResults);
        }
        infoFileOutcome(sourceJar, dupResults, OUTCOME_SKIPPED);
    }

    private boolean sourceNeedsCopy(
            SourceAction sa, JarFile source, JarFile target) {
        return sa == SourceAction.COPY_IF_GREATER && source.isGreaterThan(target)
                || sa == SourceAction.COPY_IF_GREATER_OR_EQUIVALENT
                        && source.isGreaterOrEquivalentTo(target);
    }

    //--- Target Actions -------------------------------------------------------

    private void performTargetAction(
            TargetAction action,
            List<JarFile> jarFiles,
            List<DupResult> dupResults) {
        switch (action) {
        case RENAME:
            Streams.failableStream(jarFiles) //entry.getConflictingTargets())
                .forEach(jf -> {
                    renameToBackup(jf);
                    dupResults.add(new DupResult(jf, "target renamed"));
                });
            break;
        case DELETE:
            Streams.failableStream(jarFiles) //entry.getConflictingTargets())
                .forEach(jf -> {
                    delete(jf);
                    dupResults.add(new DupResult(jf, "target deleted"));
                });
            break;
        default:
            break;
        }
    }

    private static void copy(JarFile sourceFile, File targetDir) throws IOException {
        FileUtils.copyFileToDirectory(sourceFile.toFile(), targetDir, true);
    }
    private static void delete(JarFile file) throws IOException {
        FileUtil.delete(file.toFile());
    }
    private static void renameToBackup(JarFile file) throws IOException {
        var renamedFile = new File(file.toFile().getAbsolutePath()
                + ".bak-" + createTimestamp());
        FileUtil.moveFile(file.toFile(), renamedFile);
    }
    private static String createTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    //--- Logging --------------------------------------------------------------

    private static void error(String error, Object... args) {
        if (commandLine) {
            System.err.println(String.format(error, args)); //NOSONAR
        } else if (LOG.isErrorEnabled()) {
            LOG.error(String.format(error, args));
        }
    }
    private static void info(String info, Object... args) {
        if (commandLine) {
            System.out.println(String.format(info, args)); //NOSONAR
        } else if (LOG.isInfoEnabled()){
            LOG.info(String.format(info, args));
        }
    }
    private static void infoFileOutcome(
            JarFile srcFile, List<DupResult> dupResults, String outcome) {
        info(rightPad(srcFile.getFullName() + ' ',
                55 - outcome.length(), '.') + ' ' + outcome);
        dupResults.forEach(res -> info(compareSymbol(srcFile, res.dup)
                + res.dup.getFullName() + " " + res.action));
    }
    private static String compareSymbol(JarFile first, JarFile second) {
        var result = first.compareTo(second);
        if (result < 0) {
            return "  < ";
        }
        if (result > 0) {
            return "  > ";
        }
        return "  = ";
    }

    public static void main(String[] args) throws IOException {
        try {
            doMain(args);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage()); //NOSONAR
            }
            System.exit(-1);
        }
    }

    static void doMain(String... args) throws IOException {
        commandLine = true;
        var cmdPrompts = new JarCopierCmdPrompts();
        if (args.length < 1) {
            cmdPrompts.printUsage();
            throw new IllegalArgumentException();
        }

        //--- Resolve source path ---
        var source = new File(args[0]);
        var isSourceDirectory = source.isDirectory();
        if (!isSourceDirectory) {
            validateCommandLineJarPath(source);
        }

        //--- Resolve target ---
        var target = args.length >= 2
                ? new File(args[1])
                : cmdPrompts.promptForTargetDirectory();
        if (!target.exists()) {
            System.out.println( //NOSONAR
                    "The target directory does not exist and will be created.");
            target.mkdirs();
        }

        //--- Source action ---
        SourceAction sourceAction = null;
        if (args.length >= 3) {
            var choice = parseIntChoiceArg("onCondition", args[2], 1, 5);
            if (choice != 5) {
                sourceAction = SourceAction.of(choice);
            }
        } else {
            sourceAction = cmdPrompts.promptForGlobalSourceAction();
        }

        JarCopier jarCopier = null;
        if (sourceAction == null) {
            jarCopier = new JarCopier(cmdPrompts);
        } else {
            //--- Target action ---
            TargetAction targetAction;
            if (args.length >= 4) {
                var choice = parseIntChoiceArg("onOverwrite", args[3], 1, 3);
                targetAction = TargetAction.of(choice);
            } else {
                targetAction = cmdPrompts.promptForGlobalTargetAction();
            }
            jarCopier = new JarCopier(
                    new OnJarConflict(sourceAction, targetAction));
        }

        //--- Perform the copying ---
        if (isSourceDirectory) {
            jarCopier.copyJarDirectory(source, target);
        } else {
            jarCopier.copyJarFile(source, target);
        }
    }

    private static int parseIntChoiceArg(
            String argName, String argValue, int fromIncl, int toIncl) {
        var choice = NumberUtils.toInt(argValue, -1);
        if (choice < fromIncl || choice > toIncl) {
            throw new IllegalArgumentException(String.format(
                    "Invalid \"%s\" argument. Must be a number between"
                    + "%s and %s, inclusively. Was: %s.",
                    argName, fromIncl, toIncl, argValue));
        }
        return choice;
    }

    private static void validateCommandLineJarPath(File path) {
        if (path.isFile() && path.getName().endsWith(".jar")) {
            return;
        }
        throw new IllegalArgumentException(String.format(
                "Path not a valid/existing Jar or directory: %s"
                + "%s and %s, inclusively. Was: %s.",
                path.getAbsolutePath()));
    }

    //--- Inner-Classes --------------------------------------------------------

    /**
     * Encapsulate target jar conflict resolution options.
         * @since 3.0.0
     */
    @Data
    @Getter
    @With
    @Accessors(fluent = true)
    public static final class OnJarConflict {

        public static final OnJarConflict DEFAULT = new OnJarConflict();

        public enum SourceAction {
            COPY_IF_GREATER_OR_EQUIVALENT,
            COPY_IF_GREATER,
            COPY,
            NOOP
            ;
            final int cmdLineOption;
            SourceAction() {
                cmdLineOption = ordinal() + 1;
            }
            static SourceAction of(int option) {
                return Stream.of(values())
                    .filter(sa -> sa.cmdLineOption == option)
                    .findFirst()
                    .orElse(null);
            }
        }
        public enum TargetAction {
            RENAME,
            DELETE,
            NOOP
            ;
            final int cmdLineOption;
            TargetAction() {
                cmdLineOption = ordinal() + 1;
            }
            static TargetAction of(int option) {
                return Stream.of(values())
                    .filter(sa -> sa.cmdLineOption == option)
                    .findFirst()
                    .orElse(null);
            }
        }

        /**
         * Action to perform (or not) on source file.
         * @param sourceAction source action
         * @return {@code this}.
         */
        @SuppressWarnings("javadoc")
        private final SourceAction sourceAction;
        /**
         * Action to perform (or not) on target file.
         * @param targetAction target action
         * @return {@code this}.
         */
        @SuppressWarnings("javadoc")
        private final TargetAction targetAction;

        public OnJarConflict() {
            this(null, null);
        }
        public OnJarConflict(
                SourceAction sourceAction, TargetAction targetAction) {
            this.sourceAction = ofNullable(sourceAction)
                    .orElse(SourceAction.COPY_IF_GREATER_OR_EQUIVALENT);
            this.targetAction = ofNullable(targetAction)
                    .orElse(TargetAction.RENAME);
        }
    }

    @AllArgsConstructor
    private static class DupResult {
        private JarFile dup;
        private String action;
    }

    //--- Deprecated -----------------------------------------------------------

    /**
     * Copy source Jar only if greater or same version as target
     * Jar after renaming target Jar (.bak-[timestamp]).
     * @deprecated Use {@link JarCopier#JarCopier(OnJarConflict)} instead with
     *     {@link SourceAction#COPY_IF_GREATER_OR_EQUIVALENT} and
     *     {@link TargetAction#RENAME}
     */
    @Deprecated(since = "3.0.0")
    public static final int STRATEGY_RENAME_COPY = 1; //NOSONAR
    /**
     * Copy source Jar only if greater or same version as target
     * Jar after deleting target Jar.
     * @deprecated Use {@link JarCopier#JarCopier(OnJarConflict)} instead with
     *     {@link SourceAction#COPY_IF_GREATER_OR_EQUIVALENT} and
     *     {@link TargetAction#DELETE}.
     */
    @Deprecated(since = "3.0.0")
    public static final int STRATEGY_DELETE_COPY = 2; //NOSONAR
    /**
     * Do not copy source Jar (leave target Jar as is).
     * @deprecated Use {@link JarCopier#JarCopier(OnJarConflict)} instead with
     *     {@link SourceAction#NOOP}.
     */
    @Deprecated(since = "3.0.0")
    public static final int STRATEGY_NO_COPY = 3; //NOSONAR
    /**
     * Copy source Jar regardless of target Jar
     * (may overwrite or cause mixed versions).
     * @deprecated Use {@link JarCopier#JarCopier(OnJarConflict)} instead with
     *     {@link SourceAction#COPY}.
     */
    @Deprecated(since = "3.0.0")
    public static final int STRATEGY_PLAIN_COPY = 4; //NOSONAR
    /**
     * Interactive, let the user chose (requires execution on command prompt).
     * @deprecated Use {@link JarCopier#JarCopier(OnJarConflict)} instead.
     */
    @Deprecated(since = "3.0.0")
    public static final int STRATEGY_INTERACTIVE = 5; //NOSONAR

    /**
     * Constructor.
     * @param strategy the strategy to use when encountering
     *                 duplicates/conflicts
     * @deprecated Use {@link JarCopier#JarCopier(OnJarConflict)} instead.
     */
    @Deprecated(since = "3.0.0")
    public JarCopier(int strategy) { //NOSONAR
        this(strategy == STRATEGY_INTERACTIVE
                ? new JarCopierCmdPrompts()
                : (s, t) -> toOnJarConflict(strategy));
    }

    static OnJarConflict toOnJarConflict(int strategy) {
        return switch (strategy) {
        case STRATEGY_RENAME_COPY: //NOSONAR
            yield new OnJarConflict();
        case STRATEGY_DELETE_COPY: //NOSONAR
            yield new OnJarConflict().withTargetAction(TargetAction.DELETE);
        case STRATEGY_NO_COPY: //NOSONAR
            yield new OnJarConflict(SourceAction.NOOP, TargetAction.NOOP);
        case STRATEGY_PLAIN_COPY: //NOSONAR
            yield new OnJarConflict(SourceAction.COPY, TargetAction.NOOP);
        case STRATEGY_INTERACTIVE: //NOSONAR
            yield null;
        default:
            throw new IllegalArgumentException("Invalid strategy: " + strategy);
        };
    }

    /**
     * Gets the strategy used when encountering duplicates or version conflicts.
     * @return <code>-1</code>.
     * @deprecated See {@link JarCopier#JarCopier(OnJarConflict)}.
     */
    @Deprecated(since = "3.0.0")
    public int getStrategy() { //NOSONAR
        return -1;
    }
}
