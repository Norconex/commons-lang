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
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.file.FileUtil;
import com.norconex.commons.lang.unit.DataUnit;

//TODO check remaining memory and cache to file before capacity is reached
// if preferable.

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
    
    private OutputStream outputStream;
    
    private ByteBuffer byteBuffer; 
    private File cacheFile;
    private OutputStream cacheFileOutputStream;
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
        byteBuffer = ByteBuffer.allocate(maxCacheSize);
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
        if (cacheFileOutputStream != null) {
            cacheFileOutputStream.write(b);
        } else if (byteBuffer.position() + 1 == byteBuffer.capacity()) {
            cacheToFile();
            cacheFileOutputStream.write(b);
        } else {
            byteBuffer.put((byte) b);
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
        if (cacheFileOutputStream != null) {
            cacheFileOutputStream.write(b, off, len);
        } else if (byteBuffer.position() + (len - off) 
                >= byteBuffer.capacity()) {
            cacheToFile();
            cacheFileOutputStream.write(b, off, len);
        } else {
            byteBuffer.put(b, off, len);
        }
        cacheEmpty = false;

//        //vvvvvvvvvvvvvvvvvvvvvvvvvvv
//        try {
//            System.out.println("WRITE:" + IOUtils.toString(b));
//        } catch (IOException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//        //^^^^^^^^^^^^^^^^^^^^^^^^^^^
    
    }

    public CachedInputStream getInputStream() throws IOException {
        if (closed) {
            throw new IllegalStateException("Cannot get InputStream on a "
                    + "closed CachedOutputStream.");
        }
        CachedInputStream is = null;
        if (cacheFile != null) {
            is = new CachedInputStream(cacheFile);
        } else {
            byteBuffer.flip();
            is = new CachedInputStream(byteBuffer);
        }
        close(false);
        
//        //vvvvvvvvvvvvvvvvvvvvvvvvvvv
//        try {
//            System.out.println("READ:" + IOUtils.toString(is));
//        } catch (IOException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//        //^^^^^^^^^^^^^^^^^^^^^^^^^^^
        
        return is;
    }
    
    private void close(boolean clearCache) throws IOException {
        closed = true;
        if (byteBuffer != null) {
            if (clearCache) {
                byteBuffer.clear();
            }
            byteBuffer = null;
        }
        if (outputStream != null) {
            outputStream.flush();
            IOUtils.closeQuietly(outputStream);
            outputStream = null;
        }
        if (cacheFileOutputStream != null) {
            cacheFileOutputStream.flush();
            IOUtils.closeQuietly(cacheFileOutputStream);
            cacheFileOutputStream = null;
        }
        if (cacheFile != null) {
            if (clearCache) {
                FileUtil.delete(cacheFile);
                LOG.debug("Deleted cache file: " + cacheFile);
            }
            cacheFile = null;
        }
        cacheEmpty = true;
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
        cacheFile = File.createTempFile(
                "CachedOutputStream-", "-temp", cacheDirectory);
        LOG.debug("Reached max cache size. Swapping to file: " + cacheFile);
        RandomAccessFile f = new RandomAccessFile(cacheFile, "rw");
        FileChannel channel = f.getChannel();
        cacheFileOutputStream = Channels.newOutputStream(channel);
        byteBuffer.position(0);
        
        IOUtils.copy(new ByteBufferInputStream(
                byteBuffer), cacheFileOutputStream);
        byteBuffer.clear();
        byteBuffer = null;
    }
    
    
    @Override
    protected void finalize() throws Throwable {
        close();
    }
}
