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
package com.norconex.commons.lang.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStore;
import java.security.Security;

import org.junit.jupiter.api.Test;

class KeyStoreBuilderTest {

    @Test
    void testKeyStoreBuilder() throws Exception {

        // Test that empty key store has a single dummy entry
        assertThat(KeyStoreBuilder.empty().create().size()).isOne();

        // There should be way more than 10 anywhere this test is run
        assertThat(KeyStoreBuilder
                .fromJavaHome()
                .setPassword(null)
                .setPasswordKey(null)
                .setProvider(Security.getProviders()[0].getName())
                .setType(KeyStore.getDefaultType())
                .create()
                .size()).isGreaterThan(10);
    }
}
