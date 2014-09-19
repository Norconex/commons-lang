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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
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
 * {@link OutputStream} wrapper that caches the output so it can be retrieved
 * once as a {@link CachedInputStream}. Invoking {@link #getInputStream()}
 * effectively {@link #close()} this stream and it can no longer be written
 * to.  Obtaining an input stream before or instead of calling the close
 * method will not delete the cache content, but rather pass the reference 
 * to it to the CachedInputStream. 
 * <p/> 
 * The internal cache stores written bytes into memory, up to to the 
 * specified maximum cache size. If content exceeds
 * the cache limit, the cache transforms itself into a file-based cache
 * of unlimited size. Default memory cache size is 128 KB.
 * <p/>
 * @author Pascal Essiembre
 * @since 1.5
 */
public class CachedOutputStream extends OutputStream {

    private static final Logger LOG = 
            LogManager.getLogger(CachedOutputStream.class);
    
    public static final int DEFAULT_MAX_CACHE_MEMORY = 
            (int) DataUnit.KB.toBytes(128);

    private static final int MINIMUM_FREE_JVM_MEMORY_FOR_MEM_CACHE = 
            (int) DataUnit.MB.toBytes(10);
    
    private final int maxCacheSize;
    
    private OutputStream outputStream;
    
    private byte[] memCache;
    private ByteArrayOutputStream memOutputStream;
    
    private File fileCache;
    private OutputStream fileOutputStream;
    private boolean doneWriting = false;
    private boolean closed = false;
    private boolean cacheEmpty = true;
    private final File cacheDirectory;
    
    //--- Constructors ---------------------------------------------------------
    /**
     * Creates a new cached OutputStream.
     * @param cacheDirectory directory where to store large content
     */
    public CachedOutputStream(File cacheDirectory) {
        this(null, DEFAULT_MAX_CACHE_MEMORY, cacheDirectory);
    }
    /**
     * Creates a new cached OutputStream.
     * @param maxCacheSize maximum byte size of memory cache 
     *        (before caching to file).
     */
    public CachedOutputStream(int maxCacheSize) {
        this(null, maxCacheSize, null);
    }
    /**
     * Creates a new cached OutputStream.
     * @param maxCacheSize maximum byte size of memory cache 
     *        (before caching to file).
     * @param cacheDirectory directory where to store large content
     */
    public CachedOutputStream(int maxCacheSize, File cacheDirectory) {
        this(null, maxCacheSize, cacheDirectory);
    }
    /**
     * Caches the wrapped OutputStream.  The wrapped stream
     * will be written to as expected, but its content will be cached in this
     * instance.
     * @param out OutputStream to cache
     */
    public CachedOutputStream(OutputStream out) {
        this(out, DEFAULT_MAX_CACHE_MEMORY);
    }
    /**
     * Caches the wrapped OutputStream.The wrapped stream
     * will be written to as expected, but its content will be cached in this
     * instance.
     * @param out OutputStream to cache
     * @param cacheDirectory directory where to store large content
     */
    public CachedOutputStream(
            OutputStream out, File cacheDirectory) {
        this(out, DEFAULT_MAX_CACHE_MEMORY, cacheDirectory);
    }
    /**
     * Caches the wrapped OutputStream. The wrapped stream
     * will be written to as expected, but its content will be cached in this
     * instance.
     * @param out OutputStream to cache
     * @param maxCacheSize maximum byte size of memory cache 
     *        (before caching to file).
     */
    public CachedOutputStream(OutputStream out, int maxCacheSize) {
        this(out, maxCacheSize, null);
    }
    /**
     * Caches the wrapped OutputStream. The wrapped stream
     * will be written to as expected, but its content will be cached in this
     * instance.
     * @param out OutputStream to cache
     * @param maxCacheSize maximum byte size of memory cache 
     *        (before caching to file).
     * @param cacheDirectory directory where to store large content
     */
    public CachedOutputStream(
            OutputStream out, int maxCacheSize, File cacheDirectory) {
        super();
        
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

        if (out != null) {
            if (out instanceof BufferedOutputStream) {
                this.outputStream = out;
            } else {
                this.outputStream = new BufferedOutputStream(out);
            }
        }
        if (cacheDirectory == null) {
            this.cacheDirectory = FileUtils.getTempDirectory();
        } else {
            this.cacheDirectory = cacheDirectory;
        }
    }

    //--- Methods --------------------------------------------------------------

    @Override
    public void write(int b) throws IOException {
        if (doneWriting) {
            throw new IllegalStateException(
                    "Cannot write to this closed output stream.");
        }
        if (outputStream != null) {
            outputStream.write(b);
        }
        if (fileOutputStream != null) {
            // Write to file cache
            fileOutputStream.write(b);
        } else if (memOutputStream.size() == maxCacheSize) {
            // Too big: create file cache and write to it.
            cacheToFile();
            fileOutputStream.write(b);
        } else {
            // Write to memory cache
            memOutputStream.write(b);
        }
        cacheEmpty = false;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (doneWriting) {
            throw new IllegalStateException(
                    "Cannot write to this closed output stream.");
        }
        if (outputStream != null) {
            outputStream.write(b, off, len);
        }
        if (fileOutputStream != null) {
            fileOutputStream.write(b, off, len);
        } else if (memOutputStream.size() + (len - off) >= maxCacheSize) {
            cacheToFile();
            fileOutputStream.write(b, off, len);
        } else {
            memOutputStream.write(b, 0, len);
        }
        cacheEmpty = false;
    }

    public CachedInputStream getInputStream() throws IOException {
        if (closed) {
            throw new IllegalStateException("Cannot get CachedInputStream on a "
                    + "closed CachedOutputStream.");
        }
        CachedInputStream is = null;
        if (fileCache != null) {
            is = new CachedInputStream(fileCache);
        } else if (memCache != null) {
            is = new CachedInputStream(memCache);
        } else {
            memCache = memOutputStream.toByteArray();
            memOutputStream.close();
            memOutputStream = null;
            is = new CachedInputStream(memCache);
        }
        close(false);
        return is;
    }
    
    private void close(boolean clearCache) throws IOException {
        if (!closed) {
            closed = true;
            if (memCache != null && clearCache) {
                memCache = null;
            }
            if (outputStream != null) {
                outputStream.flush();
                IOUtils.closeQuietly(outputStream);
                outputStream = null;
            }
            if (fileOutputStream != null) {
                fileOutputStream.flush();
                IOUtils.closeQuietly(fileOutputStream);
                fileOutputStream = null;
            }
            if (fileCache != null && clearCache) {
                FileUtil.delete(fileCache);
                LOG.debug("Deleted cache file: " + fileCache);
                fileCache = null;
            }
            if (memOutputStream != null && clearCache) {
                memOutputStream.flush();
                memOutputStream.close();
                memOutputStream = null;
            }
            cacheEmpty = true;
        }
    }
    
    @Override
    public void close() throws IOException {
        close(true);
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
    
    @SuppressWarnings("resource")
    private void cacheToFile() throws IOException {
        fileCache = File.createTempFile(
                "CachedOutputStream-", "-temp", cacheDirectory);
        LOG.debug("Reached max cache size. Swapping to file: " + fileCache);
        RandomAccessFile f = new RandomAccessFile(fileCache, "rw");
        FileChannel channel = f.getChannel();
        fileOutputStream = Channels.newOutputStream(channel);
        
        IOUtils.write(memOutputStream.toByteArray(), fileOutputStream);
        memOutputStream = null;
    }
    
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
