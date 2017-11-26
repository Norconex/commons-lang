/* Copyright 2010-2017 Norconex Inc.
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

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
 *    &lt;host&gt;$host&lt;/host&gt;
 *    &lt;port&gt;$port&lt;/port&gt;
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
        try {
            String xml = loadString(configFile, variables);
            // clean-up extra duplicate declaration tags due to template
            // includes/imports that could break parsing.
            // Keep first <?xml... tag only, and delete all <!DOCTYPE...
            // as they are not necessary to parse configs.
            xml = Pattern.compile("((?!^)<\\?xml.*?\\?>|<\\!DOCTYPE.*?>)",
                    Pattern.MULTILINE).matcher(xml).replaceAll("");
            return XMLConfigurationUtil.newXMLConfiguration(
                    new StringReader(xml));
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load configuration file: \"" + configFile + "\". "
                  + "Probably a misconfiguration or the configuration XML "
                  + "is not well-formed.", e);
        }
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
        try (Reader reader = new InputStreamReader(
                new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            velocityEngine.evaluate(
                    context, sw, configFile.getAbsolutePath(), reader);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load config file as a string: " + file, e);
        }
        return sw.toString();
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
				List<String> lines = 
				        IOUtils.readLines(is, StandardCharsets.UTF_8);
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
                Properties props = new Properties();
                try (Reader r = new InputStreamReader(
                        new FileInputStream(vars), StandardCharsets.UTF_8)) {
                    props.load(r);
                }
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
