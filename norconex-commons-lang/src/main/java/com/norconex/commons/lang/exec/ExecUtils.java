/* Copyright 2010-2017 Norconex Inc.
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

import com.norconex.commons.lang.io.IStreamListener;
import com.norconex.commons.lang.io.StreamGobbler;

/**
 * Utility methods related to process execution.
 * @author Pascal Essiembre
 * @since 1.13.0 (previously part of 
 *        <a href="https://www.norconex.com/jef/api/">JEF API</a> 4.0).
 */
public final class ExecUtils {

    /** Identifier for standard output. */
    public static final String STDOUT = "STDOUT";
    /** Identifier for standard error. */
    public static final String STDERR = "STDERR";
    
    /**
     * Constructor.
     */
    private ExecUtils() {
        super();
    }

    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link StreamGobbler} to read its error and output stream.
     * @param process the process to watch
     * @return process exit value
     * @throws InterruptedException problem while waiting for process to finish
     */
    public static int watchProcess(Process process)
            throws InterruptedException {
        return watchProcess(
                process, new IStreamListener[] {}, new IStreamListener[] {});
    }
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link StreamGobbler} to read its error and output stream.
     * The listener will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param listener the listener to use for both "STDERR" or "STDOUT". 
     * @return process exit value
     * @throws InterruptedException problem while waiting for process to finish
     */
    public static int watchProcess(
            Process process,
            IStreamListener listener) throws InterruptedException {
        return watchProcess(process,
                new IStreamListener[] {listener},
                new IStreamListener[] {listener});
    }
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link StreamGobbler} to read its error and output stream.
     * The listener will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param listeners the listeners to use for both "STDERR" or "STDOUT". 
     * @return process exit value
     * @throws InterruptedException problem while waiting for process to finish
     */
    public static int watchProcess(
            Process process,
            IStreamListener[] listeners) throws InterruptedException {
        return watchProcess(process, listeners, listeners);
    }
    
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link StreamGobbler} to read its error and output stream.
     * The listener will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param outputListener the process output listener 
     * @param errorListener the process error listener 
     * @return process exit value
     * @throws InterruptedException problem while waiting for process to finish
     */
    public static int watchProcess(
            Process process,
            IStreamListener outputListener,
            IStreamListener errorListener) throws InterruptedException {
        return watchProcess(process,
                new IStreamListener[] {outputListener},
                new IStreamListener[] {errorListener});
    }
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link StreamGobbler} to read its error and output stream.
     * The listeners will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param outputListeners the process output listeners
     * @param errorListeners the process error listeners 
     * @return process exit value
     * @throws InterruptedException problem while waiting for process to finish
     */
    public static int watchProcess(
            Process process,
            IStreamListener[] outputListeners,
            IStreamListener[] errorListeners) throws InterruptedException {
        watchProcessOutput(process, outputListeners, errorListeners);
        return process.waitFor();
    }
    

    /**
     * Watches process output.  This method is the same as 
     * {@link #watchProcess(Process, IStreamListener, IStreamListener)}
     * with the exception of not waiting for the process to complete before
     * returning.
     * @param process the process on which to watch outputs
     * @param outputListener the process output listeners
     * @param errorListener the process error listeners 
     */
    public static void watchProcessOutput(
            Process process,
            IStreamListener outputListener,
            IStreamListener errorListener) {
        watchProcessOutput(process,
                new IStreamListener[] {outputListener},
                new IStreamListener[] {errorListener});
    }
    
    
    /**
     * Watches process output.  This method is the same as 
     * {@link #watchProcess(Process, IStreamListener[], IStreamListener[])}
     * with the exception of not waiting for the process to complete before
     * returning.
     * @param process the process on which to watch outputs
     * @param outputListeners the process output listeners
     * @param errorListeners the process error listeners 
     */
    public static void watchProcessOutput(
            Process process,
            IStreamListener[] outputListeners,
            IStreamListener[] errorListeners) {
        // listen for output
        StreamGobbler output = 
                new StreamGobbler(process.getInputStream(), STDOUT);
        output.addStreamListener(outputListeners);
        output.start();

        // listen for error
        StreamGobbler error = 
                new StreamGobbler(process.getErrorStream(), STDERR);
        error.addStreamListener(errorListeners);
        error.start();
    }
    
}
