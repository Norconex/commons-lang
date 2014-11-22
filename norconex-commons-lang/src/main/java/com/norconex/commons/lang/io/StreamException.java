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
package com.norconex.commons.lang.io;

/**
 * Runtime exception when dealing with I/O streams.
 * @author Pascal Essiembre
 */
public class StreamException extends RuntimeException {

    private static final long serialVersionUID = 2988235417669737473L;

    /**
     * Constructor.
     */
    public StreamException() {
    }
    /**
     * Constructor.
     * @param message exception message
     */
    public StreamException(String message) {
        super(message);
    }
    /**
     * Constructor.
     * @param cause exception cause
     */
    public StreamException(Throwable cause) {
        super(cause);
    }
    /**
     * Constructor.
     * @param message exception message
     * @param cause exception cause
     */
    public StreamException(String message, Throwable cause) {
        super(message, cause);
    }

}
