/* Copyright 2010-2018 Norconex Inc.
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
package com.norconex.commons.lang.config;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.event.IncludeEventHandler;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Velocity include event handler that check for includes both relative
 * to a template location, and absolute to the current file system root
 * otherwise.   Used by {@link ConfigurationLoader}.
 */
public class RelativeIncludeEventHandler implements IncludeEventHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(RelativeIncludeEventHandler.class);

    @Override
    public String includeEvent(Context context, String includeResourcePath,
            String currentResourcePath, String directiveName) {

        // Get main template file
        String inclFile;
        if (includeResourcePath.startsWith("/")
                || includeResourcePath.startsWith("\\")
                || includeResourcePath.startsWith("file://")
                || includeResourcePath.matches("^[A-Za-z]:\\.*")) {
            inclFile = includeResourcePath;
        } else {
            var baseDir = FilenameUtils.getFullPath(currentResourcePath);
            inclFile = FilenameUtils.normalize(baseDir + includeResourcePath);
        }

        if (StringUtils.isBlank(inclFile)) {
            throw new ConfigurationException("Cannot resolve relative "
                    + "include/parse resource path: " + includeResourcePath
                    + " (relative to: " + currentResourcePath + "). "
                    + "Possible cause: using a relative path to identify the "
                    + "parent template. Try with an absolute path.");
        }

        // Load template properties if present
        if (context != null) {
            VariablesFileResolver.resolve(
                    FilenameUtils.getFullPath(inclFile),
                    FilenameUtils.getBaseName(inclFile))
                .forEach(context::put);
        }
        LOG.debug("Resolved include/parse template file: {}", inclFile);
        return inclFile;
    }
}
