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

/**
 * Filter for limiting the exceptions to be eligible for retry.
 * @author Pascal Essiembre
 * @see Retrier
 * @since 1.13.0 (previously part of now deprecated JEF API)
 */
public interface IExceptionFilter {

    /**
     * Filters an exception. Runtime exceptions can be of any type,
     * but checked exceptions are always wrapped
     * in a {@link RetriableException}.
     * @param e the exception to filter
     * @return <code>true</code> if the exception should trigger a retry,
     *         <code>false</code> to abort execution
     */
    boolean retry(Exception e);
}
