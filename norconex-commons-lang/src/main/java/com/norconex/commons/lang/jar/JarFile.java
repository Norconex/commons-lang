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
import java.io.FileFilter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;


/**
 * Simple Jar file representation holding name and version information. 
 * @author Pascal Essiembre
 * @since 1.10.0
 */
public class JarFile implements Comparable<JarFile> {

    public static final FileFilter FILTER = 
            new SuffixFileFilter(".jar");
    
    private final File path;
    private final String fullName;
    private final String baseName;
    private final String version;
    private final Version comparableVersion;
    
    public JarFile(File jarFile) {
        super();
        
        if (jarFile == null) {
            throw new NullPointerException("jarFile argument cannot be null."); 
        }
        if (!jarFile.isFile()) {
            throw new IllegalArgumentException(
                    "jarFile must be a valid file: " + jarFile); 
        }
        if (!jarFile.getName().endsWith(".jar")) {
            throw new IllegalArgumentException(
                    "jarFile must end with .jar extension: " + jarFile); 
        }
        this.path = jarFile;
        this.fullName = jarFile.getName();
        
        Pattern p = Pattern.compile("(.*?)-(\\d[\\.\\d\\w-]*)\\.jar");
        Matcher m = p.matcher(fullName);
        if (m.find()) {
            String jarName = m.group(1);
            String jarVersion = m.group(2);
            this.baseName = jarName;
            this.version = jarVersion;
        } else {
            this.baseName = fullName;
            this.version = null;
        }
        this.comparableVersion = new Version(this.version);
    }

    public File getPath() {
        return path;
    }
    public String getFullName() {
        return fullName;
    }
    public String getBaseName() {
        return baseName;
    }
    public String getVersion() {
        return version;
    }
    public Date getLastModified() {
        return new Date(path.lastModified());
    }
    
    public boolean isVersionGreaterThan(JarFile file) {
        return compareTo(file) < 0;
    }
    
    @Override
    public String toString() {
        return path.toString();
    }
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof JarFile)) {
            return false;
        }
        JarFile castOther = (JarFile) other;
        return path.equals(castOther.getPath());
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(path)
                .toHashCode();
    }

    @Override
    public int compareTo(JarFile o) {
        int result = comparableVersion.compareTo(o.comparableVersion);
        if (result != 0) {
            return result;
        }
        return path.compareTo(o.path);
    }
    
    private class Version implements Comparable<Version> {
        Object[] segments;

        public Version(String version) {
            super();
            if (version == null) {
                segments = new Object[0];
            } else {
                String[] parts = StringUtils.split(version, ".-");
                segments = new Object[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (NumberUtils.isDigits(part)) {
                        segments[i] = NumberUtils.toInt(part);
                    } else {
                        segments[i] = part;
                    }
                }
            }
        }
        @Override
        public int compareTo(Version o) {
            if (segments.equals(o.segments)) {
                return 0;
            }
            if (segments.length == 0 && o.segments.length > 0) {
                return -1;
            }
            if (segments.length > 0 && o.segments.length == 0) {
                return 1;
            }

            int maxLength = Math.max(segments.length, o.segments.length);
            
            for (int i = 0; i < maxLength; i++) {
                Object mine = null;
                Object other = null;
                if (i < segments.length) {
                    mine = segments[i];
                }
                if (i < o.segments.length) {
                    other = o.segments[i];
                }

                // if digit (1.2.[11]) vs null (1.2[]), digit wins
                if (mine instanceof Integer && other == null) {
                    return 1;
                }
                
                // if text (1.2-[SNAPSHOT] vs null (1.2[]), null wins
                if (mine instanceof String && other == null) {
                    return -1;
                }

                // if digit (1.2.[11]) vs text (1.2-[SNAPSHOT]), digit wins
                if (mine instanceof Integer && other instanceof String) {
                    return 1;
                }
                
                // if text (1.2-[SNAPSHOT]) vs digit (1.2.[11]), digit wins
                if (mine instanceof String && other instanceof Integer) {
                    return -1;
                }
                
                // if two digits, compare as such, reversing compare 
                // (highest first)
                if (mine instanceof Integer && other instanceof Integer) {
                    int result = ((Integer) mine).compareTo((Integer) other);
                    if (result != 0) {
                        return result *= -1;
                    }
                }
                
                // if two text, compare as such (shall we favor some text over
                // others??? like beta over alpha?)
                if (mine instanceof String && other instanceof String) {
                    int result = ((String) mine).compareTo((String) other);
                    if (result != 0) {
                        return result;
                    }
                }
            }
            // it is a tie... weird this should not happen 
            return 0;
        }        
    }
}
