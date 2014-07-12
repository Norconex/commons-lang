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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.file.FileUtil;
import com.norconex.commons.lang.unit.DataUnit;

//TODO check remaining memory and cache to file before capacity is reached
// if preferable.

/**
 * InputStream wrapper that can be re-read any number of times.  This class
 * will cache the wrapped input steam content the first time it is read, and
 * subsequent read will use the cache.   
 * <p/>
 * In order to re-use this InputStream, it must first be read fully.  
 * Once done reading the stream, you will get the -1 character as usual.
 * Starting reading the stream again will start reading bytes from the 
 * beginning (re)using its internal cache. <b>Do not close the stream 
 * until you are done re-using it.</b> The moment you close the stream, 
 * its internal cache will be wiped out and you will not be able to use it 
 * anymore.  It is important to <b>close the stream to delete any temporary 
 * cache file created</b>.
 * <p/>
 * The internal cache uses a {@link ByteBuffer} to store the stream content
 * into memory, up to the specified maximum cache size. If content exceeds
 * the cache limit, the cache transforms itself into a file-based cache
 * of unlimited size, using a fast {@link RandomAccessFile} to access it.
 * <p/>
 * Implementors can optionally dictate how and where temporary cache files
 * get created by overrriding {@link #newCacheFile()}.
 * <p/>
 * @author Pascal Essiembre
 * @since 1.5
 */
public class ReusableInputStream extends InputStream {

    private static final Logger LOG = 
            LogManager.getLogger(ReusableInputStream.class);
    
    public static final int DEFAULT_MAX_CACHE_MEMORY = 
            (int) DataUnit.KB.toBytes(128);
    
    private InputStream inputStream;
    
    private ByteBuffer byteBuffer; 
    private File cacheFile;
    private OutputStream fileOutputStream;
    private boolean firstRead = true;
    private boolean needNewStream = false;
    
    /**
     * Makes the wrapped InputSource re-usable.
     * @param is InputSource to make re-usable
     */
    public ReusableInputStream(InputStream is) {
        this(is, DEFAULT_MAX_CACHE_MEMORY);
    }
    
    /**
     * Makes the wrapped InputSource re-usable.  
     * @param is InputSource to make re-usable
     * @param maxCacheSize maximum byte size of memory cache 
     *        (before caching to file).
     */
    public ReusableInputStream(InputStream is, int maxCacheSize) {
        super();
        byteBuffer = ByteBuffer.allocate(maxCacheSize);
        if (is instanceof BufferedInputStream) {
            this.inputStream = is;
        } else {
            this.inputStream = new BufferedInputStream(is);
        }
    }

    @Override
    public int read() throws IOException {
        if (needNewStream) {
            createInputStreamFromCache();
        }
        if (firstRead) {
            int read = inputStream.read();
            if (read == -1) {
                innerClose();
                return read;
            }
            if (fileOutputStream != null) {
                fileOutputStream.write(read);
            } else if (byteBuffer.position() + 1 == byteBuffer.capacity()) {
                cacheToFile();
                fileOutputStream.write(read);
            } else {
                byteBuffer.put((byte) read);
            }
            return read;
        }
        int read = inputStream.read();
        if (read == -1) {
            innerClose();
        }
        return read;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (needNewStream) {
            createInputStreamFromCache();
        }
        if (firstRead) {
            int num = inputStream.read(b, off, len);
            if (num == -1) {
                innerClose();
                return num;
            }
            if (fileOutputStream != null) {
                fileOutputStream.write(b, 0, num);
            } else if (byteBuffer.position() + num == byteBuffer.capacity()) {
                cacheToFile();
                fileOutputStream.write(b, 0, num);
            } else {
                byteBuffer.put(b, 0, num);
            }
            return num;
        }
        int num = inputStream.read(b, off, len);
        if (num == -1) {
            innerClose();
        }
        return num;
    }
    
    private void innerClose() {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(fileOutputStream);
        fileOutputStream = null;
        firstRead = false;
        needNewStream = true;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (byteBuffer != null) {
            byteBuffer.clear();
            byteBuffer = null;
        }
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
        if (fileOutputStream != null) {
            fileOutputStream.flush();
            fileOutputStream.close();
            fileOutputStream = null;
        }
        if (cacheFile != null) {
            FileUtil.delete(cacheFile);
            LOG.debug("Deleted cache file: " + cacheFile);
        }
    }
    
    @SuppressWarnings("resource")
    private void cacheToFile() throws IOException {
        cacheFile = newCacheFile();
        LOG.debug("Reached max cache size. Swapping to file: " + cacheFile);
        RandomAccessFile f = new RandomAccessFile(cacheFile, "rw");
        FileChannel channel = f.getChannel();
        fileOutputStream = Channels.newOutputStream(channel);
        byteBuffer.position(0);
        
        IOUtils.copy(new ByteBufferInputStream(
                byteBuffer), fileOutputStream);
        byteBuffer.clear();
        byteBuffer = null;
    }

    /**
     * Creates a new temporary file used for caching larger content.
     * Can be overridden.
     * @return a file
     * @throws IOException could not create file
     */
    protected File newCacheFile() throws IOException {
        return File.createTempFile("importer-", "-temp");
    }
    
    @SuppressWarnings("resource")
    private void createInputStreamFromCache() throws FileNotFoundException {
        if (cacheFile != null) {
            LOG.debug("Creating new input stream from file cache.");
            RandomAccessFile f = new RandomAccessFile(cacheFile, "r");
            FileChannel channel = f.getChannel();
            inputStream = Channels.newInputStream(channel);
        } else {
            LOG.debug("Creating new input stream from memory cache.");
            byteBuffer.position(0);
            inputStream = new ByteBufferInputStream(byteBuffer);
        }
        needNewStream = false;
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
    }
}
