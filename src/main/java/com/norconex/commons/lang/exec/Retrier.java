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
package com.norconex.commons.lang.exec;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.ExceptionUtil;
import com.norconex.commons.lang.Sleeper;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This class is responsible for executing {@link Retriable}
 * instances.  Upon reaching the maximum number of retries allowed, it
 * will throw a {@link RetriableException}, wrapping the last exceptions
 * encountered, if any, to a configurable maximum.
 * @since 1.13.0
 */
@EqualsAndHashCode
@ToString
public class Retrier {

    private static final Logger LOG = LoggerFactory.getLogger(Retrier.class);

    /** Default maximum number of retries. */
    public static final int DEFAULT_MAX_RETRIES = 10;
    /** Default wait time (milliseconds) before making a new attempt. */
    public static final long DEFAULT_RETRY_DELAY = 0;
    /** Default maximum number of exception causes kept. */
    public static final int DEFAULT_MAX_CAUSES_KEPT = 10;

    private static final Exception[] EMPTY_EXCEPTIONS = {};

    private int maxRetries = DEFAULT_MAX_RETRIES;
    private long retryDelay = DEFAULT_RETRY_DELAY;
    private ExceptionFilter exceptionFilter;
    private int maxCauses = DEFAULT_MAX_CAUSES_KEPT;

    /**
     * Creates a new instance with the default maximum retries and default
     * retry delay (no delay).
     */
    public Retrier() {
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
     * the {@link ExceptionFilter} (up to <code>maxRetries</code>).
     * Uses the default maximum retries and default retry delay.
     * @param exceptionFilter exception filter
     */
    public Retrier(ExceptionFilter exceptionFilter) {
        this.exceptionFilter = exceptionFilter;
    }
    /**
     * Creates a new instance which will retry execution
     * only if the exception thrown by an attempt is accepted by
     * the {@link ExceptionFilter} (up to <code>maxRetries</code>).
     * @param exceptionFilter exception filter
     * @param maxRetries maximum number of retries
     */
    public Retrier(ExceptionFilter exceptionFilter, int maxRetries) {
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
    public ExceptionFilter getExceptionFilter() {
        return exceptionFilter;
    }
    /**
     * Sets an exception filter that limits the exceptions eligible for retry.
     * @param exceptionFilter exception filter
     * @return this instance
     */
    public Retrier setExceptionFilter(ExceptionFilter exceptionFilter) {
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
     * Runs the {@link Retriable} instance.  This method is only
     * thread-safe if not modified after being used for the first time,
     * and the exception filter is thread-safe (or <code>null</code>).
     * @param retriable the code to run
     * @param <T> type of optional return value
     * @return execution output if any, or null
     * @throws RetriableException wrapper around last exception encountered
     * or exception thrown when max rerun attempts is reached.
     */
    public <T> T execute(Retriable<T> retriable) throws RetriableException {
        int retryCount = 0;
        CircularFifoQueue<Exception> exceptions =
                new CircularFifoQueue<>(maxCauses);
        // we do <= instead of just < because there are always
        // 1 execution + x retries
        while (retryCount <= maxRetries) {
            try {
                T val = retriable.execute();
                if (retryCount > 0) {
                    LOG.info("Execution successfully recovered on attempt #{}",
                            retryCount + 1);
                }
                return val;
            } catch (Exception e) {
                exceptions.add(e);
                if (exceptionFilter != null && !exceptionFilter.retry(e)) {
                    throw new RetriableException(
                            "Encountered an exception preventing "
                          + "execution retry.",
                          exceptions.toArray(EMPTY_EXCEPTIONS));
                }
            }
            retryCount++;
            if (retryCount <= maxRetries) {
                 LOG.warn("Execution failed, retrying "
                        + "({} of {} maximum retries). Cause:\n{}",
                        retryCount, maxRetries,
                        ExceptionUtil.getFormattedMessages(
                                exceptions.get(exceptions.size() -1)));
                Sleeper.sleepMillis(retryDelay);
            }
        }
        throw new RetriableException(
                "Execution failed, maximum number of retries reached.",
                exceptions.toArray(EMPTY_EXCEPTIONS));
    }
}
