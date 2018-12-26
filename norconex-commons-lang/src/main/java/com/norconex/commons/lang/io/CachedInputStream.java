/* Copyright 2014-2018 Norconex Inc.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.file.FileUtil;
import com.norconex.commons.lang.io.CachedStreamFactory.MemoryTracker;

/**
 * {@link InputStream} wrapper that can be re-read any number of times.  This
 * class will cache the wrapped input steam content the first time it is read,
 * and subsequent read will use the cache.
 * <br><br>
 * To create new instances of {@link CachedInputStream}, use the
 * {@link CachedStreamFactory} class.   Reusing the same factory
 * will ensure all {@link CachedInputStream} instances created share the same
 * combined maximum memory.  Invoking one of the
 * <code>newInputStream(...)</code> methods on this class have the same effect.
 * <br><br>
 * In order to re-use this InputStream, you must call {@link #rewind()} first
 * on it. Once done reading the stream, you will get the -1 character as
 * expected, and it will remain at that until you rewind or dispose.
 * <br><br>
 * Starting reading the stream again will start reading bytes from the
 * beginning (re)using its internal cache.
 * <br><br>
 * Calling {@link #close()} has
 * no effect, and the cache data remains available for subsequent read.
 * <br><br>
 * To explicitly dispose of resources allocated to the cache, you can
 * use the {@link #dispose()} method.
 * Attempting to read a disposed instance will throw an {@link IOException}.
 * It is recommended you explicitly dispose of <code>CachedInputStream</code>
 * instances to speed up the release of resources. Otherwise, resources are
 * de-allocated automatically when the instance is finalized.
 * <br><br>
 * The internal cache stores read bytes into memory, up to to the
 * specified maximum cache size. If content exceeds
 * the cache limit, the cache transforms itself into a fast file-based cache
 * of unlimited size.  Default memory cache size is 128 KB.
 * <br><br>
 * <b>Starting with 1.6.0</b>, <code>mark(int)</code> is supported. The mark
 * limit is always unlimited so the method argument is ignored.
 * <br><br>
 * @author Pascal Essiembre
 * @since 1.5.0
 * @see CachedStreamFactory
 */
public class CachedInputStream extends InputStream implements ICachedStream {

    private static final Logger LOG =
            LoggerFactory.getLogger(CachedInputStream.class);

    private static final int UNDEFINED_LENGTH = -42;

    private final CachedStreamFactory factory;
    private final MemoryTracker tracker;

    private InputStream inputStream;

    private byte[] memCache;
    private ByteArrayOutputStream memOutputStream;

    private Path fileCache;
    private RandomAccessFile randomAccessFile;

    private boolean firstRead = true;
    private boolean needNewStream = false;
    private boolean cacheEmpty = true;
    private boolean disposed = false;

    private final Path cacheDirectory;

    private int count;        // total number of bytes read so far
    private int pos = 0;      // byte position we are in
    private int markpos = -1; // position we want to go back to

    // undefined until a full read was performed
    private int length = UNDEFINED_LENGTH;

    /**
     * Caches the wrapped InputStream.
     * @param factory stream factory
     * @param cacheDirectory directory where to store large content
     * @param is InputStream to cache
     */
    /*default*/ CachedInputStream(
            CachedStreamFactory factory, Path cacheDirectory, InputStream is) {
        super();

        this.factory = factory;
        this.tracker = factory.new MemoryTracker();

        memOutputStream = new ByteArrayOutputStream();

        if (is instanceof BufferedInputStream) {
            this.inputStream = is;
        } else {
            this.inputStream = new BufferedInputStream(is);
        }
        this.cacheDirectory = nullSafeCacheDirectory(cacheDirectory);
    }

