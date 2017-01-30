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

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.Sleeper;

/**
 * This class is responsible for executing {@link IRetriable}
 * instances.  Upon reaching the maximum number of retries allowed, it 
 * will throw a {@link RetriableException}, wrapping the last exceptions 
 * encountered, if any, to a configurable maximum. 
 * @author Pascal Essiembre
 * @since 1.13.0 (previously "RetriableExecutor" part of 
 *        <a href="https://www.norconex.com/jef/api/">JEF API</a> 4.0).
 */
public class Retrier {

    private static final Logger LOG = LogManager.getLogger(Retrier.class);

    /** Default maximum number of retries. */
    public static final int DEFAULT_MAX_RETRIES = 10;
    /** Default wait time (milliseconds) before making a new attempt. */
    public static final long DEFAULT_RETRY_DELAY = 0;
    /** Default maximum number of exception causes kept. */
    public static final int DEFAULT_MAX_CAUSES_KEPT = 10;
    
    private static final Exception[] EMPTY_EXCEPTIONS = new Exception[] {};
    
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private long retryDelay = DEFAULT_RETRY_DELAY;
    private IExceptionFilter exceptionFilter;
    private int maxCauses = DEFAULT_MAX_CAUSES_KEPT;

    /**
     * Creates a new instance with the default maximum retries and default 
     * retry delay (no delay).
     */
    public Retrier() {
        super();
    }
    /**
     * Creates a new instance with the default retry delay (no delay). 
     * @param maxRetries maximum number of execution retries
     */
    public Retrier(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    /**
     * Creates a new instance which will retry execution 
     * only if the exception thrown by an attempt is accepted by
     * the {@link IExceptionFilter} (up to <code>maxRetries</code>).
     * Uses the default maximum retries and default retry delay.
     * @param exceptionFilter exception filter
     */    
    public Retrier(IExceptionFilter exceptionFilter) {
        this.exceptionFilter = exceptionFilter;
    }
    /**
     * Creates a new instance which will retry execution 
     * only if the exception thrown by an attempt is accepted by
     * the {@link IExceptionFilter} (up to <code>maxRetries</code>).
     * @param exceptionFilter exception filter
     * @param maxRetries maximum number of retries
     */
    public Retrier(IExceptionFilter exceptionFilter, int maxRetries) {
        super();
        this.maxRetries = maxRetries;
        this.exceptionFilter = exceptionFilter;
    }

    /**
     * Gets the maximum number of retries (the initial run does not count as 
     * a retry).
     * @return maximum number of retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }
    /**
     * Sets the maximum number of retries (the initial run does not count as 
     * a retry).
     * @param maxRetries maximum number of retries
     * @return this instance
     */
    public Retrier setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }
    /**
     * Gets the delay in milliseconds before attempting to execute again.
     * @return delay in milliseconds
     */
    public long getRetryDelay() {
        return retryDelay;
    }
    /**
     * Sets the delay in milliseconds before attempting to execute again.
     * @param retryDelay delay in milliseconds
     * @return this instance
     */
    public Retrier setRetryDelay(long retryDelay) {
        this.retryDelay = retryDelay;
        return this;
    }
    /**
     * Sets an exception filter that limits the exceptions eligible for retry.
     * @return exception filter
     */
    public IExceptionFilter getExceptionFilter() {
        return exceptionFilter;
    }
    /**
     * Sets an exception filter that limits the exceptions eligible for retry.
     * @param exceptionFilter exception filter
     * @return this instance
     */
    public Retrier setExceptionFilter(IExceptionFilter exceptionFilter) {
        this.exceptionFilter = exceptionFilter;
        return this;
    }
    /**
     * Gets the maximum number of exception causes to keep when all attempts
     * were made and a {@link RetriableException} is thrown.
     * @return max number of causes
     */
    public int getMaxCauses() {
        return maxCauses;
    }
    /**
     * Sets the maximum number of exception causes to keep when all attempts
     * were made and a {@link RetriableException} is thrown.
     * @param maxCauses max number of causes
     * @return this instance
     */
    public Retrier setMaxCauses(int maxCauses) {
        this.maxCauses = maxCauses;
        return this;
    }
    /**
     * Runs the {@link IRetriable} instance.  This method is not thread safe.
     * @param retriable the code to run
     * @return execution output if any, or null
     * @throws RetriableException wrapper around last exception encountered
     * or exception thrown when max rerun attempts is reached.
     */
    public <T> T execute(IRetriable<T> retriable) throws RetriableException {
        int attemptCount = 0;
        CircularFifoQueue<Exception> exceptions = 
                new CircularFifoQueue<>(maxCauses);
        while (attemptCount <= maxRetries) {
            try {
                return retriable.execute();
            } catch (Exception e) {
                exceptions.add(e);
                if (exceptionFilter != null && !exceptionFilter.retry(e)) {
                    throw new RetriableException(
                            "Encountered an exception preventing "
                          + "execution retry.", 
                          exceptions.toArray(EMPTY_EXCEPTIONS));
                }
            }
            attemptCount++;
            if (attemptCount < maxRetries) {
                LOG.warn("Execution failed, retrying ("
                        + attemptCount + " of " + maxRetries
                        + " maximum retries).", 
                        exceptions.get(exceptions.size() -1));
                Sleeper.sleepMillis(retryDelay);
            }
        }
        throw new RetriableException(
                "Execution failed, maximum number of retries reached.", 
                exceptions.toArray(EMPTY_EXCEPTIONS));
    }
}
