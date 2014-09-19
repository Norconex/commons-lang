/* Copyright 2014 Norconex Inc.
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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.MemoryUtil;
import com.norconex.commons.lang.file.FileUtil;
import com.norconex.commons.lang.unit.DataUnit;

/**
 * {@link InputStream} wrapper that can be re-read any number of times.  This 
 * class will cache the wrapped input steam content the first time it is read, 
 * and subsequent read will use the cache.   
 * <p/>
 * In order to re-use this InputStream, you must call {@link #rewind()} first
 * on it. Once done reading the stream, you will get the -1 character as 
 * expected, and it will remain at that until you rewind or dispose.
 * <p/>
 * Starting reading the stream again will start reading bytes from the 
 * beginning (re)using its internal cache.
 * <p/>
 * Calling {@link #close()} has
 * no effect, and the cache data remains available for subsequent read.
 * <p/>
 * To explicitly dispose of resources allocated to the cache, you can 
 * use the {@link #dispose()} method.  
 * Attempting to read a disposed instance will throw an {@link IOException}.
 * It is recommended you explicitly dispose of <code>CachedInputStream</code>
 * instances to speed up the release of resources. Otherwise, resources are
 * de-allocated automatically when the instance is finalized.
 * <p/>
 * The internal cache stores read bytes into memory, up to to the 
 * specified maximum cache size. If content exceeds
 * the cache limit, the cache transforms itself into a fast file-based cache
 * of unlimited size.  Default memory cache size is 128 KB.
 * <p/>
 * @author Pascal Essiembre
 * @since 1.5
 */
public class CachedInputStream extends InputStream {

    private static final Logger LOG = 
            LogManager.getLogger(CachedInputStream.class);
    
    public static final int DEFAULT_MAX_CACHE_MEMORY = 
            (int) DataUnit.KB.toBytes(128);

    private static final int MINIMUM_FREE_JVM_MEMORY_FOR_MEM_CACHE = 
            (int) DataUnit.MB.toBytes(10);

    
    private final int maxCacheSize;
    
    private InputStream inputStream;
    
    private byte[] memCache;
    private ByteArrayOutputStream memOutputStream;
    
    private File fileCache;
    private OutputStream fileOutputStream;
    
    private boolean firstRead = true;
    private boolean needNewStream = false;
    private boolean cacheEmpty = true;
    private Integer bufferLimit = null;
    private boolean disposed = false;
    
    private final File cacheDirectory;
    
    /**
     * Caches the wrapped InputStream.
     * @param is InputStream to cache
     */
    public CachedInputStream(InputStream is) {
        this(is, DEFAULT_MAX_CACHE_MEMORY);
    }

    /**
     * Caches the wrapped InputStream.
     * @param is InputStream to cache
     * @param cacheDirectory directory where to store large content
     */
    public CachedInputStream(
            InputStream is, File cacheDirectory) {
        this(is, DEFAULT_MAX_CACHE_MEMORY, cacheDirectory);
    }
    
    /**
     * Caches the wrapped InputStream.
     * @param is InputStream to cache
     * @param maxCacheSize maximum byte size of memory cache 
     *        (before caching to file).
     */
    public CachedInputStream(InputStream is, int maxCacheSize) {
        this(is, maxCacheSize, null);
    }
    
    /**
     * Caches the wrapped InputStream.
     * @param is InputStream to cache
     * @param maxCacheSize maximum byte size of memory cache 
     *        (before caching to file).
     * @param cacheDirectory directory where to store large content
     */
    public CachedInputStream(
            InputStream is, int maxCacheSize, File cacheDirectory) {
        super();
        
        //TODO instead of checking for remaining memory (or in addition),
        // have a static init parameter to specify absolut maximum
        // memory all cached input streams can take?
        
        long freeMem = MemoryUtil.getFreeMemory(true);
        if (freeMem < MINIMUM_FREE_JVM_MEMORY_FOR_MEM_CACHE) {
            LOG.warn("Not enough memory remaining to create memory cache for "
                    + "new CachedInputStream instance. Using file cache");
            this.maxCacheSize = 0;
        } else if (freeMem / 2 < maxCacheSize) {
            LOG.info("Maximum memory cache for CachedInputStream cannot be "
                    + "higher than half the remaining JVM memory. "
                    + "Reducing it.");
            this.maxCacheSize = (int) freeMem / 2;
        } else {
            this.maxCacheSize = maxCacheSize;
        }
        
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
    /*default*/ CachedInputStream(byte[] memCache) {
        this.maxCacheSize = -1;
        this.memCache = memCache;
        this.cacheDirectory = null;
        firstRead = false;
        needNewStream = true;
    }
    /**
     * Creates an input stream with an existing file cache.
     * @param cacheFile the file cache
     */
    /*default*/ CachedInputStream(File cacheFile) {
        this.maxCacheSize = -1;
        this.fileCache = cacheFile;
        this.cacheDirectory = null;
        firstRead = false;
        needNewStream = true;
    }

    /*default*/ Integer getBufferLimit() {
        return bufferLimit;
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
            } else if (memOutputStream.size() == maxCacheSize) {
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
        } else if (num > 0) {
//            cacheEmpty = false;
        }

        if (firstRead) {
            if (fileOutputStream != null) {
                fileOutputStream.write(b, 0, num);
            } else if (memOutputStream.size() + num >= maxCacheSize) {
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
    
    @SuppressWarnings("resource")
    private void cacheToFile() throws IOException {
        fileCache = File.createTempFile(
                "CachedInputStream-", "-temp", cacheDirectory);
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
        } else {// if (memCache != null) {
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
