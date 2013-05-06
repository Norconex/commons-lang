/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * A stream gobbler will simply read incoming text from a stream.  This is
 * often required by some processes/operating systems in order to prevent
 * application freeze.  For example, this is a way to capture the STDOUT and
 * STDERR from a process.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
@SuppressWarnings("nls")
public class StreamGobbler extends Thread {

    private static final Logger LOG =
            LogManager.getLogger(StreamGobbler.class);
    private final List<IStreamListener> listeners = 
        Collections.synchronizedList(new ArrayList<IStreamListener>());
    /** The input stream we are reading. */
    private final InputStream is;
    private final String type;

    /**
     * Constructor.
     * @param is input stream
     */
    public StreamGobbler(InputStream is) {
        this(is, null);
    }
    /**
     * Constructor.
     * @param is input stream
     */
    public StreamGobbler(InputStream is, String type) {
        super("StreamGobbler" + (type == null ? "": "-" + type));
        this.is = is;
        this.type = type;
    }
    
    @Override
    public void run() {
        beforeStreaming();
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                fireLineStreamed(line);
            }
        } catch (IOException e) {
            throw new StreamException("Problem gobbling input stream.", e);
        }
        afterStreaming();
    }
    /**
     * Adds a stream listener.
     * @param listener stream listener
     */
    public synchronized void addStreamListener(IStreamListener listener) {
        listeners.add(0, listener);
    }
    /**
     * Adds stream listeners.
     * @param listeners stream listeners
     */
    public synchronized void addStreamListeners(IStreamListener[] listeners) {
        this.listeners.addAll(0, Arrays.asList(listeners));
    }
    /**
     * Removes a stream listener.
     * @param listener stream listener
     */
    public synchronized void removeStreamListener(IStreamListener listener) {
        listeners.remove(listener);
    }

    /**
     * Invoked just before steaming begins, in a new thread.
     * Default implementation does nothing.  This method is for implementors. 
     */
    protected void beforeStreaming() {};
    /**
     * Invoked just after steaming ended, before the thread dies.
     * Default implementation does nothing.  This method is for implementors. 
     */
    protected void afterStreaming() {};

    private synchronized void fireLineStreamed(String line) {
        if (LOG.isDebugEnabled()) {
            if (type != null) {
                LOG.debug(type + ":" + line);
            } else {
                LOG.debug(line);
            }
        }
        for (IStreamListener listener : listeners) {
            listener.lineStreamed(type, line);
        }
    }
}
