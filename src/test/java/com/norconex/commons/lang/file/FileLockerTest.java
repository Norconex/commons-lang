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
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLockerTest {

    @TempDir
    Path tempDir;

    @Test
    void testFileLock() throws IOException {
        // Loop a bunch of time to put locking under pressure for testing
        for (int i = 1; i <= 10; i++) {
            Path file = tempDir.resolve("test.lck");
            FileLocker locker = new FileLocker(tempDir.resolve("test.lck"));

            // File is not locked at first.
            Assertions.assertFalse(locker.isLocked());
            // Lock was never created so unlock does nothing.
            Assertions.assertDoesNotThrow(locker::unlock);

            // Creating lock for first time, should be just fine.
            Assertions.assertDoesNotThrow(() -> locker.lock());
            // File should now be locked.
            Assertions.assertTrue(locker.isLocked());
            // Locking an already locked file should throw exception
            Assertions.assertThrows(
                    FileAlreadyLockedException.class, locker::lock);
            // "Trying" to lock an already locked file should return false
            Assertions.assertFalse(locker.tryLock());

            // Given it was previously locked ok, it should unlock just fine.
            Assertions.assertDoesNotThrow(locker::unlock);
            // Confirms it is unlocked.
            Assertions.assertFalse(locker.isLocked());

            // Create the file manually to simulate it was locked by other
            // process, when it is not the case.
            FileUtils.touch(file.toFile());
            // Despite the lock file existence, it should not be locked.
            Assertions.assertFalse(locker.isLocked());
            // Despite the lock file existence, we should be able to lock it
            Assertions.assertDoesNotThrow(() -> locker.lock());

            // we are done, unlock
            Assertions.assertDoesNotThrow(locker::unlock);
            Assertions.assertFalse(locker.isLocked());
        }
    }

    @Test
    void testTryLock() throws IOException {
        // File was never locked, true on first attempt, false after
        FileLocker locker = new FileLocker(tempDir.resolve("try.lck"));
        Assertions.assertTrue(locker.tryLock());
        Assertions.assertFalse(locker.tryLock());
    }
}