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

import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.Sleeper;

/**
 * <p>
 * Simple file-based lock mechanism.  To prevent undeleted lock files
 * preventing execution (e.g. due to system crash), an interval of time
 * can be specified. The file will be "touched" a the specified interval
 * to keep its timestamp current.
 * If the file gets too old, it is considered as no longer being locked.
 * </p>
 * <p>
 * Trying to lock an active file lock results in a
 * {@link FileAlreadyLockedException}.
 * </p>
 * <p>
 * Trying to unlock an inexistant or inactive lock has no effect.
 * </p>
 * <p>
 * The locking scope is tied to the use of this class. No attempt is made to
 * instruct an actual file lock on a process or the underlying system.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class FileLock {

    private static final Logger LOG = LoggerFactory.getLogger(FileLock.class);

    public static final long NO_TOUCH = 0;
    private final Path lockFile;
    private final long touchInterval;

    public FileLock(Path lockFile) {
        this(lockFile, NO_TOUCH);
    }
    public FileLock(Path lockFile, Duration touchInterval) {
        this(lockFile, touchInterval == null
                ? NO_TOUCH : touchInterval.toMillis());
    }
    public FileLock(Path lockFile, long touchInterval) {
        super();
        this.lockFile = Objects.requireNonNull(
                lockFile, "'lockFile' must not be null.");
        this.touchInterval = touchInterval;
    }

    /**
     * <p>
     * Creates a file lock. The lock is considered active until one of these
     * conditions are met:
     * </p>
     * <ol>
     *   <li>{@link #unlock()} is called for the same lock file.</li>
     *   <li>Lock file gets deleted.</li>
     * </ol>
     * @return <code>true</code> if the lock file was already present
     * (but inactive).
     * @throws FileAlreadyLockedException if a lock file is already active
     * @throws IOException could not update the lock file timestamp
     */
    public synchronized boolean lock() throws IOException {
        LOG.debug("Activating file lock: {}", lockFile.toAbsolutePath());
        if (isLocked()) {
            throw new FileAlreadyLockedException(
                    "An active lock file already exists: "
                            + lockFile.toAbsolutePath());
        }
        boolean exists = lockFile.toFile().exists();
        FileUtils.touch(lockFile.toFile());
        if (touchInterval > NO_TOUCH) {
            Executors.newSingleThreadExecutor().submit(() -> {
                Thread.currentThread().setName("File locker");
                try {
                    while(lockFile.toFile().exists()) {
                        FileUtils.touch(lockFile.toFile());
                        Sleeper.sleepMillis(touchInterval);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(
                            "Cannot update lock timestamp.", e);
                }
            });
        }
        LOG.debug("File lock activated: {}", lockFile.toAbsolutePath());
        return exists;
    }
    /**
     * Unlock the file.
     * @return <code>true</code> if the lock file existed
     * @throws IOException
     */
    public synchronized boolean unlock() throws IOException {
        boolean exists = lockFile.toFile().exists();
        LOG.debug("Deactivating file lock: {}", lockFile.toAbsolutePath());
        Files.deleteIfExists(lockFile);
        LOG.debug("Deactivated file lock: {}", lockFile.toAbsolutePath());
        return exists;
    }
    /**
     * Whether a file is locked (existing and active if applicable).
     * @return <code>true</code> if locked
     */
    public synchronized boolean isLocked() {
        boolean exists = lockFile.toFile().exists();
        if (exists && touchInterval <= NO_TOUCH) {
            return true;
        }
        long delta = currentTimeMillis() - lockFile.toFile().lastModified();
        LOG.trace("Lock last touch: {}", delta);
        return exists && delta <= touchInterval;
    }
}
