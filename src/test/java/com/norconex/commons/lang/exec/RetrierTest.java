/* Copyright 2017-2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RetrierTest {

    @Test
    void testRetrierDefaults() {
        Retrier r = new Retrier();
        r.setExceptionFilter(null);
        assertThat(r.getMaxCauses()).isEqualTo(Retrier.DEFAULT_MAX_CAUSES_KEPT);
        assertThat(r.getMaxRetries()).isEqualTo(Retrier.DEFAULT_MAX_RETRIES);
        assertThat(r.getRetryDelay()).isEqualTo(Retrier.DEFAULT_RETRY_DELAY);
        try {
            r.execute(() -> {
                throw new RuntimeException();
            });
        } catch (RetriableException e) {
            // initial run + default retries
            assertThat(e.getAllCauses()).hasSize(Retrier.DEFAULT_MAX_RETRIES);
        }
    }

    @Test
    void testRetrierMaxRetryReached() {
        final AtomicInteger count = new AtomicInteger();
        try {
            new Retrier(20).setMaxCauses(5).execute(() -> {
                count.incrementAndGet();
                throw new RuntimeException(count.toString());
            });
        } catch (RetriableException e) {
            // initial run + 20 retries == 21
            Assertions.assertEquals(21, count.get());
            // Only keeps 5 causes
            Assertions.assertEquals(5, e.getAllCauses().length);
            Assertions.assertEquals("17", e.getAllCauses()[0].getMessage());
            Assertions.assertEquals("21", e.getCause().getMessage());
        }
    }

    @Test
    void testRetrierExceptionFilter() {
        final AtomicInteger count = new AtomicInteger();
        try {
            new Retrier(e -> "retryMe".equals(e.getMessage()), 20)
                    .execute(() -> {
                        count.incrementAndGet();
                        if (count.get() < 7) {
                            throw new RuntimeException("retryMe");
                        }
                        throw new RuntimeException("failMe");
                    });
        } catch (RetriableException e) {
            // Failed for real after 7 attempts
            Assertions.assertEquals(7, count.get());
            Assertions.assertEquals(7, e.getAllCauses().length);
            Assertions.assertEquals("retryMe",
                    e.getAllCauses()[0].getMessage());
            Assertions.assertEquals("failMe", e.getCause().getMessage());
        }
    }

    @Test
    void testRetrierRecovered() {
        final AtomicInteger count = new AtomicInteger();
        Retrier r = new Retrier(e -> true);
        r.setMaxRetries(12);
        r.setRetryDelay(2);
        assertThat(r.getExceptionFilter()).isNotNull();

        int returnedCount = assertDoesNotThrow(() -> r.execute(() -> {
            count.incrementAndGet();
            if (count.get() != 4) {
                throw new RuntimeException("failMe");
            }
            return count.get();
        }));
        assertThat(returnedCount).isEqualTo(4);
    }
}
