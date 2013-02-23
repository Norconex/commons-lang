package com.norconex.commons.lang.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * Class parsing a Velocity template (which can have parse/include directives)
 * and uses a properties file for Velocity variables.
 * Templates, whether the main template or any template
 * included using the <code>#parse</code> directive, can have properties files
 * attached, for which each key would become a variable in the Velocity
 * context.  The properties file is of the same name as the template file, 
 * with ".properties" for the extension.  The property file must have
 * key and values separated by an equal sign, one variable per line.  The 
 * key and value strings are taken literally, after trimming leading and
 * trailing spaces. 
 * In addition, any properties
 * file can also be specified when the creating a ConfigurationLoader.  
 * Refer to <a href="http://velocity.apache.org/engine/devel/user-guide.html">
 * Velocity User Guide</a> for template documentation. 
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public final class ConfigurationLoader {
    
    private final VelocityEngine velocityEngine;
    
    /**
     * Constructor.
     */
    public ConfigurationLoader() {
        this(new File("/"));
    }

    /**
     * Constructor.
     * @param templateRoot directory acting as the root for all relative paths 
     */
    public ConfigurationLoader(File templateRoot) {
        super();
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE,
                RelativeIncludeEventHandler.class.getName());
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
        velocityEngine.setProperty(
                RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "");
        velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
        	      "org.apache.velocity.runtime.log.Log4JLogChute");
        velocityEngine.setProperty("runtime.log", "");
        //        ve.setProperty("runtime.log.logsystem.log4j.logger",
        //                LOGGER_NAME);
    }

    /**
     * Constructor. 
     * @param velocityProperties custom properties for parsing Velocity files
     */
    public ConfigurationLoader(Properties velocityProperties) {
        try {
            velocityEngine = new VelocityEngine(velocityProperties);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Could not create parsing Velocity engine.", e);
        }
    }

    /**
     * Loads a configuration file.
     * @param configFile XML configuration file
     * @return Apache XMLConfiguration instance
     */
    public XMLConfiguration loadXML(File configFile) {
        return loadXML(configFile, null);
    }

    /**
     * Loads a configuration file.
     * @param configFile XML configuration file
     * @param variables path to properties file defining variables. 
     * @return Apache XMLConfiguration instance
     */
    public XMLConfiguration loadXML(File configFile, File variables) {
        if (!configFile.exists()) {
            return null;
        }
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        Reader reader = new StringReader(loadString(configFile, variables));
        try {
            config.load(reader);
            reader.close();
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load configuration file: \"" + configFile + "\". "
                  + "Probably a misconfiguration or the configuration XML "
                  + "is not well-formed.", e);
        }
        return config;
    }

    /**
     * Loads a configuration file as a string.
     * @param configFile configuration file
     * @param variables path to properties file defining variables. 
     * @return configuration as string
     */
    public String loadString(File configFile, File variables) {

        if (configFile == null) {
            throw new NullPointerException("No configuration file specified.");
        }
        if (!configFile.exists()) {
            return null;
        }

        VelocityContext context = new VelocityContext();

        // Load from explicitly referenced properties
        loadVariables(context, variables);

        // Load from properties matching config file name
        String file = configFile.getAbsolutePath();
        String fullpath = FilenameUtils.getFullPath(file);
        String baseName = FilenameUtils.getBaseName(file);
        
        File varsFile = getVariablesFile(fullpath, baseName);
        if (varsFile != null) {
            loadVariables(context, varsFile);
        }

        StringWriter sw = new StringWriter();
        try {
            FileReader reader = new FileReader(configFile);
            velocityEngine.evaluate(
                    context, sw, configFile.getAbsolutePath(), reader);
            reader.close();
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load config file as a string: " + file, e);
        }
        return sw.toString();
    }
    
    /**
     * This load method will return an Apache XML Configuration without
     * any variable substitution or Velocity directives. 
     * Velocity parsing or variable substitution.
     * @param in input stream
     * @return XMLConfiguration
     * @throws ConfigurationException 
     */
    public static XMLConfiguration loadXML(Reader in)
            throws ConfigurationException {
        XMLConfiguration xml = new XMLConfiguration();
        xml.setDelimiterParsingDisabled(true);
        try {
            xml.load(in);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Cannot load from XML", e);
        }
        return xml;
    }

    private File getVariablesFile(String fullpath, String baseName) {
        File vars = new File(fullpath + baseName + ".properties");
        if (vars.exists() && vars.isFile()) {
        	return vars;
        }
        return null;
    }
    
    private void loadVariables(VelocityContext context, File vars) {
        try {
            if (vars != null && vars.exists() && vars.isFile()) {
                FileInputStream is = new FileInputStream(vars);
				List<String> lines = IOUtils.readLines(is);
                is.close();
                for (String line : lines) {
					if (line.contains("=")) {
						String key = 
								StringUtils.substringBefore(line, "=").trim();
						String value = 
								StringUtils.substringAfter(line, "=").trim();
	                    context.put(key, value);
					}
				}
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Cannot load variables from file: " + vars, e);
        }
    }
}
