/* Copyright 2017-2019 Norconex Inc.
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

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetrierTest {

    @Test
    public void testRetrierMaxRetryReached() {
        final MutableInt count = new MutableInt();
        try {
            new Retrier(20).setMaxCauses(5).execute(() -> {
                count.increment();
                throw new RuntimeException(count.toString());
            });
        } catch (RetriableException e) {
            // initial run + 20 retries == 21
            Assertions.assertEquals(21, count.intValue());
            // Only keeps 5 causes
            Assertions.assertEquals(5, e.getAllCauses().length);
            Assertions.assertEquals("17", e.getAllCauses()[0].getMessage());
            Assertions.assertEquals("21", e.getCause().getMessage());
        }
    }

    @Test
    public void testRetrierExceptionFilter() {
        final MutableInt count = new MutableInt();
        try {
            new Retrier(e -> "retryMe".equals(e.getMessage()), 20).execute(() -> {
                count.increment();
                if (count.intValue() < 7) {
                    throw new RuntimeException("retryMe");
                }
                throw new RuntimeException("failMe");
            });
        } catch (RetriableException e) {
            // Failed for real after 7 attempts
            Assertions.assertEquals(7, count.intValue());
            Assertions.assertEquals(7, e.getAllCauses().length);
            Assertions.assertEquals("retryMe", e.getAllCauses()[0].getMessage());
            Assertions.assertEquals("failMe", e.getCause().getMessage());
        }
    }
}
