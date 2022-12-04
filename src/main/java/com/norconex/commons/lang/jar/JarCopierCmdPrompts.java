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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.norconex.commons.lang.jar.JarCopier.OnJarConflict;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.SourceAction;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.TargetAction;

/**
 * On jar conflict command-line supplier with interactive prompts.
 * @since 3.0.0
 */
class JarCopierCmdPrompts
        implements BiFunction<JarFile, JarFile, OnJarConflict> {

    private final PrintStream out;
    private final PrintStream err;
    private final Scanner scanner;

    private final Properties prompts = new Properties();

    JarCopierCmdPrompts() {
        out = System.out; //NOSONAR
        err = System.err; //NOSONAR
        scanner = new Scanner(System.in);
        initLoadPrompts();
    }

    @Override
    public OnJarConflict apply(JarFile source, JarFile target) {
        return promptForSingleFileAction(source, target);
    }

    void printUsage() {
        err.println(prompts.get("usage"));
    }

    File promptForTargetDirectory() {
        out.println("Please enter a target directory:");
        File file = new File(scanner.nextLine());
        if (file.exists() && !file.isDirectory()) {
            err.println("Path already exists and is not a directory: " + file);
            System.exit(-1);
        }
        return file;
    }

    SourceAction promptForGlobalSourceAction() {
        int choice = promptForIntChoice("globalGourceAction", 1, 5);
        if (choice == 5) {
            return null;
        }
        return SourceAction.of(choice);
    }

    TargetAction promptForGlobalTargetAction() {
        return TargetAction.of(promptForIntChoice("globalTargetAction", 1, 3));
    }

    OnJarConflict promptForSingleFileAction(JarFile source, JarFile target) {
        out.println();
        out.println("Jar conflict:");
        out.println();
        out.println("  Source: " + source.getFullName()
                + " (last modified: " + source.getLastModified() + ")");
        out.println("  Target: " + target.getFullName()
                + " (last modified: " + target.getLastModified() + ")");

        int choice = promptForIntChoice("singleFileActions", 1, 4);
        switch (choice) {
        case 1:
            return new OnJarConflict(SourceAction.COPY, TargetAction.RENAME);
        case 2:
            return new OnJarConflict(SourceAction.COPY, TargetAction.DELETE);
        case 3:
            return new OnJarConflict(SourceAction.COPY, TargetAction.NOOP);
        default: // 4
            return new OnJarConflict(SourceAction.NOOP, TargetAction.NOOP);
        }
    }

    // guaranteed to be a number in range
    private int promptForIntChoice(
            String promptKey, int fromIncl, int toIncl) {
        out.println(prompts.getProperty(promptKey));
        while (true) {
            out.println("");
            out.print("Your choice (default = 1): ");
            String choiceStr = scanner.nextLine();
            out.println("");
            if (StringUtils.isEmpty(choiceStr)) {
                return 1;
            }
            int choice = NumberUtils.toInt(choiceStr, -1);
            if ((choice >= fromIncl) && (choice <= toIncl)) {
                return choice;
            }
            out.println("");
            out.println("Wrong selection! Try again.");
        }
    }

    private void initLoadPrompts() {
        try {
            Matcher m = Pattern.compile("(?smi)^<([a-z]+)>(.*)^</\\1>").matcher(
                    IOUtils.toString(getClass().getResourceAsStream(
                            "JarCopier.prompts"), UTF_8));
            while (m.find()) {
                prompts.setProperty(m.group(1), m.group(2));
            }
        } catch (IOException e) {
            // should never happen
            throw new UncheckedIOException(e);
        }
    }
}
