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

    public void setContext(Context context) {
        this.context = context;
    }

}
