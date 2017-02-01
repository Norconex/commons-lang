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
package com.norconex.commons.lang.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * A stream gobbler will simply read incoming text from a stream.  This is
 * often required by some processes/operating systems in order to prevent
 * application freeze.  For example, this is a way to capture the STDOUT and
 * STDERR from a process.
 * @author Pascal Essiembre
 */
public class StreamGobbler extends Thread {

    private static final Logger LOG =
            LogManager.getLogger(StreamGobbler.class);
    private final List<IStreamListener> listeners = 
        Collections.synchronizedList(new ArrayList<IStreamListener>());
    /** The input stream we are reading. */
    private final InputStream is;
    private final String type;
    private final String encoding;
    
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
     * @param type an optional way to identify each line by adding a type
     */
    public StreamGobbler(InputStream is, String type) {
        this(is, type, null);
    }
    /**
     * Constructor.
     * @param is input stream
     * @param type an optional way to identify each line by adding a type
     * @param encoding character encoding
     * @since 1.5.0
     */
    public StreamGobbler(InputStream is, String type, String encoding) {
        super("StreamGobbler" + (type == null ? "": "-" + type));
        this.is = is;
        this.type = type;
        this.encoding = encoding;
    }
    
    @Override
    public void run() {
        beforeStreaming();
        try {
            String safeEncoding = encoding;
            if (StringUtils.isBlank(safeEncoding)) {
                safeEncoding = CharEncoding.UTF_8;
            }
            InputStreamReader isr = new InputStreamReader(is, safeEncoding);
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
     * Adds stream listeners.
     * @param l stream listeners
     */
    public synchronized void addStreamListener(IStreamListener... l) {
        this.listeners.addAll(0, Arrays.asList(l));
    }
    /**
     * Removes a stream listener.
     * @param listener stream listener
     */
    public synchronized void removeStreamListener(IStreamListener listener) {
        listeners.remove(listener);
    }
    /**
     * Returns stream listeners.
     * @return the listeners
     * @since 1.5.0
     */
    public IStreamListener[] getStreamListeners() {
        return listeners.toArray(new IStreamListener[] {});
    }

    /**
     * Invoked just before steaming begins, in a new thread.
     * Default implementation does nothing.  This method is for implementors. 
     */
    protected void beforeStreaming() {
        // do nothing (for subclasses)
    }
    /**
     * Invoked just after steaming ended, before the thread dies.
     * Default implementation does nothing.  This method is for implementors. 
     */
    protected void afterStreaming() {
        // do nothing (for subclasses)
    }

    /**
     * Gets the stream type.
     * @return the type
     * @since 1.5.0
     */
    public String getType() {
        return type;
    }
    /**
     * Gets the character encoding.
     * @return character encoding
     * @since 1.5.0
     */
    public String getEncoding() {
        return encoding;
    }
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
