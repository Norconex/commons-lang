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

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;

public class RetrierTest {

    @Test
    public void testRetrierMaxRetryReached() {
        final MutableInt count = new MutableInt();
        try {
            new Retrier(20).setMaxCauses(5).execute(new IRetriable<Void>() {
                @Override
                public Void execute() throws RetriableException {
                    count.increment();
                    throw new RuntimeException(count.toString());
                }
            });
        } catch (RetriableException e) {
            // initial run + 20 retries == 21
            Assert.assertEquals(21, count.intValue());
            // Only keeps 5 causes
            Assert.assertEquals(5, e.getAllCauses().length);
            Assert.assertEquals("17", e.getAllCauses()[0].getMessage());
            Assert.assertEquals("21", e.getCause().getMessage());
        }
    }

    @Test
    public void testRetrierExceptionFilter() {
        final MutableInt count = new MutableInt();
        try {
            new Retrier(new IExceptionFilter() {
                @Override
                public boolean retry(Exception e) {
                    return "retryMe".equals(e.getMessage());
                }
            }, 20).execute(new IRetriable<Void>() {
                @Override
                public Void execute() throws RetriableException {
                    count.increment();
                    if (count.intValue() < 7) {
                        throw new RuntimeException("retryMe");
                    }
                    throw new RuntimeException("failMe");
                }
            });
        } catch (RetriableException e) {
            // Failed for real after 7 attempts
            Assert.assertEquals(7, count.intValue());
            Assert.assertEquals(7, e.getAllCauses().length);
            Assert.assertEquals("retryMe", e.getAllCauses()[0].getMessage());
            Assert.assertEquals("failMe", e.getCause().getMessage());
        }
    }
}
