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
 * @author Pascal Essiembre (pascal.essiembre&#x40;norconex.com)
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
            //TODO handle better
            throw new RuntimeException("Problem gobbling input stream.", e);
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
