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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.norconex.commons.lang.io.IInputStreamListener;
import com.norconex.commons.lang.io.InputStreamConsumer;

/**
 * Utility methods related to process execution. Checked exceptions
 * are wrapped in a runtime {@link ExecException}.
 * @author Pascal Essiembre
 * @since 1.13.0 (previously part of 
 *        <a href="https://opensource.norconex.com/jef/api/">JEF API</a> 4.0).
 */
public final class ExecUtil {

    /** Identifier for standard output. */
    public static final String STDOUT = "STDOUT";
    /** Identifier for standard error. */
    public static final String STDERR = "STDERR";
    
    /**
     * Constructor.
     */
    private ExecUtil() {
        super();
    }

    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link InputStreamConsumer} to read its error and output stream.
     * @param process the process to watch
     * @return process exit value
     */
    public static int watchProcess(Process process) {
        return watchProcess(process, 
                new IInputStreamListener[] {}, new IInputStreamListener[] {});
    }
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link InputStreamConsumer} to read its error and output stream.
     * The listener will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param listener the listener to use for both "STDERR" or "STDOUT". 
     * @return process exit value
     */
    public static int watchProcess(
            Process process,
            IInputStreamListener listener) {
        return watchProcess(process,
                new IInputStreamListener[] {listener},
                new IInputStreamListener[] {listener});
    }
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link InputStreamConsumer} to read its error and output stream.
     * The listener will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param listeners the listeners to use for both "STDERR" or "STDOUT". 
     * @return process exit value
     */
    public static int watchProcess(
            Process process,
            IInputStreamListener[] listeners) {
        return watchProcess(process, listeners, listeners);
    }
    
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link InputStreamConsumer} to read its error and output stream.
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
            IInputStreamListener outputListener,
            IInputStreamListener errorListener) throws InterruptedException {
        return watchProcess(process,
                new IInputStreamListener[] {outputListener},
                new IInputStreamListener[] {errorListener});
    }
    /**
     * Watches a running process.  This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link InputStreamConsumer} to read its error and output stream.
     * The listeners will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param outputListeners the process output listeners
     * @param errorListeners the process error listeners 
     * @return process exit value
     */
    public static int watchProcess(
            Process process,
            IInputStreamListener[] outputListeners,
            IInputStreamListener[] errorListeners) {
        return watchProcess(process, null, outputListeners, errorListeners);
    }
    /**
     * Watches a running process while sending data to its STDIN. 
     * This method will wait until the process
     * as finished executing before returning with its exit value.
     * It ensures the process does not hang on some platform by making use
     * of the {@link InputStreamConsumer} to read its error and output stream.
     * The listeners will be notified every time an error or output line
     * gets written by the process.
     * The listener line type will either be "STDERR" or "STDOUT".
     * @param process the process to watch
     * @param input input sent to process STDIN
     * @param outputListeners the process output listeners
     * @param errorListeners the process error listeners 
     * @return process exit value
     */
    public static int watchProcess(
            Process process,
            InputStream input,
            IInputStreamListener[] outputListeners,
            IInputStreamListener[] errorListeners) {
        watchProcessAsync(process, input, outputListeners, errorListeners);
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            throw new ExecException("Process was interrupted.", e);
        }
    }
    

    /**
     * Watches process output.  This method is the same as 
     * {@link #watchProcess(
     *            Process, IInputStreamListener, IInputStreamListener)}
     * with the exception of not waiting for the process to complete before
     * returning.
     * @param process the process on which to watch outputs
     * @param outputListener the process output listeners
     * @param errorListener the process error listeners 
     */
    public static void watchProcessAsync(
            Process process,
            IInputStreamListener outputListener,
            IInputStreamListener errorListener) {
        watchProcessAsync(process,
                new IInputStreamListener[] {outputListener},
                new IInputStreamListener[] {errorListener});
    }
    
    
    /**
     * Watches process output.  This method is the same as 
     * {@link #watchProcess(
     *            Process, IInputStreamListener[], IInputStreamListener[])}
     * with the exception of not waiting for the process to complete before
     * returning.
     * @param process the process on which to watch outputs
     * @param outputListeners the process output listeners
     * @param errorListeners the process error listeners 
     */
    public static void watchProcessAsync(
            Process process,
            IInputStreamListener[] outputListeners,
            IInputStreamListener[] errorListeners) {
        watchProcessAsync(process, null, outputListeners, errorListeners);
    }

    /**
     * Watches process output while sending data to its STDIN.  
     * This method is the same as 
     * {@link #watchProcess(Process, InputStream, 
     *            IInputStreamListener[], IInputStreamListener[])}
     * with the exception of not waiting for the process to complete before
     * returning.
     * @param process the process on which to watch outputs
     * @param input input sent to process STDIN
     * @param outputListeners the process output listeners
     * @param errorListeners the process error listeners 
     */
    public static void watchProcessAsync(
            final Process process,
            final InputStream input,
            final IInputStreamListener[] outputListeners,
            final IInputStreamListener[] errorListeners) {
        // listen for output
        InputStreamConsumer output = new InputStreamConsumer(
                process.getInputStream(), STDOUT, outputListeners);
        output.start();

        // listen for error
        InputStreamConsumer error = new InputStreamConsumer(
                process.getErrorStream(), STDERR, errorListeners);
        error.start();

        // send input
        if (input != null) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try (OutputStream stdin = process.getOutputStream()) {
                        IOUtils.copy(input, stdin);
                    } catch (IOException e) {
                        throw new ExecException(
                                "Error sending input stream to proces.", e);
                    }
                }
            };
            t.start();
            try{
               t.join();
            } catch(InterruptedException e) {
                throw new ExecException(
                        "Process interrupted while sending input stream.", e);
            } 
        }
    }
}