    /**
     * Creates an input stream with an existing memory cache.
     * @param factory stream factory
     * @param cacheDirectory directory where to store large content
     * @param byteBuffer the InputStream cache.
     */
    /*default*/ CachedInputStream(
            CachedStreamFactory factory, Path cacheDirectory, byte[] memCache) {
        this.factory = factory;
        this.tracker = factory.new MemoryTracker();
        this.memCache = ArrayUtils.clone(memCache);
        this.cacheDirectory = nullSafeCacheDirectory(cacheDirectory);
        this.firstRead = false;
        this.needNewStream = true;
        if (memCache != null) {
            this.length = memCache.length;
        }
    }
    /**
     * Creates an input stream with an existing file cache.
     * @param factory stream factory
     * @param cacheDirectory directory where to store large content
     * @param cacheFile the file cache
     */
    /*default*/ CachedInputStream(
            CachedStreamFactory factory, Path cacheDirectory, Path cacheFile) {
        this.factory = factory;
        this.tracker = factory.new MemoryTracker();
        this.fileCache = cacheFile;
        this.cacheDirectory = nullSafeCacheDirectory(cacheDirectory);
        this.firstRead = false;
        this.needNewStream = true;
        File file = cacheFile.toFile();
        if (file != null && file.exists() && file.isFile()) {
            this.length = (int) file.length();
        }
    }

    // Now called by all constructors to prevent NPE in case this instance
    // is used to obtain the cache temp dir for other usage.
    private static Path nullSafeCacheDirectory(Path cacheDir) {
        if (cacheDir == null) {
            return FileUtils.getTempDirectory().toPath();
        } else {
            return cacheDir;
        }
    }

    /**
     * Always <code>true</code> since 1.6.0.
     * @return <code>true</code>
     */
    @Override
    public boolean markSupported() {
        return true;
    }
    /**
     * The read limit value is ignored. Limit is always unlimited.
     * Supported since 1.6.0.
     * @param readlimit any value (ignored)
     */
    @Override
    public synchronized void mark(int readlimit) {
        markpos = pos;
    }
    /**
     * If no mark has previously been set, it resets to the beginning.
     * Supported since 1.6.0.
     */
    @Override
    public synchronized void reset() throws IOException {
        pos = markpos;
        markpos = -1;
    }

    /**
     * Whether caching is done in memory for this instance for what has been
     * read so far. Otherwise, file-based caching is used.
     * @return <code>true</code> if caching is in memory.
     */
    public boolean isInMemory() {
        return fileCache == null;
    }

    /**
     * Returns <code>true</code> if this input stream is empty (zero-lenght
     * content). Unless the stream has been fully read at least once, this
     * method is more efficient than checking if {@link #length()}
     * is zero.
     * @return <code>true</code> if empty
     * @throws IOException problem checking if empty
     * @since 2.0.0
     */
    public boolean isEmpty() throws IOException {
        mark(1);
        boolean empty = read() == -1;
        reset();
        return empty;
    }

    @Override
    public int read() throws IOException {
        if (disposed) {
            throw new IOException("CachedInputStream has been disposed.");
        }
        int cursor = pos;
        if (cursor < count) {
            int val = -1;
            if (isInMemory()) {
                if (memOutputStream != null) {
                    val = memOutputStream.getByte(cursor);
                } else {
                    if (cursor >= memCache.length) {
                        val = -1;
                    } else {
                        // Adding 0xFF is necessary to make it signed and avoid
                        // false -1.
                        val = memCache[cursor] & 0xFF;
                    }
                }
            } else {
                randomAccessFile.seek(cursor);
                val = randomAccessFile.read();
            }
            if (val != -1) {
                pos++;
            }
            return val;
        }

        int b = realRead();
        if (b != -1) {
            pos++;
            count++;
        }
        return b;
    }

