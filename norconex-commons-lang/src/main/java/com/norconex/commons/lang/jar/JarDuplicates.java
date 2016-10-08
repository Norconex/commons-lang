/* Copyright 2016 Norconex Inc.
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
package com.norconex.commons.lang.jar;

import java.io.File;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Holds jar files that are considered duplicates (same name and same or 
 * different versions).
 * @author Pascal Essiembre
 * @since 1.10.0
 */
public class JarDuplicates {

    private final JarFile[] jarFiles;

    public JarDuplicates(JarFile... jarFiles) {
        super();
        if (ArrayUtils.isEmpty(jarFiles) || jarFiles.length < 2) {
            throw new IllegalArgumentException(
                    "Must have 2 or more jar files.");
        }
        this.jarFiles = jarFiles;
    }

    /** 
     * Gets the jar files being considered duplicates of each other.
     * @return jar files
     */
    public JarFile[] getJarFiles() {
        return jarFiles;
    }
    
    /**
     * Gets the jar file that is considered the most recent of the batch.
     * If two ore more jars share the same latest version, there is no 
     * guarantee which one will be returned. 
     * The best attempt is made to detect more recent version based
     * on version provided. Given version patterns may vary, there
     * is no guarantee of 100% accuracy, so use with caution.
     * @return latest jar file
     */
    public JarFile getLatestVersion() {
        return jarFiles[0];
    }
    
    /**
     * Whether all jar files share the same version or if at least one of them
     * have a different version.
     * @return <code>true</code> if at least one jar file has a different 
     *         version
     */
    public boolean hasVersionConflict() {
        if (ArrayUtils.isEmpty(jarFiles)) {
            return false;
        }
        String version = jarFiles[0].getVersion();
        for (JarFile jarFile : jarFiles) {
            if (!Objects.equals(version, jarFile.getVersion())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets whether this duplicate batch contains the given jar file.
     * @param jarFile jar file
     * @return <code>true</code> if this instance contains the given jar file
     */
    public boolean contains(File jarFile) {
        for (JarFile jar : jarFiles) {
            if (Objects.equals(jar.getPath(), jarFile)) {
                return true;
            }
        }
        return false;
    }
}
