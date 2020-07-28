/* Copyright 2010-2017 Norconex Inc.
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

import org.apache.commons.lang3.ArrayUtils;

/**
 * Exception thrown when {@link Retrier} failed to execute a 
 * {@link IRetriable} instance. In cases there were multiple failed attempts,
 * {@link #getCause()} will return the last exception captured.
 * You can get all exceptions captured by {@link Retrier}
 * with {@link #getAllCauses()} (up to a maximum specified by {@link Retrier}).
 * @author Pascal Essiembre
 * @see Retrier
 * @since 1.13.0 (previously part of 
 *        <a href="https://opensource.norconex.com/jef/api/">JEF API</a> 4.0).
 */
public class RetriableException extends Exception {

    private static final long serialVersionUID = 5236102272021889018L;

    private final Throwable[] causes; 
    
    /**
     * Constructor.
     * @param message exception message
     */
    /*default*/ RetriableException(final String message) {
        super(message);
        this.causes = null;
    }
    /**
     * Constructor.
     * @param causes exception causes
     */
    /*default*/ RetriableException(final Throwable... causes) {
        super(getLastCause(causes));
        this.causes = causes;
    }
    /**
     * @param message exception message
     * @param causes exception causes
     */
    /*default*/ RetriableException(
            final String message, final Throwable... causes) {
        super(message, getLastCause(causes));
        this.causes = causes;
    }
    
    public synchronized Throwable[] getAllCauses() {
        return ArrayUtils.clone(causes);
    }
    
    private static Throwable getLastCause(Throwable[] causes) {
        if (ArrayUtils.isEmpty(causes)) {
            return null;
        }
        return causes[causes.length -1];
    }
}
