/* Copyright 2014-2017 Norconex Inc.
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.file.FileUtil;
import com.norconex.commons.lang.io.CachedStreamFactory.MemoryTracker;

/**
 * {@link OutputStream} wrapper that caches the output so it can be retrieved
 * once as a {@link CachedInputStream}. Invoking {@link #getInputStream()}
 * effectively {@link #close()} this stream and it can no longer be written
 * to.  Obtaining an input stream before or instead of calling the close
 * method will not delete the cache content, but rather pass the reference 
 * to it to the CachedInputStream. 
 * <br><br> 
 * To create new instances of {@link CachedOutputStream}, use the
 * {@link CachedStreamFactory} class.   Reusing the same factory
 * will ensure all {@link CachedOutputStream} instances created share the same
 * combined maximum memory.  Invoking one of the 
 * <code>newOutputStream(...)</code> methods on this class have the same effect.
 * <br><br> 
 * The internal cache stores written bytes into memory, up to to the 
 * specified maximum cache size. If content exceeds
 * the cache limit, the cache transforms itself into a file-based cache
 * of unlimited size. Default memory cache size is 128 KB.
 * <br><br>
 * @author Pascal Essiembre
 * @since 1.5
 * @see CachedStreamFactory
 */
public class CachedOutputStream extends OutputStream
        implements ICachedStream {

    private static final Logger LOG = 
            LogManager.getLogger(CachedOutputStream.class);
    
    private final CachedStreamFactory factory;
    private final MemoryTracker tracker;
    
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
     * Caches the wrapped OutputStream. The wrapped stream
     * will be written to as expected, but its content will be cached in this
     * instance.
     * @param factory Cached stream factory
     * @param cacheDirectory directory where to store large content
     * @param out OutputStream to cache
     */
    /*default*/ CachedOutputStream(CachedStreamFactory factory, 
            File cacheDirectory, OutputStream out) {
        super();

        this.factory = factory;
        this.tracker = factory.new MemoryTracker();

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
        } else if (!tracker.hasEnoughAvailableMemory(memOutputStream, 1)) {
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
        } else if (!tracker.hasEnoughAvailableMemory(
                memOutputStream, len - off)) {
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
        CachedInputStream is;
        if (fileCache != null) {
            is = factory.newInputStream(fileCache);
        } else if (memCache != null) {
            is = factory.newInputStream(memCache);
        } else {
            memCache = memOutputStream.toByteArray();
            memOutputStream.close();
            memOutputStream = null;
            is = factory.newInputStream(memCache);
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
    
    public CachedStreamFactory getStreamFactory() {
        return factory;
    }
    
    /**
     * Returns <code>true</code> if was nothing to cache (no writing was 
     * performed) or if the stream was closed. 
     * @return <code>true</code> if empty
     */
    public boolean isCacheEmpty() {
        return cacheEmpty;
    }
    

    public CachedOutputStream newOuputStream(OutputStream os) {
        return factory.newOuputStream(os);
    }
    public CachedOutputStream newOuputStream() {
        return factory.newOuputStream();
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
    
    @SuppressWarnings("resource")
    private void cacheToFile() throws IOException {
        fileCache = File.createTempFile(
                "CachedOutputStream-", "-temp", cacheDirectory);
        fileCache.deleteOnExit();
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
