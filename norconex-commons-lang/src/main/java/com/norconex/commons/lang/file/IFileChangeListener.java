/* Copyright 2010-2014 Norconex Inc.
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

import java.io.File;

/**
 * Listener for file changes, to be used with a {@link FileMonitor}.
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public interface IFileChangeListener {
    /**
     * Invoked when a file changes.
     * @param file changed file.
     */
    void fileChanged(File file);
}
