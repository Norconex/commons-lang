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

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.XMLValidationError;
import com.norconex.commons.lang.xml.XMLValidationException;

/**
 * <p>Configuration file parser using Velocity template engine
 * (which can have parse/include directives) and using separate files for
 * defining Velocity variables.
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
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html#load-java.io.Reader-">
 * Java API documentation</a> for exact syntax and parsing logic.</p>
 *
 * <p>When both <code>.variables</code> and <code>.properties</code> exist
 * for a template, the <code>.properties</code> file variables take
 * precedence.</p>
 *
 * <p>Any <code>.variables</code> or <code>.properties</code> file
 * can also be specified using the {@link #setVariablesFile(Path)} method.
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
 * XML xml = new ConfigurationLoader().loadXML(
 *         Path.get("/path/to/myconfig.cfg"));</pre>
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
 * <a href="https://velocity.apache.org/engine/2.0/user-guide.html">
 * Velocity User Guide</a> for complete syntax and template documentation.
 * </p>
 * @author Pascal Essiembre
 */
public final class ConfigurationLoader {

    private static final String EXTENSION_PROPERTIES = ".properties";
    private static final String EXTENSION_VARIABLES = ".variables";

    private final VelocityEngine velocityEngine;
    private final VelocityContext defaultContext;

    private Path variablesFile;
    private boolean ignoreErrors;

    /**
     * Constructor.
     */
    public ConfigurationLoader() {
        super();
        defaultContext = createDefaultContext();
        velocityEngine = createVelocityEngine();
    }

    /**
     * Sets a variables file. See class documentation for details.
     * @param variablesFile variables file
     * @return this instance
     * @since 2.0.0
     */
    public ConfigurationLoader setVariablesFile(Path variablesFile) {
        this.variablesFile = variablesFile;
        return this;
    }

    /**
     * Sets whether to ignore validation errors when applicable.
     * When loading an XML file,
     * the default behavior will throw a {@link XMLValidationException}
     * upon encountering validation errors.
     * @param ignoreErrors <code>true</code> to ignore validation errors
     * @return this instance
     * @since 2.0.0
     */
    public ConfigurationLoader setIgnoreValidationErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
        return this;
    }

    /**
     * Loads an XML configuration file.
     * @param configFile XML configuration file
     * @return XML
     * @since 2.0.0
     */
    public XML loadXML(Path configFile) {
        if (!configFile.toFile().exists()) {
            return null;
        }
        try {
            String xml = loadString(configFile);
            // clean-up extra duplicate declaration tags due to template
            // includes/imports that could break parsing.
            // Keep first <?xml... tag only, and delete all <!DOCTYPE...
            // as they are not necessary to parse configs.
            xml = Pattern.compile("((?!^)<\\?xml.*?\\?>|<\\!DOCTYPE.*?>)")
                    .matcher(xml).replaceAll("");
            return new XML(xml);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load configuration file: \"" + configFile + "\". "
                  + "Probably a misconfiguration or the configuration XML "
                  + "is not well-formed.", e);
        }
    }

    /**
     * Loads an XML configuration file and populates a new object
     * represented by the given "class" attribute found on XML root element.
     * @param configFile XML configuration file
     * @return new object
     * @since 2.0.0
     */
    public <T> T loadFromXML(Path configFile) {
        return loadFromXML(configFile, null);
    }

    /**
     * Loads an XML configuration file and populates a new object
     * represented by the given class.
     * @param configFile XML configuration file
     * @param objClass type of object to create and populate
     * @return new object
     * @since 2.0.0
     */
    public <T> T loadFromXML(Path configFile, Class<T> objClass) {
        XML xml = loadXML(configFile);
        if (xml == null) {
            return null;
        }
        if (objClass == null) {
            return xml.toObject(ignoreErrors);
        }
        T obj;
        try {
            obj = objClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConfigurationException(
                    "This class could not be instantiated: " + objClass, e);
        }
        loadFromXML(configFile, obj);
        return obj;
    }
    /**
     * Loads an XML configuration file and populates a given object.
     * @param configFile XML configuration file
     * @param object object to populate
     * @since 2.0.0
     */
    public void loadFromXML(Path configFile, Object object) {
        Objects.requireNonNull("'object' must not be null.");
        XML xml = loadXML(configFile);
        if (xml != null) {
            List<XMLValidationError> errors = xml.populate(object);
            if (!ignoreErrors && !errors.isEmpty()) {
                throw new XMLValidationException(errors, xml);
            }
        }
    }

    /**
     * Loads a configuration file as a string.
     * @param configFile configuration file
     * @return configuration as string
     * @since 2.0.0
     */
    public String loadString(Path configFile) {
        if (configFile == null) {
            throw new ConfigurationException(
                    "No configuration file specified.");
        }
        if (!configFile.toFile().exists()) {
            return null;
        }

        VelocityContext context = new VelocityContext(defaultContext);

        // Load from explicitly referenced properties
        loadVariables(context, variablesFile);

        // Load from properties matching config file name
        String file = configFile.toAbsolutePath().toString();
        String fullpath = FilenameUtils.getFullPath(file);
        String baseName = FilenameUtils.getBaseName(file);

        Path varsFile = getVariablesFile(fullpath, baseName);
        if (varsFile != null) {
            loadVariables(context, varsFile);
        }

        StringWriter sw = new StringWriter();
        try (Reader reader = Files.newBufferedReader(configFile)) {
            velocityEngine.evaluate(
                    context, sw, configFile.toString(), reader);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load config file as a string: " + file, e);
        }
        return sw.toString();
    }

    //--- Protected methods ----------------------------------------------------

    // @since 2.0.0
    protected VelocityContext createDefaultContext() {
        return null;
    }

    // @since 2.0.0
    protected VelocityEngine createVelocityEngine() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE,
                RelativeIncludeEventHandler.class.getName());
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
        velocityEngine.setProperty(
                RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "");
        velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING,
                StandardCharsets.UTF_8.toString());
        velocityEngine.setProperty(RuntimeConstants.ENCODING_DEFAULT,
                StandardCharsets.UTF_8.toString());
        velocityEngine.setProperty("runtime.log", "");
        return velocityEngine;
    }

    //--- Private methods ------------------------------------------------------

    private Path getVariablesFile(String fullpath, String baseName) {
        Path vars = Paths.get(fullpath + baseName + EXTENSION_PROPERTIES);
        if (isVariableFile(vars, EXTENSION_PROPERTIES)) {
        	return vars;
        }
        vars = Paths.get(fullpath + baseName + EXTENSION_VARIABLES);
        if (isVariableFile(vars, EXTENSION_VARIABLES)) {
            return vars;
        }
        return null;
    }

    private void loadVariables(VelocityContext context, Path vars) {
        try {
            if (isVariableFile(vars, EXTENSION_VARIABLES)) {
                for (String line : Files.readAllLines(vars)) {
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

                try (Reader r = Files.newBufferedReader(vars)) {
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

    private boolean isVariableFile(Path vars,  String extension) {
        return vars != null && vars.toFile().exists()
                && vars.toFile().isFile()
                && vars.toString().endsWith(extension);
    }
}
