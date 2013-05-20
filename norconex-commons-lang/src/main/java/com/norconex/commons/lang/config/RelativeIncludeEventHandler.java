/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.velocity.app.event.IncludeEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.util.ContextAware;

/**
 * Velocity include event handler that check for includes both relative
 * to a template location, and absolute to the current file system root
 * otherwise.   Used by {@link ConfigurationLoader}.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
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
                FileInputStream is;
                try {
                    is = new FileInputStream(vars);
                    props.load(is);
                    is.close();
                    Set<?> varNames = props.keySet();
                    for (Object varName : varNames) {
                        context.put((String) varName, props.get(varName));
                    }
                } catch (IOException e) {
                    LOG.error("Cannot load properties for template (skipped): "
                            + vars, e);
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
