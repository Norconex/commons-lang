package com.norconex.commons.lang.config.vlt;

import com.norconex.commons.lang.SystemUtil;
import com.norconex.commons.lang.config.ConfigurationException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.velocity.app.VelocityEngine;

public class CustomIncludeDirectiveTest {


    private static final String CFG_BASE_PATH = "src/test/resources/config/";

    private ConfigurationLoader configLoader;

    private VelocityEngine velocityEngine;

    @BeforeEach
    void beforeEach() {
        configLoader = ConfigurationLoader.builder().build();
    }

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
        String template = "#include($nullArg)";

        // Set up Velocity context
        VelocityContext context = new VelocityContext();
        context.put("nullArg", null);

        // Render the template
        StringWriter writer = new StringWriter();
        velocityEngine.evaluate(context, writer, "test", template);

        // Verify that the output is as expected (empty output or error handling depending on implementation)
        String output = writer.toString();
        assertEquals("", output, "null error with arg 0 please see log. null");
    }


    @Test
    public void testMissingParseError() throws Exception
    {
        var loader = ConfigurationLoader.builder().build();

        var includeError0 = cfgPath("vlt/missingparse.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));
    }

    @Test
    public void testMissingIncludeError() throws Exception
    {
        var loader = ConfigurationLoader.builder().build();

        var includeError0 = cfgPath("vlt/missinginclude.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));

    }

    @Test
    public void testParseError() throws Exception
    {

        var loader = ConfigurationLoader.builder().build();

        var includeError0 = cfgPath("vlt/parsemain.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));

    }

    @Test
    public void testParseError2() throws Exception
    {

        var loader = ConfigurationLoader.builder().build();
        var includeError0 = cfgPath("vlt/parsemain2.vm");
        assertThrows(ConfigurationException.class,
                () -> loader.toString(includeError0));

    }

    @Test
    void testErrorWithArg() throws Exception{
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
//
//    @Test
//    void testIndentToString() throws Exception {
//
//        var loader = ConfigurationLoader.builder()
//                .variablesFile(cfgPath("vlt_item7.vm"))
//                .build();
//        var str = SystemUtil.callWithProperty("date", "2024-08-20",
//                () -> loader.toString(cfgPath("vlt_indent.yaml")));
//        // "varB" should not be resolved as it comes from an #include
//        // directive (as opposed to parse)
//        assertThat(StringUtils.remove(str, '\r')).isEqualTo(
//                """
//                        title: $title
//                        date: $date
//                        title: template title
//                        date: 2024-08-20
//                        depth1_include_vlt_item2:
//                          title: $title
//                          date: $date
//                          key: value
//                          depth_vlt_item2:
//                            title: $title
//                            date: $date
//                        depth1_parse_vlt_item2:
//                          key: value
//                          depth_vlt_item2:
//                            title: template title
//                            date: 2024-08-20
//                        multi_depth_vlt_item2:
//                          multi_depth_vlt_item2_1:
//                            key: value
//                            depth_vlt_item2:
//                              title: $title
//                              date: $date
//                            key: value
//                            depth_vlt_item2:
//                              title: template title
//                              date: 2024-08-20
//                        recursive_depth_vlt_item3:
//                          depth_vlt_item3:
//                            key: value
//                            title: $title
//                            date: $date
//                            title: template title
//                            date: 2024-08-20
//                            depth_vlt_item3_1:
//                              tst_depth_3_1_1:
//                                key: value
//                                title: $title
//                                date: $date
//                                key: value
//                              tst_depth_3_1_2:
//                                title: template title
//                                date: 2024-08-20
//                                key: value
//                                depth_vlt_item2:
//                                  title: template title
//                                  date: 2024-08-20
//                                tst_depth_3_1_2-1
//                                  key: value
//                                  key: value
//                        ifelse_loop_depth_vlt_item6:
//                          Name: Alice
//                          Age: 10
//                          age: you are nothing.
//                          Name: Bob
//                          Age: 30
//                          Feels: very old
//                          Name: Charlie
//                          Age: 50
//                          Feels: very old
//                            """);
//    }

    private Path cfgPath(String path) {
        return Path.of(CFG_BASE_PATH + path);
    }

}
