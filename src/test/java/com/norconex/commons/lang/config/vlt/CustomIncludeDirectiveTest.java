package com.norconex.commons.lang.config.vlt;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringWriter;
import java.nio.file.Path;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;

class CustomIncludeDirectiveTest {

    private static final String CFG_BASE_PATH = "src/test/resources/config/";

    private VelocityEngine velocityEngine;

    @BeforeEach
    void setUp() {
        // Initialize Velocity engine
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("runtime.custom_directives",
                "com.norconex.commons.lang.config.vlt.CustomIncludeDirective,"
                        + "com.norconex.commons.lang.config.vlt.CustomParseDirective");

        velocityEngine.init();
    }

    @Test
    void testIncludeWithNullArgument() throws Exception {

        //test 40-42
        // Prepare the template with the #include directive and a null argument
        var template = "#include($nullArg)";

        // Set up Velocity context
        var context = new VelocityContext();
        context.put("nullArg", null);

        // Render the template
        var writer = new StringWriter();
        velocityEngine.evaluate(context, writer, "test", template);

        // Verify that the output is as expected (empty output or error handling depending on implementation)
        var output = writer.toString();
        assertEquals("", output, "null error with arg 0 please see log. null");
    }

    @Test
    void testMissingParseError() throws Exception {
        var loader = ConfigurationLoader.builder().build();

        var includeError0 = cfgPath("vlt/missingparse.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));
    }

    @Test
    void testMissingIncludeError() throws Exception {
        var loader = ConfigurationLoader.builder().build();

        var includeError0 = cfgPath("vlt/missinginclude.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));

    }

    @Test
    void testParseError() throws Exception {

        var loader = ConfigurationLoader.builder().build();

        var includeError0 = cfgPath("vlt/parsemain.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));

    }

    @Test
    void testParseError2() throws Exception {

        var loader = ConfigurationLoader.builder().build();
        var includeError0 = cfgPath("vlt/parsemain2.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));

    }

    @Test
    void testErrorWithArg() throws Exception {
        //        var includeError = cfgPath("vlt/include_error.vm");

        var loader = ConfigurationLoader.builder()
                //                .variablesFile(cfgPath("vlt/include_error.vm"))
                .build();

        var includeError1 = cfgPath("vlt/include_error.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError1));

        var includeError2 = cfgPath("vlt/include_error_empty.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError2));

    }

    private Path cfgPath(String path) {
        return Path.of(CFG_BASE_PATH + path);
    }

}
