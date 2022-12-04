/* Copyright 2010-2022 Norconex Inc.
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

import java.nio.file.Path;

/**
 * Caches I/O streams for reuse. Applied on input and output stream
 * @since 3.0.0 (renamed from 2.x ICachedStream)
 */
public interface CachedStream {

    /**
     * Get the amount of bytes used for caching this stream.
     * @return amount of bytes
     */
    long getMemCacheSize();

    /**
     * Gets the cache directory where temporary cache files are created.
     * @return the cache directory
     * @since 1.14.0
     */
    Path getCacheDirectory();
}
