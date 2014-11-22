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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.CharEncoding;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 
 * @author Pascal Essiembre
 */
public class CachedStreamFactory {

    private static final Logger LOG = LogManager.getLogger(
            CachedStreamFactory.class);
    
    private final int poolMaxMemory;
    private final int instanceMaxMemory;
    private final File cacheDirectory;
    
    private final Map<ICachedStream, Void> streams = 
            Collections.synchronizedMap(new WeakHashMap<ICachedStream, Void>());
    
    /**
     * Constructor.
     * @param poolMaxMemory maximum number of bytes used for memory caching by 
     *     all instances created by this factory combined
     * @param instanceMaxMemory maximum number of bytes used for 
     *     memory by each cached stream instance created 
     */
    public CachedStreamFactory(
            int poolMaxMemory, 
            int instanceMaxMemory) {
        this(poolMaxMemory, instanceMaxMemory, null);
    }
    /**
     * Constructor.
     * @param poolMaxMemory maximum number of bytes used for memory caching by 
     *     all instances created by this factory combined
     * @param instanceMaxMemory maximum number of bytes used for 
     *     memory by each cached stream instance created 
     * @param cacheDirectory location where file-based caching takes place
     */
    public CachedStreamFactory(
            int poolMaxMemory, 
            int instanceMaxMemory,
            File cacheDirectory) {
        this.poolMaxMemory = poolMaxMemory;
        this.instanceMaxMemory = instanceMaxMemory;
        if (cacheDirectory == null) {
            this.cacheDirectory = FileUtils.getTempDirectory();
        } else {
            this.cacheDirectory = cacheDirectory;
        }
    }

    public int getPoolMaxMemory() {
        return poolMaxMemory;
    }

    public int getInstanceMaxMemory() {
        return instanceMaxMemory;
    }
    
    /*default*/ int getPoolCurrentMemory() {
        int byteSize = 0;
        synchronized (streams) {
            for (ICachedStream stream : streams.keySet()) {
                if (stream != null) {
                    byteSize += stream.getMemCacheSize();
                }
            }
        }
        return byteSize;
    }
    /*default*/ int getPoolRemainingMemory() {
        return Math.max(0, poolMaxMemory - getPoolCurrentMemory());
    }
    
    /*default*/ CachedInputStream newInputStream(byte[] bytes) {
        return registerStream(new CachedInputStream(this, bytes));
    }

    /**
     * Creates an empty input stream.  Useful when you need an input stream
     * but null is not accepted.
     * @return an empty cached input stream
     */
    public CachedInputStream newInputStream() {
        return registerStream(new CachedInputStream(
                this, new NullInputStream(0), cacheDirectory));
    }
    public CachedInputStream newInputStream(String content) {
        InputStream is = null;
        try {
            is = IOUtils.toInputStream(content, CharEncoding.UTF_8);
        } catch (IOException e) {
            LOG.error("Could not get input stream with UTF-8 encoding. "
                    + "Trying with default encoding.", e);
            is = IOUtils.toInputStream(content);
        }
        return registerStream(new CachedInputStream(this, is, cacheDirectory));
    }
    public CachedInputStream newInputStream(File file) {
        return registerStream(new CachedInputStream(this, file));
    }
    public CachedInputStream newInputStream(InputStream is) {
        return registerStream(new CachedInputStream(this, is, cacheDirectory));
    }

    public CachedOutputStream newOuputStream(OutputStream os) {
        return registerStream(new CachedOutputStream(this, os, cacheDirectory));
    }
    public CachedOutputStream newOuputStream() {
        return registerStream(
                new CachedOutputStream(this, null, cacheDirectory));
    }
    
    private <T extends ICachedStream> T registerStream(T s) {
        synchronized (streams) {
            streams.put(s, null);
        }
        return s;
    }
    
    public class MemoryTracker {
        private static final int CHECK_CHUNK_SIZE = (int) FileUtils.ONE_KB;
        private int poolRemaining = -1;
        private int kbReadSoFar = -1;
        // Checks every KB read the new max CacheSize
        public boolean hasEnoughAvailableMemory(
                ByteArrayOutputStream memOutputStream,
                int bytesToAdd) {
            int kbRead = (int) (memOutputStream.size() / CHECK_CHUNK_SIZE);
            if (kbRead != kbReadSoFar) {
                kbReadSoFar = kbRead;
                poolRemaining = getPoolRemainingMemory();
            }
            int remainingMemory = Math.min(
                    poolRemaining, 
                    getInstanceMaxMemory() - memOutputStream.size());
            return bytesToAdd <= remainingMemory;
        }
    }

}
