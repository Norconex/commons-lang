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

/**
 * <p>
 * Listener that is being notified every time a chunk of bytes is
 * processed from a given input stream.
 * </p>
 * <p>
 * Since 1.13.0, use {@link InputStreamLineListener} to listen to each line
 * being streamed from text files.
 * </p>
 * <p>
 * Should not be considered thread safe.
 * </p>
 * @see InputStreamConsumer
 * @see InputStreamLineListener
 * @deprecated Use {@link InputStreamListener} instead.
 */
@Deprecated(since="3.0.0")
@FunctionalInterface
public interface IInputStreamListener extends InputStreamListener { //NOSONAR
}
