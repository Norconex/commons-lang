/* Copyright 2010-2014 Norconex Inc.
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
 * Runtime <code>Sleep</code> exception wrapping any 
 * {@link InterruptedException} thrown.
 * @see Sleeper
 */
public class SleeperException extends RuntimeException {

    private static final long serialVersionUID = -6879301747242838385L;

    /**
     * @see Exception#Exception(java.lang.String, java.lang.Throwable)
     * @param msg exception message
     * @param cause exception root cause
     */
    public SleeperException(
            final String msg, final InterruptedException cause) {
        super(msg, cause);
    }
}
