/* Copyright 2020 Norconex Inc.
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

import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * System-related convenience methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class SystemUtil {

    private SystemUtil() {
    }

    /**
     * <p>
     * Executes the supplied {@link Runnable} after setting the system
     * property and then resets that system property.
     * This method is synchronized to make sure
     * setting the property does not affect other threads.  The system property
     * is restored to its original value (if any) after execution.
     * Useful when the invoked code expects a system property that is
     * configurable and may change from one thread to another.
     * </p>
     * <h3><code>null</code> handling</h3>
     * <p>
     * This method is <code>null</code>-safe.
     * If the runnable is <code>null</code>, invoking this method has no effect.
     * If the property name is <code>null</code>, the runnable is
     * invoked without setting any property beforehand.
     * If the property value is <code>null</code>, it will temporary
     * clear any existing system property with the same name (if any) for
     * the duration of the execution.
     * </p>
     * @param name system property name
     * @param value system property value
     * @param runnable code to run with the system property set
     */
    public static synchronized void runWithProperty(
            String name, String value, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (name == null) {
            runnable.run();
            return;
        }
        String original = value == null
                ? System.clearProperty(name)
                : System.setProperty(name, value);
        try {
            runnable.run();
        } finally {
            if (original == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, original);
            }
        }
    }
    /**
     * <p>
     * Executes the supplied {@link Callable} after setting the system
     * property and then resets that system property.
     * This method is synchronized to make sure
     * setting the property does not affect other threads.  The system property
     * is restored to its original value (if any) after execution.
     * Useful when the invoked code expects a system property that is
     * configurable and may change from one thread to another.
     * </p>
     * <h3><code>null</code> handling</h3>
     * <p>
     * This method is <code>null</code>-safe.
     * If the callable is <code>null</code>, invoking this method returns
     * <code>null</code>.
     * If the property name is <code>null</code>, the runnable is
     * invoked without setting any property beforehand.
     * If the property value is <code>null</code>, it will temporary
     * clear any existing system property with the same name (if any)
     * for the duration of the execution.
     * </p>
     * @param name system property name
     * @param value system property value
     * @param callable code to run with the system property set
     * @param <T> class type of return value
     * @return the callable return value
     * @throws Exception any exception the callable may throw
     */
    public static synchronized <T> T callWithProperty(
            String name, String value, Callable<T> callable) throws Exception {
        if (callable == null) {
            return null;
        }
        if (name == null) {
            return callable.call();
        }
        String original = value == null
                ? System.clearProperty(name)
                : System.setProperty(name, value);
        try {
            return callable.call();
        } finally {
            if (original == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, original);
            }
        }
    }

    /**
     * Gets the environment variable or system property matching the exact
     * name, or a supported variant.
     * Same as invoking {@link #getEnvironment(String)} and if
     * <code>null</code>, invoking {@link #getProperty(String)}.
     * @param name property or environment name
     * @return property or environment value
     */
    public static String getEnvironmentOrProperty(String name) {
        String value = getEnvironment(name);
        if (value != null) {
            return value;
        }
        return getProperty(name);
    }
    /**
     * Gets the system property or environment variable matching the exact
     * name, or a supported variant.
     * Same as invoking {@link #getProperty(String)} and if
     * <code>null</code>, invoking {@link #getEnvironment(String)}.
     * @param name environment or property name
     * @return environment or property value
     */
    public static String getPropertyOrEnvironment(String name) {
        String value = getProperty(name);
        if (value != null) {
            return value;
        }
        return getEnvironment(name);
    }

    /**
     * Gets the value of the specified environment variable matching the exact
     * name, or a supported variant.
     * First, it looks for a direct environment variable name match,
     * as with {@link System#getenv(String)}. If no matches are found,
     * it will iterate through all environment variables names and compare
     * them all with the requested name, but only after stripping all
     * non alpha-numeric characters and ignoring case.
     * @param name the name of the environment variable
     * @return environment variable value
     */
    public static String getEnvironment(String name) {
        // 1. As-is environment variable
        String value = System.getenv(name);
        if (value != null) {
            return value;
        }

        // 2. Compact-insensitive matching environment variable
        String compactName = toAlphaNum(name);
        for (Entry<String, String> en : System.getenv().entrySet()) {
            String key = toAlphaNum(en.getKey());
            if (key.equalsIgnoreCase(compactName)) {
                return en.getValue();
            }
        }
        return null;
    }

    /**
     * Gets the value of the system property matching the exact name,
     * or a supported variant.
     * First, it looks for a direct property name match,
     * as with {@link System#getProperty(String)}. If no matches are found,
     * it will iterate through all system property names and compare them all
     * with the requested name, but only after stripping all non alpha-numeric
     * characters and ignoring case.
     * @param name the name of the system property
     * @return system property value
     */
    public static String getProperty(String name) {
        // 1. As-is system property
        String value = System.getProperty(name);
        if (value != null) {
            return value;
        }

        // 2. Compact-insensitive matching system property
        String compactName = toAlphaNum(name);
        for (Entry<Object, Object> en : System.getProperties().entrySet()) {
            String key = toAlphaNum(Objects.toString(en.getKey()));
            if (key.equalsIgnoreCase(compactName)) {
                return Objects.toString(en.getValue());
            }
        }
        return null;
    }

    private static String toAlphaNum(String value) {
        return value.replaceAll("[^a-zA-Z0-9]", "");
    }
}
