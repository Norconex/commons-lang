/* Copyright 2017-2020 Norconex Inc.
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
package com.norconex.commons.lang.exec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.io.InputStreamLineListener;

public class SystemCommandTest {

    public static final String IN_FILE_PATH = "/exec/sample-input.txt";
    public static final String EXPECTED_OUT_FILE_PATH =
            "/exec/expected-output.txt";

    @TempDir
    static Path tempFolder;

    @Test
    public void testInFileOutFile() throws IOException, SystemCommandException {
        File inFile = inputAsFile();
        File outFile = newTempFile();

        SystemCommand cmd = ExternalApp.newSystemCommand(
                ExternalApp.TYPE_INFILE_OUTFILE, inFile, outFile);
        ExternalAppListener l = addEnvAndListener(cmd);
        cmd.execute();

        Assertions.assertEquals(
                expectedOutputAsString(), fileAsString(outFile));
        Assertions.assertTrue(
                l.capturedThemAll(), "Listener missed some output.");

    }

    @Test
    public void testInFileStdout() throws IOException, SystemCommandException {
        File inFile = inputAsFile();

        SystemCommand cmd = ExternalApp.newSystemCommand(
                ExternalApp.TYPE_INFILE_STDOUT, inFile);
        ExternalAppListener l = addEnvAndListener(cmd);
        cmd.execute();
        Assertions.assertEquals(expectedOutputAsString(), l.getStdoutContent());
        Assertions.assertTrue(
                l.capturedThemAll(), "Listener missed some output.");
    }

    @Test
    public void testStdinOutFile() throws IOException, SystemCommandException {
        InputStream input = inputAsStream();
        File outFile = newTempFile();

        SystemCommand cmd = ExternalApp.newSystemCommand(
                ExternalApp.TYPE_STDIN_OUTFILE, outFile);
        ExternalAppListener l = addEnvAndListener(cmd);
        cmd.execute(input);
        input.close();
        Assertions.assertEquals(expectedOutputAsString(), fileAsString(outFile));
        Assertions.assertTrue(
                l.capturedThemAll(), "Listener missed some output.");
    }

    @Test
    public void testStdinStdout() throws IOException, SystemCommandException {
        InputStream input = inputAsStream();

        SystemCommand cmd = ExternalApp.newSystemCommand(
                ExternalApp.TYPE_STDIN_STDOUT);
        ExternalAppListener l = addEnvAndListener(cmd);
        cmd.execute(input);
        input.close();
        Assertions.assertEquals(expectedOutputAsString(), l.getStdoutContent());
        Assertions.assertTrue(
                l.capturedThemAll(), "Listener missed some output.");
    }

    private File inputAsFile() throws IOException {
        File inFile = newTempFile();
        FileUtils.copyInputStreamToFile(
                getClass().getResourceAsStream(IN_FILE_PATH), inFile);
        return inFile;
    }
    private InputStream inputAsStream() {
        return getClass().getResourceAsStream(IN_FILE_PATH);
    }
    private String fileAsString(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8).trim()
                .replace("\r", "");
    }
    private String expectedOutputAsString() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(
                EXPECTED_OUT_FILE_PATH), StandardCharsets.UTF_8)
                        .replace("\r", "");
    }
    private File newTempFile() throws IOException {
        File file = Files.createTempFile(
                tempFolder, "SystemCommandTest", null).toFile();
        if (!file.exists()) {
            // Just making sure it exists
            FileUtils.touch(file);
        }
        return file;
    }

    private ExternalAppListener addEnvAndListener(SystemCommand cmd) {
        Map<String, String> envs = new HashMap<>();
        envs.put(ExternalApp.ENV_STDOUT_BEFORE, ExternalApp.ENV_STDOUT_BEFORE);
        envs.put(ExternalApp.ENV_STDOUT_AFTER, ExternalApp.ENV_STDOUT_AFTER);
        envs.put(ExternalApp.ENV_STDERR_BEFORE, ExternalApp.ENV_STDERR_BEFORE);
        envs.put(ExternalApp.ENV_STDERR_AFTER, ExternalApp.ENV_STDERR_AFTER);
        cmd.setEnvironmentVariables(envs);

        ExternalAppListener l = new ExternalAppListener();
        cmd.addErrorListener(l);
        cmd.addOutputListener(l);
        return l;
    }

    class ExternalAppListener extends InputStreamLineListener {
        private boolean stdoutBefore = false;
        private boolean stdoutAfter = false;
        private boolean stderrBefore = false;
        private boolean stderrAfter = false;
        private StringBuilder b = new StringBuilder();

        @Override
        public void lineStreamed(String type, String line) {
            if ("STDOUT".equals(type)) {
                if (ExternalApp.ENV_STDOUT_BEFORE.equals(line)) {
                    stdoutBefore = true;
                } else if (ExternalApp.ENV_STDOUT_AFTER.equals(line)) {
                    stdoutAfter = true;
                } else {
                    if (b.length() > 0) {
                        b.append('\n');
                    }
                    b.append(line);
                }
            } else if ("STDERR".equals(type)) {
                if (ExternalApp.ENV_STDERR_BEFORE.equals(line)) {
                    stderrBefore = true;
                } else if (ExternalApp.ENV_STDERR_AFTER.equals(line)) {
                    stderrAfter = true;
                }
            }
        }
        public boolean capturedThemAll() {
            return stdoutBefore && stdoutAfter && stderrBefore && stderrAfter;
        }
        public String getStdoutContent() {
            return b.toString();
        }
    }

}
