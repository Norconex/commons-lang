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
 * <p>Class parsing a Velocity template (which can have parse/include 
 * directives)
 * and uses a properties file for Velocity variables.
 * Templates, whether the main template or any template
 * included using the <code>#parse</code> directive, can have variable files
 * attached, for which each key would become a variable in the Velocity
 * context.  A variable file must be of the same name as the template file, 
 * with one of two possible extensions: 
 * <code>.variables</code> or <code>.properties</code>.</p>
 * 
 * <p>A <code>.variables</code> file must have
 * keys and values separated by an equal sign, one variable per line.  The 
 * key and value strings are taken literally, after trimming leading and
 * trailing spaces.</p> 
 * 
 * <p>A <code>.properties</code> file stores key/value in the way the Java
 * programming language expects it for any <code>.properties</code> file. 
 * It is essentially the same, but has more options (e.g. multi-line support)
 * and gotchas (e.g. must escape certain characters). Please
 * refer to the corresponding 
 * <a href="http://docs.oracle.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)">
 * Java API documentation</a> for exact syntax and parsing logic.</p> 
 * 
 * <p>When both <code>.variables</code> and <code>.properties</code> exist
 * for a template, the <code>.properties</code> file takes precedence.</p>
 * 
 * <p>Any <code>.variables</code> or <code>.properties</code> file 
 * can also be specified using the {@link #loadXML(File, File)} method.
 * <p>
 * Refer to <a href="http://velocity.apache.org/engine/devel/user-guide.html">
 * Velocity User Guide</a> for template documentation. 
 * </p>
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public final class ConfigurationLoader {
    
    private static final String EXTENSION_PROPERTIES = ".properties";
    private static final String EXTENSION_VARIABLES = ".variables";
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
     * @param variables path to .variables or .properties file defining 
     *        variables. 
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
     * @param variables path to .variables or .properties file defining 
     *        variables. 
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
        File vars = new File(fullpath + baseName + EXTENSION_PROPERTIES);
        if (isVariableFile(vars, EXTENSION_PROPERTIES)) {
        	return vars;
        }
        vars = new File(fullpath + baseName + EXTENSION_VARIABLES);
        if (isVariableFile(vars, EXTENSION_VARIABLES)) {
            return vars;
        }
        return null;
    }
    
    private void loadVariables(VelocityContext context, File vars) {
        try {
            if (isVariableFile(vars, EXTENSION_VARIABLES)) {
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
            } else if (isVariableFile(vars, EXTENSION_PROPERTIES)) {
                Reader r = new FileReader(vars);
                Properties props = new Properties();
                props.load(r);
                r.close();
                for (String key : props.stringPropertyNames()) {
                    context.put(key, props.getProperty(key));
                }
            } else {
                throw new ConfigurationException(
                        "Variable files must have \".variables\" or "
                      + "\".properties.\" extension: " + vars);
            }

        } catch (IOException e) {
            throw new ConfigurationException(
                    "Cannot load variables from file: " + vars, e);
        }
    }
    
    private boolean isVariableFile(File vars,  String extension) {
        return vars != null && vars.exists() && vars.isFile() 
                && vars.getName().endsWith(extension);
    }
}
