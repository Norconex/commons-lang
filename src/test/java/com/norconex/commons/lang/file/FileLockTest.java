/* Copyright 2021 Norconex Inc.
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
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.Sleeper;

class FileLockTest {

    @TempDir
    Path tempDir;

    @Test
    void testFileLock() throws IOException {
        Path file = tempDir.resolve("test.lck");
        FileLock lock = new FileLock(tempDir.resolve("test.lck"), 500);

        Assertions.assertFalse(lock.isLocked());
        Assertions.assertFalse(lock.unlock());

        Assertions.assertFalse(lock.lock());
        Assertions.assertTrue(lock.isLocked());
        Assertions.assertThrows(FileAlreadyLockedException.class, lock::lock);

        Assertions.assertTrue(lock.unlock());
        Assertions.assertFalse(lock.isLocked());
        // Wait a bit to make sure it is done with its last sleep
        Sleeper.sleepMillis(500);

        // Create the file manually to simulate it was locked by other process
        FileUtils.touch(file.toFile());
        Assertions.assertTrue(lock.isLocked());
        Assertions.assertThrows(FileAlreadyLockedException.class, lock::lock);

        // we wait a bit and they we should be able to lock it
        Sleeper.sleepMillis(500);
        Assertions.assertFalse(lock.isLocked());
        // file already exists so returns true
        Assertions.assertTrue(lock.lock());
        Assertions.assertTrue(lock.isLocked());

        // we are done, unlock
        Assertions.assertTrue(lock.unlock());
        Assertions.assertFalse(lock.isLocked());
    }
}
