/* Copyright 2014-2019 Norconex Inc.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.norconex.commons.lang.unit.DataUnit;

/**
 * <p>
 * Factory for input/output streams caching values for repeat usage.
 * </p>
 * <p>
 * When using a constructor without all arguments, default values are used.
 * System properties are first checked for such values under these keys:
 * </p>
 * <ul>
 *   <li><code>cachedstream.mem.pool</code>: Pool max memory.</li>
 *   <li><code>cachedstream.mem.instance</code>: Instances max memory.</li>
 *   <li><code>cachedstream.dir</code>: Cache directory.</li>
 * </ul>
 * <p>
 * The following are default initialization values when not supplied via
 * constructor arguments or System properties:
 * </p>
 * <ul>
 *   <li>Pool max memory: <i>1 GB</i></li>
 *   <li>Instances max memory: : <i>100 MB</i></li>
 *   <li>Cache directory: <i>Uses the system temporary directory.</i></li>
 * </ul>
 * <p>
 * Initialization values passed in constructor always take precedence.
 * </p>
 *
 */
public class CachedStreamFactory {

    public static final int DEFAULT_MAX_MEM_INSTANCE =
            DataUnit.MB.toBytes(100).intValue();
    public static final int DEFAULT_MAX_MEM_POOL =
            DataUnit.GB.toBytes(1).intValue();

    private static final String PROP_MAX_MEM_POOL = "cachedstream.mem.pool";
    private static final String PROP_MAX_MEM_INSTANCE =
            "cachedstream.mem.instance";
    private static final String PROP_DIR = "cachedstream.dir";

    private final int maxMemoryPool;
    private final int maxMemoryInstance;
    private final Path cacheDirectory;

    private final Map<CachedStream, Void> streams =
            Collections.synchronizedMap(new WeakHashMap<CachedStream, Void>());

    /**
     * Constructor.
     * @param maxMemoryPool maximum number of bytes used for memory caching by
     *     all instances created by this factory combined
     * @param maxMemoryInstance maximum number of bytes used for
     *     memory by each cached stream instance created
     */
    public CachedStreamFactory(
            int maxMemoryPool,
            int maxMemoryInstance) {
        this(maxMemoryPool, maxMemoryInstance, getDefaultCacheDirectory());
    }

    /**
     * Constructor.
     * @param maxMemoryPool maximum number of bytes used for memory caching by
     *     all instances created by this factory combined
     * @param maxMemoryInstance maximum number of bytes used for
     *     memory by each cached stream instance created
     * @param cacheDirectory location where file-based caching takes place
     * @since 2.0.0
     */
    public CachedStreamFactory(
            int maxMemoryPool,
            int maxMemoryInstance,
            Path cacheDirectory) {
        Objects.requireNonNull(
                cacheDirectory, "'cacheDirectory' must not be null");
        this.maxMemoryPool = maxMemoryPool;
        this.maxMemoryInstance = maxMemoryInstance;
        this.cacheDirectory = cacheDirectory;
    }

    /**
     * Creates a new instance with default memory values
     * (see class documentation).
     * @param cacheDirectory location where file-based caching takes place
     * @since 2.0.0
     */
    public CachedStreamFactory(Path cacheDirectory) {
        Objects.requireNonNull(
                cacheDirectory, "'cacheDirectory' must not be null");
        maxMemoryPool = getDefaultMaxMemoryPool();
        maxMemoryInstance = getDefaultMaxMemoryInstance();
        this.cacheDirectory = cacheDirectory;
    }

    /**
     * Creates a new instance with default values (see class documentation)
     * @since 2.0.0
     */
    public CachedStreamFactory() {
        this(getDefaultCacheDirectory());
    }

    private static int getDefaultMaxMemoryPool() {
        return NumberUtils.toInt(System.getProperty(
                PROP_MAX_MEM_POOL), DEFAULT_MAX_MEM_POOL);
    }

    private static int getDefaultMaxMemoryInstance() {
        return NumberUtils.toInt(System.getProperty(
                PROP_MAX_MEM_INSTANCE), DEFAULT_MAX_MEM_INSTANCE);
    }

    private static Path getDefaultCacheDirectory() {
        String dir = System.getProperty(PROP_DIR);
        if (StringUtils.isNotBlank(dir)) {
            return Paths.get(dir);
        }
        return FileUtils.getTempDirectory().toPath();
    }

    public int getMaxMemoryPool() {
        return maxMemoryPool;
    }

    public int getMaxMemoryInstance() {
        return maxMemoryInstance;
    }

    /*default*/ int getPoolCurrentMemory() {
        int byteSize = 0;
        synchronized (streams) {
            for (CachedStream stream : streams.keySet()) {
                if (stream != null) {
                    byteSize += stream.getMemCacheSize();
                }
            }
        }
        return byteSize;
    }

    /*default*/ int getPoolRemainingMemory() {
        return Math.max(0, maxMemoryPool - getPoolCurrentMemory());
    }

    /*default*/ CachedInputStream newInputStream(byte[] bytes) {
        return registerStream(
                new CachedInputStream(this, cacheDirectory, bytes));
    }

    /**
     * Creates an empty input stream.  Useful when you need an input stream
     * but null is not accepted.
     * @return an empty cached input stream
     */
    public CachedInputStream newInputStream() {
        return newInputStream(StringUtils.EMPTY);
    }

    /**
     * Creates a new input stream, assuming UTF-8 content.
     * @param content content to stream
     * @return cached input stream
     */
    public CachedInputStream newInputStream(String content) {
        return registerStream(new CachedInputStream(this, cacheDirectory,
                IOUtils.toInputStream(content, StandardCharsets.UTF_8)));
    }

    public CachedInputStream newInputStream(File file) {
        Objects.requireNonNull(file, "'file' must not be null");
        return newInputStream(file.toPath());
    }

    /**
     * Creates a new cached input stream.
     * @param path path where to cache large files
     * @return cached input stream
     * @since 2.0.0
     */
    public CachedInputStream newInputStream(Path path) {
        return registerStream(
                new CachedInputStream(this, cacheDirectory, path));
    }

    public CachedInputStream newInputStream(InputStream is) {
        return registerStream(new CachedInputStream(this, cacheDirectory, is));
    }

    public CachedOutputStream newOuputStream(OutputStream os) {
        return registerStream(new CachedOutputStream(this, cacheDirectory, os));
    }

    public CachedOutputStream newOuputStream() {
        return registerStream(
                new CachedOutputStream(this, cacheDirectory, null));
    }

    private <T extends CachedStream> T registerStream(T s) {
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
            int kbRead = memOutputStream.size() / CHECK_CHUNK_SIZE;
            if (kbRead != kbReadSoFar) {
                kbReadSoFar = kbRead;
                poolRemaining = getPoolRemainingMemory();
            }
            int remainingMemory = Math.min(
                    poolRemaining,
                    getMaxMemoryInstance() - memOutputStream.size());
            return bytesToAdd <= remainingMemory;
        }
    }

}
