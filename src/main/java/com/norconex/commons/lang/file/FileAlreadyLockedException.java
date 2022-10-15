/* Copyright 2021-2022 Norconex Inc.
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
package com.norconex.commons.lang.file;

import java.io.IOException;

import lombok.experimental.StandardException;

/**
 * Checked exception thrown by {@link FileLocker} when trying to create a
 * lock on an on an already locked file.
 * @author Pascal Essiembre
 * @see FileLocker
 * @since 2.0.0
 */
@StandardException
public class FileAlreadyLockedException extends IOException {
    private static final long serialVersionUID = 1L;
}
