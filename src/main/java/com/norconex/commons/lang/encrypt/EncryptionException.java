/* Copyright 2015-2022 Norconex Inc.
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
package com.norconex.commons.lang.encrypt;

import lombok.experimental.StandardException;

/**
 * Runtime exception thrown if there is a problem with the encryption
 * or decryption.
 * @since 1.9.0
 */
@StandardException
public class EncryptionException extends RuntimeException {
    private static final long serialVersionUID = -2107977615189833553L;
}
