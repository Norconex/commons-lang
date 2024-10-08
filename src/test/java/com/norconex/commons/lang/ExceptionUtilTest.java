/* Copyright 2022 Norconex Inc.
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
package com.norconex.commons.lang;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExceptionUtilTest {

    @Test
    void testGetFormattedMessages() {
        var msg = ExceptionUtil.getFormattedMessages(
                new RuntimeException("A parent runtime exception.",
                        new RuntimeException(null,
                                new NullPointerException(
                                        "A null pointer exception."))));
        assertThat(msg).isEqualTo(
                """
                RuntimeException: A parent runtime exception.
                  → RuntimeException:\s
                    → NullPointerException: A null pointer exception.""");
    }

    @Test
    void testGetMessageList() {
        var msgs = ExceptionUtil.getMessageList(
                new RuntimeException("A parent runtime exception.",
                        new RuntimeException(null,
                                new NullPointerException(
                                        "A null pointer exception."))));
        assertThat(msgs).containsExactly(
                "RuntimeException: A parent runtime exception.",
                "RuntimeException: ",
                "NullPointerException: A null pointer exception.");
    }
}
