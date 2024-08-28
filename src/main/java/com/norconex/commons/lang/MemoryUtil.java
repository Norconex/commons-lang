/* Copyright 2010-2017 Norconex Inc.
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
package com.norconex.commons.lang;

/**
 * Memory-related utility methods.
 * @since 1.5.0
 */
public final class MemoryUtil {

    private MemoryUtil() {
        super();
    }

    /**
     * Gets the JVM free memory.
     * The free memory is calculated by taking the JVM current free memory, plus
     * the difference between the maximum possible JVM memory and the
     * total JVM memory taken so far.
     * @return JVM free memory in bytes
     */
    public static long getFreeMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory()
                + (runtime.maxMemory() - runtime.totalMemory());
    }
}
