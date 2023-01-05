/* Copyright 2022-2023 Norconex Inc.
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
package com.norconex.commons.lang.time;

import java.time.Duration;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Time the execution of arbitrary code and return the elapsed time.
 * @since 3.0.0
 */
public final class Timer {

    private Timer() {}

    /**
     * Returns the runnable execution time in milliseconds.
     * @param runnable the code to execute
     * @return elapsed time in milliseconds
     */
    public static long time(Runnable runnable) {
        return timeWatch(runnable).getTime();
    }

    /**
     * Returns the runnable execution time as a {@link Duration}.
     * @param runnable the code to execute
     * @return elapsed time as {@link Duration}
     */
    public static Duration timeDuration(Runnable runnable) {
        return Duration.ofMillis(time(runnable));
    }

    /**
     * Returns the runnable execution time as a stopped {@link StopWatch}.
     * @param runnable the code to execute
     * @return elapsed time as a stopped {@link StopWatch}
     */
    public static StopWatch timeWatch(Runnable runnable) {
        var watch = new StopWatch();
        watch.start();
        runnable.run();
        watch.stop();
        return watch;
    }
}
