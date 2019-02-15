/* Copyright 2015-2019 Norconex Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.lang3.ArrayUtils;

/**
 * <p>This class is an alternate version of
 * Java {@link java.io.ByteArrayOutputStream}. This code is derived from the
 * Apache {@link org.apache.commons.io.output.ByteArrayOutputStream}.
 * Like the Apache version, this class creates new byte arrays as it grows
 * without copying them into a larger one (for better performance).
 * Each new array is stored in a list.</p>
 *
 * <p>In addition to the Apache version, this class offers methods to access
 * subsets of bytes ranging form zero to the total number of bytes written
 * so far. Also different, each byte array created
 * have the same length, matching the initial capacity provided
 * (default is 1024).</p>
 *
 * <p>The higher the initial capacity, the faster it should be to write
 * large streams, but the more initial memory it will take.</p>
 *
 * @author Pascal Essiembre
 * @since 2.1.0
 */
public class ByteArrayOutputStream extends OutputStream {

    public static final int DEFAULT_INITIAL_CAPACITY = 1024;

    /** The list of buffers, which grows and never reduces. */
    private final List<byte[]> buffers = new ArrayList<>();

    /** The index of the current buffer. */
    private int currentBufferIndex;

    /** Total count of bytes written in all buffers. */
    private int totalCount;

    /** The current buffer. */
    private byte[] currentBuffer;

