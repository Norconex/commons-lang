/* Copyright 2014 Norconex Inc.
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
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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
 * @author Pascal Essiembre
 * @since 1.5
 * @see CachedStreamFactory
 */
public class CachedInputStream extends InputStream implements ICachedStream {

    private static final Logger LOG = 
            LogManager.getLogger(CachedInputStream.class);
    
    private final CachedStreamFactory factory;
    private final MemoryTracker tracker;
    
    private InputStream inputStream;
    
    private byte[] memCache;
    private ByteArrayOutputStream memOutputStream;
    
    private File fileCache;
    private OutputStream fileOutputStream;
    
    private boolean firstRead = true;
    private boolean needNewStream = false;
    private boolean cacheEmpty = true;
    private boolean disposed = false;
    
    private final File cacheDirectory;
    
    /**
     * Caches the wrapped InputStream.
     * @param is InputStream to cache
     * @param cacheDirectory directory where to store large content
     */
    /*default*/ CachedInputStream(CachedStreamFactory factory, 
            InputStream is, File cacheDirectory) {
        super();
        
        this.factory = factory;
        this.tracker = factory.new MemoryTracker();

        
        memOutputStream = new ByteArrayOutputStream();
        
        if (is instanceof BufferedInputStream) {
            this.inputStream = is;
        } else {
            this.inputStream = new BufferedInputStream(is);
        }
        if (cacheDirectory == null) {
            this.cacheDirectory = FileUtils.getTempDirectory();
        } else {
            this.cacheDirectory = cacheDirectory;
        }
    }

    /**
     * Creates an input stream with an existing memory cache.
     * @param byteBuffer the InputStream cache.
     */
    /*default*/ CachedInputStream(
            CachedStreamFactory factory, byte[] memCache) {
        this.factory = factory;
        this.tracker = factory.new MemoryTracker();
        this.memCache = ArrayUtils.clone(memCache);
        this.cacheDirectory = null;
        firstRead = false;
        needNewStream = true;
    }
    /**
     * Creates an input stream with an existing file cache.
     * @param cacheFile the file cache
     */
    /*default*/ CachedInputStream(CachedStreamFactory factory, File cacheFile) {
        this.factory = factory;
        this.tracker = factory.new MemoryTracker();
        this.fileCache = cacheFile;
        this.cacheDirectory = null;
        firstRead = false;
        needNewStream = true;
    }

    @Override
    public int read() throws IOException {
        if (disposed) {
            throw new IOException("CachedInputStream has been disposed.");
        }
        
        if (needNewStream) {
            createInputStreamFromCache();
        }
        if (firstRead) {
            int read = inputStream.read();
            if (read == -1) {
                return read;
            }
            if (fileOutputStream != null) {
                // Write to file cache
                fileOutputStream.write(read);
            } else if (!tracker.hasEnoughAvailableMemory(memOutputStream, 1)) {
                // Too big: create file cache and write to it.
                cacheToFile();
                fileOutputStream.write(read);
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
        if (needNewStream) {
            createInputStreamFromCache();
        }
        int num = inputStream.read(b, off, len);
        cacheEmpty = false;
        if (num == -1) {
            return num;
        }

        if (firstRead) {
            if (fileOutputStream != null) {
                fileOutputStream.write(b, 0, num);
            } else if (!tracker.hasEnoughAvailableMemory(
                    memOutputStream, num)) {
                cacheToFile();
                fileOutputStream.write(b, 0, num);
            } else {
                memOutputStream.write(b, 0, num);
            }
        }
        return num;
    }
    
    public void rewind() {
        if (!cacheEmpty) {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(memOutputStream);
            IOUtils.closeQuietly(fileOutputStream);
            fileOutputStream = null;
            firstRead = false;
            needNewStream = true;
            if (memOutputStream != null) {
                LOG.debug("Creating memory cache from cached stream.");
                memCache = memOutputStream.toByteArray();
                memOutputStream = null;
            }
        }
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
        if (fileOutputStream != null) {
            fileOutputStream.flush();
            fileOutputStream.close();
            fileOutputStream = null;
        }
        if (fileCache != null) {
            FileUtil.delete(fileCache);
            LOG.debug("Deleted cache file: " + fileCache);
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
    public final File getCacheDirectory() {
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
     * Creates a new {@link CachedInputStream} using the same factory settings
     * that were used to create this instance.
     * @param file file to create the input stream from
     * @return cached input stream
     */
    public CachedInputStream newInputStream(File file) {
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
    
    @SuppressWarnings("resource")
    private void cacheToFile() throws IOException {
        fileCache = File.createTempFile(
                "CachedInputStream-", "-temp", cacheDirectory);
        fileCache.deleteOnExit();
        LOG.debug("Reached max cache size. Swapping to file: " + fileCache);
        RandomAccessFile f = new RandomAccessFile(fileCache, "rw");
        FileChannel channel = f.getChannel();
        fileOutputStream = Channels.newOutputStream(channel);

        IOUtils.write(memOutputStream.toByteArray(), fileOutputStream);
        memOutputStream = null;
    }

    @SuppressWarnings("resource")
    private void createInputStreamFromCache() throws FileNotFoundException {
        if (fileCache != null) {
            LOG.debug("Creating new input stream from file cache.");
            RandomAccessFile f = new RandomAccessFile(fileCache, "r");
            FileChannel channel = f.getChannel();
            inputStream = Channels.newInputStream(channel);
        } else {
            LOG.debug("Creating new input stream from memory cache.");
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
