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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.xml.XML;

class CredentialsTest {

    @Test
    void testCredentials() {
        Credentials creds = new Credentials(
                "joe", "something-encrypted", new EncryptionKey("blah"));

        Credentials credsCopy = new Credentials();
        assertThat(credsCopy.isSet()).isFalse();
        credsCopy.copyFrom(creds);
        assertThat(credsCopy.isSet()).isTrue();
        assertThat(credsCopy).isEqualTo(creds);

        credsCopy = new Credentials();
        assertThat(credsCopy.isEmpty()).isTrue();
        creds.copyTo(credsCopy);
        assertThat(credsCopy.isEmpty()).isFalse();
        assertThat(credsCopy).isEqualTo(creds);

        credsCopy = new Credentials(creds);
        assertThat(credsCopy).isEqualTo(creds);

        assertThat(creds.toString()).contains(
                "password=********", "EncryptionKey [value=********");

        assertDoesNotThrow(() -> XML.assertWriteRead(creds, "credentials"));
    }
}
