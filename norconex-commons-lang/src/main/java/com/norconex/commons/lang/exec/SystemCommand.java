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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.io.IStreamListener;

/**
 * Represents a program to be executed by the underlying system
 * (on the "command line").  This class attempts to be system-independent,
 * which means given an executable path should be sufficient to run
 * programs on any systems (e.g. it handles prefixing an executable with OS
 * specific commands as well as preventing process hanging on some OS when
 * there is nowhere to display the output).  
 * 
 * @author Pascal Essiembre
 * @since 1.13.0 (previously part of 
 *        <a href="https://www.norconex.com/jef/api/">JEF API</a> 4.0).
 */
public class SystemCommand {

    private static final Logger LOG = LogManager.getLogger(SystemCommand.class);

    private static final String[] EMPTY_STRINGS = new String[] {};
    private static final String[] CMD_PREFIXES_WIN_LEGACY = 
    		new String[] { "command.com", "/C" };
    private static final String[] CMD_PREFIXES_WIN_CURRENT = 
			new String[] { "cmd.exe", "/C" };
    private static final IStreamListener[] EMPTY_LISTENERS =
    		new IStreamListener[] {};
    
    private final String[] command;
    private final File workdir;

    private final List<IStreamListener> errorListeners =
            Collections.synchronizedList(new ArrayList<IStreamListener>());
    private final List<IStreamListener> outputListeners =
            Collections.synchronizedList(new ArrayList<IStreamListener>());

    private Process process;
    
    /**
     * Creates a command for which the execution will be in the working
     * directory of the current process.  If more than one command values
     * are passed, the first element of the array
     * is the command and subsequent elements are arguments.
     * @param command the command to run
     */
    public SystemCommand(String... command) {
    	this(null, command);
    }
    
    /**
     * Creates a command. If more than one command values
     * are passed, the first element of the array
     * is the command and subsequent elements are arguments.
     * @param command the command to run
     * @param workdir command working directory.
     */
    public SystemCommand(File workdir, String... command) {
        super();
        this.command = command;
        this.workdir = workdir;
    }
    
    
    /**
     * Gets the command to be run.
     * @return the command
     */
    public String[] getCommand() {
        return ArrayUtils.clone(command);
    }

    /**
     * Gets the command working directory.
     * @return command working directory.
     */
    public File getWorkdir() {
    	return workdir;
    }

    /**
     * Adds an error (STDERR) listener to this system command.
     * @param listener command error listener
     */
    public void addErrorListener(
            final IStreamListener listener) {
        synchronized (errorListeners) {
        	errorListeners.add(0, listener);
        }
    }
    /**
     * Removes an error (STDERR) listener.
     * @param listener command error listener
     */
    public void removeErrorListener(
            final IStreamListener listener) {
        synchronized (errorListeners) {
        	errorListeners.remove(listener);
        }
    }
    /**
     * Adds an output (STDOUT) listener to this system command.
     * @param listener command output listener
     */
    public void addOutputListener(
            final IStreamListener listener) {
        synchronized (outputListeners) {
        	outputListeners.add(0, listener);
        }
    }
    /**
     * Removes an output (STDOUT) listener.
     * @param listener command output listener
     */
    public void removeOutputListener(
            final IStreamListener listener) {
        synchronized (outputListeners) {
        	outputListeners.remove(listener);
        }
    }

    /**
     * Returns whether the command is currently running.
     * @return <code>true</code> if running
     */
    public boolean isRunning() {
    	if (process == null) {
    		return false;
    	}
    	try {
        	process.exitValue();
        	return false;
    	} catch (IllegalThreadStateException e) {
    		return true;
    	}
    }

    /**
     * Aborts the running command.  If the command is not currently running,
     * aborting it will have no effect.
     */
    public void abort() {
        if (process != null) {
            process.destroy();
        }
    }

    /**
     * Executes the given command and returns only when the underlying process
     * stopped running.  
     * @return process exit value
     * @throws SystemCommandException problem executing command
     */
    public int execute() throws SystemCommandException {
        return execute(false);
    }
    
    /**
     * Executes the given system command.  When run in the background,
     * this method does not wait for the process to complete before returning.
     * In such case the status code should always be 0 unless it terminated 
     * abruptly (may not reflect the process termination status).
     * When NOT run in the background, this method waits and returns 
     * only when the underlying process stopped running.  
     * Alternatively, to run a command asynchronously, you can wrap it in 
     * its own thread.
     * @param runInBackground <code>true</code> to runs the system command in 
     *         background.
     * @return process exit value
     * @throws SystemCommandException problem executing command
     * @throws IllegalStateException when command is already running
     */
    public int execute(boolean runInBackground) throws SystemCommandException {
        if (isRunning()) {
            throw new IllegalStateException(
                    "Command is already running: " + toString());
        }
        String[] cleanCommand = getCleanCommand();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + toString());
        }
        try {
            process = Runtime.getRuntime().exec(cleanCommand, null, workdir);
        } catch (IOException e) {
            throw new SystemCommandException("Could not execute command: "
                    + toString(), e);
        }
        int exitValue = 0;
        if (runInBackground) {
            ExecUtils.watchProcessOutput(
                    process, 
                    outputListeners.toArray(EMPTY_LISTENERS),
                    errorListeners.toArray(EMPTY_LISTENERS));
            try {
                // Check in case the process terminated abruptly.
                exitValue = process.exitValue();
            } catch (IllegalThreadStateException e) {
                // Do nothing
            }
        } else {
            try {
                exitValue = ExecUtils.watchProcess(
                        process, 
                        outputListeners.toArray(EMPTY_LISTENERS),
                        errorListeners.toArray(EMPTY_LISTENERS));
            } catch (InterruptedException e) {
                throw new SystemCommandException(
                        "Could not watch process for command: "
                                + toString(), e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Command returned with exit value " + exitValue
            		+ ": " + toString());
        }
        if (exitValue != 0) {
            LOG.error("Command returned with exit value " + process.exitValue()
                    + ": " + toString());
        }
        process = null;
        return exitValue;
    }

    /**
     * Returns the command to be executed.
     */
    @Override
    public String toString() {
        return StringUtils.join(command, " ");
    }

    private String[] getCleanCommand() throws SystemCommandException {
        if (ArrayUtils.isEmpty(command)) {
            throw new SystemCommandException("No command specified.");
        }
        
        String[] prefixes = getOSCommandPrefixes();
        if (ArrayUtils.isEmpty(prefixes)) {
            return command;
        }

        // if command starts with same prefix, do not add it.
        if (command[0].equalsIgnoreCase(prefixes[0])) {
            return command;
        }
        
        return ArrayUtils.addAll(prefixes, command);
    }
    
    private String[] getOSCommandPrefixes() {
    	if (SystemUtils.OS_NAME == null) {
    		return EMPTY_STRINGS;
    	}
    	if (SystemUtils.IS_OS_WINDOWS) {
    		if (SystemUtils.IS_OS_WINDOWS_95
    				|| SystemUtils.IS_OS_WINDOWS_98
    				|| SystemUtils.IS_OS_WINDOWS_ME) {
    			return CMD_PREFIXES_WIN_LEGACY;
    		}
            // NT, 2000, XP and up
			return CMD_PREFIXES_WIN_CURRENT;
    	}
    	return EMPTY_STRINGS;
    }
}