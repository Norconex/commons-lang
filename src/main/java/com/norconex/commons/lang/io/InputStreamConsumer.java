/* Copyright 2017-2022 Norconex Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

/**
 * A stream consumer will read all it can from a stream in its own thread.
 * This is often required by some processes/operating systems in order to
 * prevent application freeze.  For example, this is a way to capture the
 * STDOUT and STDERR from a process.  This class also allows to "listen" to
 * what is being read. Listeners should not be considered thread-safe.
 * If you share a listener between threads, make sure to
 * have unique types to identify each one or the content being streamed
 * from different threads can easily be mixed up in the order sent and is
 * likely not a desired behavior.
 * @author Pascal Essiembre
 * @since 1.13.0
 */
public class InputStreamConsumer extends Thread {

    public static final int DEFAULT_CHUNK_SIZE = 1024;
    private final List<IInputStreamListener> listeners =
            Collections.synchronizedList(new ArrayList<IInputStreamListener>());
    /** The input stream we are reading. */
    private final InputStream input;
    private final String type;
    private final int chunkSize;

    /**
     * Constructor.
     * @param input input stream
     */
    public InputStreamConsumer(InputStream input) {
        this(input, null);
    }
    /**
     * Constructor.
     * @param input input stream
     * @param type an optional way to identify each portion being read to
     *        stream listeners. Useful when listeners are shared.
     *        Can be <code>null</code>.
     * @param listeners input stream listeners
     */
    public InputStreamConsumer(
            InputStream input, String type,
            IInputStreamListener... listeners) {
        this(input, DEFAULT_CHUNK_SIZE, type, listeners);
    }
    /**
     * Constructor.
     * @param input input stream
     * @param chunkSize how many bytes to read at once before (will also
     *        be the maximum byte array size sent to listeners)
     * @param type an optional way to identify each portion being read to
     *        stream listeners. Useful when listeners are shared.
     *        Can be <code>null</code>.
     * @param listeners input stream listeners
     */
    public InputStreamConsumer(
            InputStream input, int chunkSize, String type,
            IInputStreamListener... listeners) {
        super("StreamConsumer" + (type == null ? "": "-" + type));
        this.input = input;
        this.type = type;
        this.chunkSize = chunkSize;
        if (!ArrayUtils.isEmpty(listeners)) {
            this.listeners.addAll(0, Arrays.asList(listeners));
        }
    }

    @Override
    public void run() {
        beforeStreaming();
        try {
            byte[] buffer = new byte[chunkSize];
            int length;
            while ((length = input.read(buffer)) != -1) {
                fireStreamed(buffer, length);
            }
            fireStreamed(ArrayUtils.EMPTY_BYTE_ARRAY, -1);
        } catch (IOException e) {
            throw new StreamException("Problem consuming input stream.", e);
        }
        afterStreaming();
    }
    /**
     * Returns stream listeners.
     * @return the listeners
     */
    public IInputStreamListener[] getStreamListeners() {
        return listeners.toArray(new IInputStreamListener[] {});
    }
    /**
     * Gets the stream type or <code>null</code> if no type was set.
     * @return the type
     */
    public String getType() {
        return type;
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
     * Starts this consumer thread and wait for it to complete before returning.
     * @throws StreamException if streaming is interrupted while waiting
     */
    public synchronized void startAndWait() {
        start();
        try {
            join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StreamException("Streaming interrupted.", e);
        }
    }

    private synchronized void fireStreamed(byte[] bytes, int length) {
        for (IInputStreamListener listener : listeners) {
            listener.streamed(type, bytes, length);
        }
    }

    /**
     * Convenience method for creating a consumer instance and starting it.
     * @param input input stream to consume.
     */
    public static void consume(InputStream input) {
        consume(input, null);
    }
    /**
     * Convenience method for creating a consumer instance and starting it.
     * @param input input stream
     * @param type an optional way to identify each portion being read to
     *        stream listeners. Useful when listeners are shared.
     *        Can be <code>null</code>.
     * @param listeners input stream listeners
     */
    public static void consume(InputStream input, String type,
            IInputStreamListener... listeners) {
        consume(input, DEFAULT_CHUNK_SIZE, type, listeners);
    }
    /**
     * Convenience method for creasing a consumer instance, starting it,
     * and waiting for it to complete.
     * @param input input stream
     * @param chunkSize how many bytes to read at once before (will also
     *        be the maximum byte array size sent to listeners)
     * @param type an optional way to identify each portion being read to
     *        stream listeners. Useful when listeners are shared.
     *        Can be <code>null</code>.
     * @param listeners input stream listeners
     */
    public static void consume(InputStream input, int chunkSize, String type,
            IInputStreamListener... listeners) {
        new InputStreamConsumer(input, chunkSize, type, listeners).start();
    }
    /**
     * Convenience method for creasing a consumer instance, starting it,
     * and waiting for it to complete.
     * @param input input stream to consume.
     */
    public static void consumeAndWait(InputStream input) {
        consumeAndWait(input, null);
    }
    /**
     * Convenience method for creasing a consumer instance and starting it.
     * @param input input stream
     * @param type an optional way to identify each portion being read to
     *        stream listeners. Useful when listeners are shared.
     *        Can be <code>null</code>.
     * @param listeners input stream listeners
     */
    public static void consumeAndWait(InputStream input, String type,
            IInputStreamListener... listeners) {
        consumeAndWait(input, DEFAULT_CHUNK_SIZE, type, listeners);
    }
    /**
     * Convenience method for creasing a consumer instance, starting it,
     * and waiting for it to complete.
     * @param input input stream
     * @param chunkSize how many bytes to read at once before (will also
     *        be the maximum byte array size sent to listeners)
     * @param type an optional way to identify each portion being read to
     *        stream listeners. Useful when listeners are shared.
     *        Can be <code>null</code>.
     * @param listeners input stream listeners
     */
    public static void consumeAndWait(
            InputStream input, int chunkSize, String type,
            IInputStreamListener... listeners) {
        new InputStreamConsumer(
                input, chunkSize, type, listeners).startAndWait();
    }
}
