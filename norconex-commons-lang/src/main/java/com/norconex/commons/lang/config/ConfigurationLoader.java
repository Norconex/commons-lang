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
 * directives) and using separate files for defining Velocity variables.
 * </p>
 * <h3>Variables</h3>
 * <p>
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
 * </p>
 * 
 * <h3>Configuration fragments</h3>
 * <p>
 * To include configuration fragments and favor reuse, use the 
 * <code>#include("myfile.cg")</code> or <code>#parse("myfile.cg")</code> 
 * directives.  An include directive will include the referenced file
 * as-is, without interpretation.  A parse directive will treat the included
 * file as a Velocity file and will interpret it (along with its variable
 * file if any exists -- see above).
 * </p>
 * <p> 
 * The included/parsed files are relative to the parent template, or, can be
 * absolute paths on the host where the configuration loader is executed.
 * Example (both Windows and UNIX path styles are supported equally):
 * </p>
 * <p><i>Sample directory structure:</i></p>
 * <pre>
 * c:\sample\
 *     myapp\
 *         runme.jar
 *         configs\
 *              myconfig.cfg
 *              myconfig.properties
 *     shared\
 *         sharedconfig.cfg
 *         sharedconfig.variables</pre>
 * <p><i>Configuration file myconfig.cfg:</i></p>
 * <pre>
 * &lt;myconfig&gt;
 *    &lt;host>$host&lt;/host&gt;
 *    &lt;port>$port&lt;/port&gt;
 *    #parse("../../shared/sharedconfig.cfg")
 * &lt;/myconfig&gt;</pre>
 * <p><i>Configuration loading:</i></p>
 * <pre>
 * XMLConfiguration xml = ConfigurationLoader.loadXML(
 *         new File("C:\\sample\\myapp\\myconfig.cfg"));</pre>
 * <p><i>Explanation:</i></p>
 * <p>
 * When loading myconfig.cfg, the variables defined in myconfig.properties
 * are automatically loaded and will replace the $host and $port variables.
 * The myconfig.cfg file is also parsing a shared configuration file: 
 * sharedconfig.cfg.  That file will be parsed and inserted,
 * with its variables defined in sharedconfig.variables automatically
 * loaded and resolved. 
 * </p>
 * <p>
 * Other Velocity directives are supported 
 * (if-else statements, foreach loops, macros,
 * etc).  Refer to 
 * <a href="http://velocity.apache.org/engine/devel/user-guide.html">
 * Velocity User Guide</a> for complete syntax and template documentation. 
 * </p>
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
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
            throw new ConfigurationException(
                    "No configuration file specified.");
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
     */
    public static XMLConfiguration loadXML(Reader in) {
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
