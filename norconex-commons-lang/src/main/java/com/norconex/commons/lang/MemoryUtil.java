/* Copyright 2010-2014 Norconex Inc.
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

/**
 * Memory-related utility methods.
 * @author Pascal Essiembre
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
        return getFreeMemory(false);
    }
    
    /**
     * Gets the JVM free memory.
     * The free memory is calculated by taking the JVM current free memory, plus
     * the difference between the maximum possible JVM memory and the
     * total JVM memory taken so far.
     * @param attemptGC <code>true</code> to attempts to perform garbage 
     *     collection to free up as much unused memory as possible 
     *     before assessing how much memory remains in the JVM instance.
     * @return JVM free memory in bytes
     */
    public static long getFreeMemory(boolean attemptGC) {
        if (attemptGC) {
            System.gc();
        }
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory() 
                + (runtime.maxMemory() - runtime.totalMemory());
    }
}
