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

import java.util.function.Predicate;

/**
 * Filters lines of text read from an InputStream decorated with
 * {@link FilteredInputStream}.
 * @deprecated use a String {@link Predicate} instead
 */
@Deprecated(since = "3.0.0")
@FunctionalInterface
public interface IInputStreamFilter extends Predicate<String> { //NOSONAR

    @Override
    default boolean test(String line) {
        return accept(line);
    }

    /**
     * Whether a line is "accepted" or not.  An accepted line is being
     * returned when read.
     * @param line line being read
     * @return <code>true</code> if line is accepted
     */
    boolean accept(String line);
}
