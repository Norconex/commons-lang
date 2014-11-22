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
package com.norconex.commons.lang.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.velocity.app.event.IncludeEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.util.ContextAware;

/**
 * Velocity include event handler that check for includes both relative
 * to a template location, and absolute to the current file system root
 * otherwise.   Used by {@link ConfigurationLoader}.
 * @author Pascal Essiembre
 */
public class RelativeIncludeEventHandler 
        implements IncludeEventHandler, ContextAware {

    private static final Logger LOG = LogManager.getLogger(
            RelativeIncludeEventHandler.class);
    private Context context;
    
    @Override
    @SuppressWarnings("nls")
    public String includeEvent(
            String includeResourcePath,
            String currentResourcePath, 
            String directiveName) {
        // Get main template file
        String inclFile;
        if (includeResourcePath.startsWith("/")
                || includeResourcePath.startsWith("\\")
                || includeResourcePath.startsWith("file://")
                || includeResourcePath.matches("^[A-Za-z]:\\.*")) {
            inclFile = includeResourcePath;
        } else {
            String baseDir = FilenameUtils.getFullPath(currentResourcePath);
            inclFile = FilenameUtils.normalize(baseDir + includeResourcePath);
        }
        
        // Load template properties if present
        if (context != null) {
            File vars = new File(FilenameUtils.getFullPath(inclFile) + 
                    FilenameUtils.getBaseName(inclFile) + ".properties");
            if (vars.exists() && vars.isFile()) {
                Properties props = new Properties();
                FileInputStream is = null;
                try {
                    is = new FileInputStream(vars);
                    props.load(is);
                    for (Entry<Object, Object> entry: props.entrySet()) {
                        context.put((String) entry.getKey(), entry.getValue());
                    }
                } catch (IOException e) {
                    LOG.error("Cannot load properties for template (skipped): "
                            + vars, e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
        return inclFile;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

}