    /** Each new buffer initialization length. */
    private final int bufferCapacity;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 1024 bytes.
     */
    public ByteArrayOutputStream() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size  the initial size
     * @throws IllegalArgumentException if size is negative
     */
    public ByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException(
                "Negative initial size: " + size);
        }
        synchronized (this) {
            this.bufferCapacity = size;
            addNewBuffer();
        }

    }

    private void addNewBuffer() {
        currentBuffer = new byte[bufferCapacity];
        buffers.add(currentBuffer);
        currentBufferIndex = 0;
    }

    /**
     * Gets the single byte value found at specified offset.  If the offset is
     * larger than the byte array length, <code>-1</code> is
     * returned. If the offset is lower than zero, zero is assumed.
     * @param offset position to read the byte at.
     * @return a decimal byte value
     */
    public int getByte(int offset) {
        int pos = Math.max(0, offset);
        if (pos >= totalCount) {
            return -1;
        }
        int buffersIndex = pos / bufferCapacity;
        int bufPos = pos % bufferCapacity;
        return buffers.get(buffersIndex)[bufPos];
    }

    /**
     * Gets a byte array matching the specified offset and target
     * byte array length.
     * If the offset is larger than the byte array length,
     * <code>-1</code> will be returned.
     * If the offset is lower than zero, zero is assumed.
     * If the target byte array length is larger than the byte array,
     * the value returned will be lower than the specified length.
     * @param target target byte array to store bytes into.
     * @param offset position to read the byte at.
     * @return the number of bytes read
     */
    public int getBytes(byte[] target, int offset) {
        if (target == null) {
            throw new IllegalArgumentException(
                    "Target byte array cannot be null.");
        }

        //TODO no need to synchronize since read-only and no cursor?
        int thisStartOffset = Math.max(0, offset);
        if (thisStartOffset >= totalCount) {
            return -1;
        }
        int thisLengthToRead =
                Math.min(target.length, totalCount - thisStartOffset);

        int sourceBytesLeftToRead = thisLengthToRead;
        int sourceOffset = thisStartOffset;
        int targetOffset = 0;
        while (sourceBytesLeftToRead > 0) {
            byte[] sliceBuffer = buffers.get(sourceOffset / bufferCapacity);
            int sliceOffset = sourceOffset % bufferCapacity;
            int lengthToRead;
            if (sourceBytesLeftToRead > bufferCapacity - sliceOffset) {
                lengthToRead = bufferCapacity - sliceOffset;
            } else {
                lengthToRead = sourceBytesLeftToRead;
            }
            System.arraycopy(sliceBuffer,
                    sliceOffset, target, targetOffset, lengthToRead);
            sourceBytesLeftToRead -= lengthToRead;
            sourceOffset += lengthToRead;
            targetOffset += lengthToRead;
        }
        return thisLengthToRead;
    }

    /**
     * Write the bytes to byte array.
     * @param b the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     */
    @Override
    public void write(byte[] b, int off, int len) {
        if ((off < 0)
                || (off > b.length)
                || (len < 0)
                || ((off + len) > b.length)
                || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        synchronized (this) {
            int bytesLeftToWrite = len;
            int lastOff = off;
            while (bytesLeftToWrite > 0) {
                int currentRoomLeft = bufferCapacity - currentBufferIndex;
                int lengthToWrite = Math.min(bytesLeftToWrite, currentRoomLeft);
                System.arraycopy(b, lastOff, currentBuffer,
                        currentBufferIndex, lengthToWrite);
                currentBufferIndex += lengthToWrite;
                lastOff += lengthToWrite;
                bytesLeftToWrite -= lengthToWrite;
                if (currentBufferIndex == bufferCapacity) {
                    addNewBuffer();
                }
                totalCount += lengthToWrite;
            }
        }
    }

    /**
     * Write a byte to byte array.
     * @param b the byte to write
     */
    @Override
    public synchronized void write(int b) {
        currentBuffer[currentBufferIndex] = (byte) b;
        totalCount++;
        currentBufferIndex++;
        if (currentBufferIndex == bufferCapacity) {
            addNewBuffer();
        }
    }

    /**
     * Writes the entire contents of the specified input stream to this
     * byte stream. Bytes from the input stream are read directly into the
     * internal buffers of this streams.
     *
     * @param in the input stream to read from
     * @return total number of bytes read from the input stream
     *         (and written to this stream)
     * @throws IOException if an I/O error occurs while reading the input stream
     */
    public synchronized int write(InputStream in) throws IOException {
        byte[] buffer = new byte[DEFAULT_INITIAL_CAPACITY];
        int readCount = 0;
        int length;
        while ((length = in.read(buffer)) != -1) {
            readCount += length;
            write(buffer, 0, length);
        }
        return readCount;
    }

    /**
     * Return the current size of the byte array.
     * @return the current size of the byte array
     */
    public synchronized int size() {
        return totalCount;
    }

    /**
     * Closing a <tt>ByteArrayOutputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     *
     * @throws IOException never (this method should not declare this exception
     * but it has to now due to backwards compatability)
     */
    @Override
    public void close() throws IOException {
        //nop
    }

    /**
     * @see java.io.ByteArrayOutputStream#reset()
     */
    public synchronized void reset() {
        totalCount = 0;
        buffers.clear();
        addNewBuffer();
    }

    /**
     * Writes the entire contents of this byte stream to the
     * specified output stream.
     *
     * @param out  the output stream to write to
     * @throws IOException if an I/O error occurs, such as if the stream is
     * closed
     * @see java.io.ByteArrayOutputStream#writeTo(OutputStream)
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        int remaining = totalCount;
        for (byte[] buf : buffers) {
            int c = Math.min(buf.length, remaining);
            out.write(buf, 0, c);
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
    }

    /**
     * <p>
     * Fetches entire contents of an <code>InputStream</code> and represent
     * same data as result InputStream.
     * </p>
     * <p>
     * This method is useful where,
     * </p>
     * <ul>
     * <li>Source InputStream is slow.</li>
     * <li>It has network resources associated, so we cannot keep it open for
     * long time.</li>
     * <li>It has network timeout associated.</li>
     * </ul>
     * <p>
     * It can be used in favor of {@link #toByteArray()}, since it
     * avoids unnecessary allocation and copy of byte[].<br>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * </p>
     * <p>
     * This method closes the supplied input stream before returning a new one.
     * </p>
     *
     * @param input Stream to be fully buffered.
     * @return A fully buffered stream.
     * @throws IOException if an I/O error occurs
     */
    public static InputStream toBufferedInputStream(InputStream input)
            throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                InputStream is = input) {
            output.write(is);
            return output.toBufferedInputStream();
        }
    }

    /**
     * Gets the current contents of this byte stream as a Input Stream. The
     * returned stream is backed by buffers of <code>this</code> stream,
     * avoiding memory allocation and copy, thus saving space and time.<br>
     *
     * @return the current contents of this output stream.
     * @see java.io.ByteArrayOutputStream#toByteArray()
     * @see #reset()
     */
    private InputStream toBufferedInputStream() {
        int remaining = totalCount;
        if (remaining == 0) {
            return new ClosedInputStream();
        }
        List<ByteArrayInputStream> list = new ArrayList<>(buffers.size());
        for (byte[] buf : buffers) {
            int c = Math.min(buf.length, remaining);
            list.add(new ByteArrayInputStream(buf, 0, c));
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
        return new SequenceInputStream(Collections.enumeration(list));
    }

    /**
     * Gets the current contents of this byte stream as a byte array.
     * The result is independent of this stream.
     *
     * @return the current contents of this output stream, as a byte array
     * @see java.io.ByteArrayOutputStream#toByteArray()
     */
    public synchronized byte[] toByteArray() {
        int remaining = totalCount;
        if (remaining == 0) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        byte[] newbuf = new byte[remaining];
        int pos = 0;
        for (byte[] buf : buffers) {
            int c = Math.min(buf.length, remaining);
            System.arraycopy(buf, 0, newbuf, pos, c);
            pos += c;
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
        return newbuf;
    }

    /**
     * Gets the current contents of this byte stream as a string.
     * @return the contents of the byte array as a String
     * @see java.io.ByteArrayOutputStream#toString()
     */
    @Override
    public String toString() {
        return new String(toByteArray());
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the specified encoding.
     *
     * @param enc  the name of the character encoding
     * @return the string converted from the byte array
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @see java.io.ByteArrayOutputStream#toString(String)
     */
    public String toString(String enc) throws UnsupportedEncodingException {
        return new String(toByteArray(), enc);
    }
}