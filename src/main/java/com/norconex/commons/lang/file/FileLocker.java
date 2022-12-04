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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * <p>
 * Simple {@link FileLock} wrapper to use operating-system file-locking
 * ability to help with multi-processes management.
 * No instance of the file being locked is returned by this class.
 * It's main purpose is to prevent other processes from trying to acquire
 * a lock when it is not desirable to do so. For instance, it can be used
 * as a way to guarantee only one instance of a specific process can run
 * per operating system and file system.
 * </p>
 * <p>
 * The {@link #lock()}, {@link #tryLock()}, and {@link #unlock()} are similar
 * to {@link FileChannel#lock()}, {@link FileChannel#tryLock()}, and
 * {@link FileLock#release()} but with slightly different behaviors. Refer
 * to the corresponding method documentation for more detail.
 * </p>
 *
 * @since 2.0.0
 */
public class FileLocker {

    private final Path lockFile;
    private FileLock lock;
    private FileChannel fileChannel;

    public FileLocker(Path lockFile) {
        super();
        this.lockFile = Objects.requireNonNull(
                lockFile, "'lockFile' must not be null.");
    }

    /**
     * <p>
     * Tries to locks the unlocked file and returns <code>true</code> when
     * successful. If the file does not exist,
     * an empty file is created. If the file exists and is already locked,
     * this method silently fails and returns <code>false</code>.
     * </p>
     * @return <code>true</code> if file was successfully locked.
     * @throws IOException could not lock
     * @see #lock()
     */
    public synchronized boolean tryLock() throws IOException {
        if (isLocked()) {
            return false;
        }
        try {
            FileChannel fc = fileChannel();
            FileLock lck = lock(fc, false);
            if (lck != null) {
                this.fileChannel = fc;
                this.lock = lck;
            }
            return lock != null;
        } catch (OverlappingFileLockException e) {
            return false;
        }
    }

    /**
     * <p>
     * Locks the unlocked file. If the file does not exist, an empty file
     * is created. If the file exists and is already locked, this method
     * throws a {@link FileAlreadyLockedException}.
     * </p>
     * <p>
     * Contrary to {@link FileChannel#lock()}, this method does not block
     * by default. To have it block and wait for a lock to be free,
     * use {@link #lock(boolean)} with <code>true</code> instead.
     * </p>
     * @throws IOException could not lock
     * @see #tryLock()
     */
    public synchronized void lock() throws IOException {
        lock(false);
    }

    /**
     * <p>
     * Locks the unlocked file. If the file does not exist, an empty file
     * is created. If the file exists and is already locked, this method
     * throws a {@link FileAlreadyLockedException}.
     * </p>
     * @param block set to <code>true</code> to wait for an available lock
     * @throws IOException could not lock
     */
    public synchronized void lock(boolean block) throws IOException {
        if (!block && isLocked()) {
            throw alreadyLocked(null);
        }
        try {
            FileChannel fc = fileChannel();
            FileLock lck = lock(fc, block);
            if (lck == null) {
                throw alreadyLocked(null);
            }
            this.fileChannel = fc;
            this.lock = lck;
        } catch (OverlappingFileLockException e) {
            throw alreadyLocked(e);
        }
    }

    /**
     * <p>
     * Unlocks the locked file. Unlocking an inexistent or already unlocked
     * file has no effect.
     * </p>
     * @throws IOException could not unlock
     */
    public synchronized void unlock() throws IOException {
        try {
            if (lock != null) {
                lock.release();
                lock = null;
            }
        } finally {
            try {
                if (fileChannel != null) {
                    fileChannel.close();
                    fileChannel = null;
                }
            } finally {
                Files.deleteIfExists(lockFile);
            }
        }
    }

    /**
     * Gets whether the file is locked.
     * @return <code>true</code> if locked.
     */
    public synchronized boolean isLocked() {
        return lock != null && lock.isValid();
    }

    private FileChannel fileChannel() throws IOException {
        return FileChannel.open(lockFile,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.SYNC);
    }
    private FileLock lock(FileChannel fileChannel, boolean block)
            throws IOException {
        if (block) {
            return fileChannel.lock();
        }
        return fileChannel.tryLock();
    }
    private FileAlreadyLockedException alreadyLocked(Exception e) {
        return new FileAlreadyLockedException(
                lockFile.toAbsolutePath() + " already locked.", e);
    }
}
