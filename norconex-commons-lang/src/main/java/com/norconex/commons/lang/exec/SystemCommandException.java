/* Copyright 2017 Norconex Inc.
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
package com.norconex.commons.lang.exec;

/**
 * An exception thrown by a executing {@link SystemCommand}.
 * @author Pascal Essiembre
 * @see SystemCommand
 * @since 1.13.0
 */
public class SystemCommandException extends Exception {

    private static final long serialVersionUID = 5236102272021889018L;

    /**
     * @see Exception#Exception(String)
     * @param message exception message
     */
    public SystemCommandException(final String message) {
        super(message);
    }
    /**
     * @see Exception#Exception(Throwable)
     * @param cause exception cause
     */
    public SystemCommandException(final Throwable cause) {
        super(cause);
    }
    /**
     * @see Exception#Exception(String, Throwable)
     * @param message exception message
     * @param cause exception cause
     */
    public SystemCommandException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
