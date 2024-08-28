/* Copyright 2023 Norconex Inc.
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
package com.norconex.commons.lang.config;

import java.util.function.Consumer;

/**
 * The implementing class can be configured via the object returned
 * by {@link #getConfiguration()}.
 * Provides a consistent way to obtain a file configuration dynamically.
 * Could be useful for object mapping, for instance.
 * @param <T> The configuration class type
 * @since 3.0.0
 */
public interface Configurable<T> {

    /**
     * Practical constant for the string representation of the
     * "configuration" property.
     */
    String PROPERTY = "configuration";

    /**
     * Gets the configuration for a configurable object.
     * Never <code>null</code>.
     * @return configuration
     */
    T getConfiguration();

    /**
     * <p>
     * Convenience method to facilitate configuring this instance in method
     * chaining. E.g.
     * </p>
     * <pre>
     * var myClass = Configurable.configure(
     *      new MyConfigurableClass(), cfg -> cfg.setSomeValue("my value"));
     * </pre>
     * <p>
     * You can supply multiple consumers and they'll all be executed in the
     * order provided. This is an alternative to using lambda
     * {@link Consumer#andThen(Consumer)} without the need for casting.
     * </p>
     * @param configurable the configurable object
     * @param configurers one or more object configuration consumers
     *      (not invoked if <code>null</code> or if the configuration
     *      is <code>null</code>)
     * @return this instance
     */
    @SafeVarargs
    static <T, R extends Configurable<T>> R configure(
            R configurable, Consumer<T>... configurers) {
        var cfg = configurable.getConfiguration();
        if (configurers != null && cfg != null) {
            for (Consumer<T> configurer : configurers) {
                configurer.accept(cfg);
            }
        }
        return configurable;
    }
}
