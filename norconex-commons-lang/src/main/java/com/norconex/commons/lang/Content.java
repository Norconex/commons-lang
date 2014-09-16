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
package com.norconex.commons.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.unit.DataUnit;

/**
 * Represents any type of content (binary or text, document or string, etc).
 * The content can be re-read multiple times.
 * This class is not thread-safe.
 * @author Pascal Essiembre
 * @since 1.5.0
 */
public class Content {
    
    private static final Logger LOG = LogManager.getLogger(Content.class);
    
    //TODO Have max cache size being JVM/classloader scope.  Check every 
    // x changes if we can still use memory before deciding to swap to file.
    // Maybe make have a few cache scope options.
    
    public static final int DEFAULT_MAX_MEMORY_CACHE_SIZE = 
            (int) DataUnit.MB.toBytes(1);
    
    private CachedInputStream cacheStream;
    
    public Content(File file) throws FileNotFoundException {
        this(new FileInputStream(file), DEFAULT_MAX_MEMORY_CACHE_SIZE);
    }
    public Content(File file, int maxMemoryCacheSize) 
            throws FileNotFoundException {
        this(new FileInputStream(file), maxMemoryCacheSize);
    }
    public Content(InputStream is) {
        this(is, DEFAULT_MAX_MEMORY_CACHE_SIZE);
    }
    public Content(InputStream is, int maxMemoryCacheSize) {
        super();
        if (is == null) {
            cacheStream = new CachedInputStream(new NullInputStream(0), 0);
        } else {
            cacheStream = new CachedInputStream(is, maxMemoryCacheSize);
        }
    }
    public Content(CachedInputStream is) {
        cacheStream = is;
    }
    public Content(String string) {
        this(string, DEFAULT_MAX_MEMORY_CACHE_SIZE);
    }
    public Content(String string, int maxMemoryCacheSize) {
        InputStream is = null;
        try {
            is = IOUtils.toInputStream(string, CharEncoding.UTF_8);
        } catch (IOException e) {
            LOG.error("Could not get input stream with UTF-8 encoding. "
                    + "Trying with default encoding.", e);
            is = IOUtils.toInputStream(string);
        }
        cacheStream = new CachedInputStream(is, maxMemoryCacheSize);
    }
    /**
     * Creates an empty content.
     */
    public Content() {
        this(StringUtils.EMPTY, 0);
    }
    
    /**
     * Gets the content input stream. 
     * This method is not thread-safe.   
     * @return input stream
     */
    public CachedInputStream getInputStream() {
        cacheStream.rewind();
        return cacheStream;
    }
    
    /**
     * Releases any resources attached to this content (clears this content
     * cache).   
     * @throws IOException could not dispose content
     */
    public void dispose() throws IOException {
        if (cacheStream != null) {
            cacheStream.dispose();
        }
        cacheStream = null;
    }
    
    @Override
    protected void finalize() throws Throwable {
        IOUtils.closeQuietly(cacheStream);
        super.finalize();
    }
}
