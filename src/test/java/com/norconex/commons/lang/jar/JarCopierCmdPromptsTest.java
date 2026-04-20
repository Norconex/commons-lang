/* Copyright 2026 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.norconex.commons.lang.jar.JarCopier.OnJarConflict;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.SourceAction;
import com.norconex.commons.lang.jar.JarCopier.OnJarConflict.TargetAction;
import com.norconex.commons.lang.mock.Mocker;

class JarCopierCmdPromptsTest {

    @TempDir
    private Path tempDir;

    @Test
    void testPrintUsageAndDefaultActions() throws IOException {
        var output = Mocker.mockStdInOutErr(() -> {
            var prompts = new JarCopierCmdPrompts();

            prompts.printUsage();

            assertThat(prompts.promptForGlobalSourceAction())
                    .isEqualTo(SourceAction.COPY_IF_GREATER_OR_EQUIVALENT);
            assertThat(prompts.promptForGlobalTargetAction())
                    .isEqualTo(TargetAction.RENAME);
        }, "", "");

        assertThat(output).contains("Usage:");
        assertThat(output).contains("How do you want to handle jar conflicts?");
        assertThat(output)
                .contains(
                        "What do you want to do with conflicting target jars");
    }

    @Test
    void testPromptForTargetDirectory() throws IOException {
        var newDirectory = tempDir.resolve("target-dir");

        var output = Mocker.mockStdInOutErr(() -> {
            var prompts = new JarCopierCmdPrompts();
            assertThat(prompts.promptForTargetDirectory())
                    .isEqualTo(newDirectory.toFile());
        }, newDirectory.toString());

        assertThat(output).contains("Please enter a target directory:");
    }

    @Test
    void testPromptForInteractiveGlobalSourceActionReturnsNull()
            throws IOException {
        Mocker.mockStdInOutErr(() -> {
            var prompts = new JarCopierCmdPrompts();
            assertThat(prompts.promptForGlobalSourceAction()).isNull();
        }, "5");
    }

    @ParameterizedTest
    @CsvSource(
        {
                "1,COPY,RENAME",
                "2,COPY,DELETE",
                "3,COPY,NOOP",
                "4,NOOP,NOOP"
        }
    )
    void testApplySingleFileActions(
            int choice,
            SourceAction expectedSourceAction,
            TargetAction expectedTargetAction) throws IOException {
        var conflict = new AtomicReference<OnJarConflict>();

        var output = Mocker.mockStdInOutErr(() -> {
            var prompts = new JarCopierCmdPrompts();
            conflict.set(prompts.apply(
                    createJarFile("source-1.0.0.jar"),
                    createJarFile("target-1.0.0.jar")));
        }, Integer.toString(choice));

        assertThat(conflict.get()).isNotNull();
        assertThat(conflict.get().sourceAction()).isEqualTo(
                expectedSourceAction);
        assertThat(conflict.get().targetAction()).isEqualTo(
                expectedTargetAction);
        assertThat(output).contains("Jar conflict:");
    }

    @Test
    void testApplyRetriesAfterInvalidChoice() throws IOException {
        var conflict = new AtomicReference<OnJarConflict>();

        var output = Mocker.mockStdInOutErr(() -> {
            var prompts = new JarCopierCmdPrompts();
            conflict.set(prompts.apply(
                    createJarFile("retry-source-1.0.0.jar"),
                    createJarFile("retry-target-1.0.0.jar")));
        }, "0", "3");

        assertThat(conflict.get()).isNotNull();
        assertThat(conflict.get().sourceAction()).isEqualTo(SourceAction.COPY);
        assertThat(conflict.get().targetAction()).isEqualTo(TargetAction.NOOP);
        assertThat(output).contains("Wrong selection! Try again.");
    }

    private JarFile createJarFile(String fileName) throws IOException {
        var path = tempDir.resolve(fileName);
        JarTestUtil.createJar(path);
        return new JarFile(path.toFile());
    }
}