    private int realRead() throws IOException {
        if (needNewStream) {
            createInputStreamFromCache();
        }
        if (firstRead) {
            int read = inputStream.read();
            if (read == -1) {
                return read;
            }
            if (randomAccessFile != null) {
                // Write to file cache
                randomAccessFile.write(read);
            } else if (!tracker.hasEnoughAvailableMemory(memOutputStream, 1)) {
                // Too big: create file cache and write to it.
                cacheToFile();
                randomAccessFile.write(read);
            } else {
                // Write to memory cache
                memOutputStream.write(read);
            }
            cacheEmpty = false;
            return read;
        }
        int read = inputStream.read();
        cacheEmpty = false;
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (disposed) {
            throw new IOException("CachedInputStream has been disposed.");
        }

        int cursor = pos;
        int read = 0;
        if (cursor < count) {
            int toRead = Math.min(len, count - cursor);
            if (isInMemory()) {
                if (memOutputStream != null) {
                    byte[] bytes = new byte[toRead];
                    read = memOutputStream.getBytes(bytes, cursor);
                    System.arraycopy(bytes, 0, b, off, toRead);
                } else {
                    if (cursor >= memCache.length) {
                        read = -1;
                    } else {
                        System.arraycopy(memCache, cursor, b, off, toRead);
                        read = toRead;
                    }
                }
            } else {
                randomAccessFile.seek(cursor);
                read = randomAccessFile.read(b, off, toRead);
            }
            if (read != -1) {
                pos += read;
            }
        }

        if (read != -1 && read < len) {
            int maxToRead = len - read;
            int remainingRead = realRead(b, off + read, maxToRead);
            if (remainingRead != -1) {
                pos += remainingRead;
                count += remainingRead;
            } else if (read > 0) {
                return read;
            }
            read += remainingRead;
        }
        return read;
    }

    private int realRead(byte[] b, int off, int len) throws IOException {
        if (needNewStream) {
            createInputStreamFromCache();
        }
        int num = inputStream.read(b, off, len);
        cacheEmpty = false;
        if (num == -1) {
            return num;
        }

        if (firstRead) {
            if (randomAccessFile != null) {
                randomAccessFile.write(b, off, num);
            } else if (!tracker.hasEnoughAvailableMemory(
                    memOutputStream, num)) {
                cacheToFile();
                randomAccessFile.write(b, off, num);
            } else {
                memOutputStream.write(b, off, num);
            }
        }
        return num;
    }

    /**
     * If not already fully cached, forces the inner input stream to be
     * fully cached.
     * @throws IOException could not enforce full caching
     */
    public void enforceFullCaching() throws IOException {
        if (firstRead) {
            IOUtils.copy(this, new NullOutputStream());
            length = count;
            firstRead = false;
        }
    }

    /**
     * Rewinds this stream so it can be read again from the beginning.
     * If this input stream was not fully read at least once, it will
     * be fully read first, so its entirety is cached properly.
     */
    public void rewind() {
        if (!cacheEmpty) {
            // Rewinding a stream that we not fully read will truncate
            // it. We finish reading it all to avoid that.
            if (firstRead) {
                try {
                    enforceFullCaching();
                } catch (IOException e) {
                    throw new StreamException("Could not read entire stream "
                            + "so rewind() can occur safely.", e);
                }
            }
            resetStream();
        }
    }

    private void resetStream() {
        // Rewind
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(memOutputStream);
        IOUtils.closeQuietly(randomAccessFile);
        randomAccessFile = null;
        firstRead = false;
        needNewStream = true;
        if (memOutputStream != null) {
            LOG.trace("Creating memory cache from cached stream.");
            memCache = memOutputStream.toByteArray();
            memOutputStream = null;
        }
        // Reset marking
        pos = 0;
        markpos = -1;
        count = 0;
    }

    public void dispose() throws IOException {
        if (memCache != null) {
            memCache = null;
        }
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
        if (memOutputStream != null) {
            memOutputStream.flush();
            memOutputStream.close();
            memOutputStream = null;
        }
        if (randomAccessFile != null) {
            randomAccessFile.close();
            randomAccessFile = null;
        }
        if (fileCache != null) {
            FileUtil.delete(fileCache.toFile());
            LOG.trace("Deleted cache file: {}", fileCache);
        }
        disposed = true;
        cacheEmpty = true;
    }

