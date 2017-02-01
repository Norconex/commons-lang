/* Copyright 2017 Norconex Inc.
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 * Sample external app that reverse word order in lines.
 * Also prints specific environment variables to STDOUT or STDERR.
 * @author Pascal Essiembre
 */
public class ExternalApp {

    public static final String TYPE_INFILE_OUTFILE = "infile-outfile";
    public static final String TYPE_INFILE_STDOUT = "infile-stdout";
    public static final String TYPE_STDIN_OUTFILE = "stdin-outfile";
    public static final String TYPE_STDIN_STDOUT = "stdin-stdout";

    public static final String ENV_STDOUT_BEFORE = "stdout_before";
    public static final String ENV_STDOUT_AFTER = "stdout_after";
    public static final String ENV_STDERR_BEFORE = "stderr_before";
    public static final String ENV_STDERR_AFTER = "stderr_after";
    
    // reverse the word order in each lines
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println(
                    "Expected arguments: <testType> [infile] [outfile]");
            System.err.println("Where: <testType> is one of:");
            System.err.println("    " + TYPE_INFILE_OUTFILE);
            System.err.println("    " + TYPE_INFILE_STDOUT);
            System.err.println("    " + TYPE_STDIN_OUTFILE);
            System.err.println("    " + TYPE_STDIN_STDOUT);
            System.exit(-1);
        }
        
        String type = args[0];
        int fileArgIndex = 1;
        File inFile = null;
        File outFile = null;
        if (type.contains("infile")) {
            inFile = new File(args[fileArgIndex]);
            fileArgIndex++;
        }
        if (type.contains("outfile")) {
            outFile = new File(args[fileArgIndex]);
        }

        printEnvToStdout(ENV_STDOUT_BEFORE);
        printEnvToStderr(ENV_STDERR_BEFORE);
        OutputStream output = getOutputStream(outFile);
        try (InputStream input = getInputStream(inFile)) {
            List<String> lines = 
                   IOUtils.readLines(input, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] words =  line.split(" ");
                ArrayUtils.reverse(words);
                output.write(StringUtils.join(words, " ").getBytes());
                output.write('\n');
                output.flush();
            }
        }

        printEnvToStdout(ENV_STDOUT_AFTER);
        printEnvToStderr(ENV_STDERR_AFTER);
        if (output != System.out) {
            output.close();
        }
    }
    
    
    private static void printEnvToStdout(String varName) {
        String var = System.getenv(varName);
        if (StringUtils.isNotBlank(var)) {
            System.out.println(var);
        }
    }
    private static void printEnvToStderr(String varName) {
        String var = System.getenv(varName);
        if (StringUtils.isNotBlank(var)) {
            System.err.println(var);
        }
    }
    
    private static InputStream getInputStream(File inFile) 
            throws FileNotFoundException {
        if (inFile != null) {
            return new FileInputStream(inFile);
        }
        return System.in;
    }
    private static OutputStream getOutputStream(File outFile) 
            throws FileNotFoundException {
        if (outFile != null) {
            return new FileOutputStream(outFile);
        }
        return System.out;
    }

    public static SystemCommand newSystemCommand(String type, File... files) {
        return new SystemCommand(newCommandLine(type, files));
    }

    public static String newCommandLine(String type, File... files) {
        Project project = new Project();
        project.init();
        try {
            Java javaTask = new Java();
            javaTask.setTaskName("runjava");
            javaTask.setProject(project);
            javaTask.setFork(true);
            javaTask.setFailonerror(true);
            javaTask.setClassname(ExternalApp.class.getName());
            javaTask.setClasspath(
                    new Path(project, SystemUtils.JAVA_CLASS_PATH));
            String args = type;
            if (files != null) {
                for (File file : files) {
                    args += " \"" + file.getAbsolutePath() + "\"";
                }
            }
            javaTask.getCommandLine().createArgument().setLine(args);

            String cmd = StringUtils.join(javaTask.getCommandLine(), " ");
            return cmd;
        } catch (BuildException e) {
            throw e;
        }
    }    
    
}