    @Override
    public int available() throws IOException {
        if (needNewStream) {
            createInputStreamFromCache();
        }
        if (inputStream == null) {
            return 0;
        }
        return inputStream.available();
    }

    /**
     * Gets the cache directory where temporary cache files are created.
     * @return the cache directory
     */
    @Override
    public final Path getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Returns <code>true</code> if was nothing to cache (no writing was
     * performed) or if the stream was closed.
     * @return <code>true</code> if empty
     */
    public boolean isCacheEmpty() {
        return cacheEmpty;
    }

    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public long getMemCacheSize() {
        if (memCache != null) {
            return memCache.length;
        }
        if (memOutputStream != null) {
            return memOutputStream.size();
        }
        return 0;
    }

    /**
     * <p>Gets the length of the cached input stream. The length represents the
     * number of bytes that were read from this input stream,
     * after it was read entirely at least once.</p>
     * <p><b>Note:</b> Invoking this method when this stream is only partially
     * read (on a first read) will force it to read entirely and cache the
     * inner input stream it wraps.  To prevent an unnecessary read cycle,
     * it is always best to invoke this method after this stream was fully
     * read through normal use first.
     * </p>
     * @return the byte length
     * @since 1.6.1
     */
    public int length() {
        if (length == UNDEFINED_LENGTH) {
            LOG.debug("Obtaining stream length before a stream "
                    + "of unknown lenght was fully read. "
                    + "This forces a full "
                    + "read just to get the length. To avoid this extra "
                    + "read cycle, consider calling "
                    + "the length() method after the stream has been "
                    + "fully read at least once through regular usage.");

            // Reset marking
            int savedPos = pos;
            int savedMarkpos = markpos;
            try {
                enforceFullCaching();
                resetStream();
                //TODO investigate having a seek(int) method instead
                IOUtils.skip(this, savedPos);
            } catch (IOException e) {
                throw new StreamException("Could not read entire stream "
                        + "to obtain its byte length.", e);
            }
            pos = savedPos;
            markpos = savedMarkpos;
        }
        return length;
    }

    /**
     * Creates a new {@link CachedInputStream} using the same factory settings
     * that were used to create this instance.
     * @param file file to create the input stream from
     * @return cached input stream
     */
    public CachedInputStream newInputStream(Path file) {
        return factory.newInputStream(file);
    }
    /**
     * Creates a new {@link CachedInputStream} using the same factory settings
     * that were used to create this instance.
     * @param is input stream
     * @return cached input stream
     */
    public CachedInputStream newInputStream(InputStream is) {
        return factory.newInputStream(is);
    }

    public CachedStreamFactory getStreamFactory() {
        return factory;
    }

    private void cacheToFile() throws IOException {
        fileCache = Files.createTempFile(
                cacheDirectory, "CachedInputStream-", "-temp");
        fileCache.toFile().deleteOnExit();
        LOG.trace("Reached max cache size. Swapping to file: {}", fileCache);
        randomAccessFile = new RandomAccessFile(fileCache.toFile(), "rw");
        randomAccessFile.write(memOutputStream.toByteArray());
        memOutputStream = null;
    }

    private void createInputStreamFromCache() throws FileNotFoundException {
        if (fileCache != null) {
            LOG.trace("Creating new input stream from file cache.");
            randomAccessFile = new RandomAccessFile(fileCache.toFile(), "r");
            FileChannel channel = randomAccessFile.getChannel();
            inputStream = Channels.newInputStream(channel);
        } else {
            LOG.trace("Creating new input stream from memory cache.");
            inputStream = new ByteArrayInputStream(memCache);
        }
        needNewStream = false;
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }
}
