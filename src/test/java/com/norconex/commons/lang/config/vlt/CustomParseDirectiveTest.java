package com.norconex.commons.lang.config.vlt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.norconex.commons.lang.SystemUtil;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.vlt.CustomParseDirective;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;
import java.io.Writer;
import java.nio.file.Path;

public class CustomParseDirectiveTest {

    private static final String CFG_BASE_PATH = "src/test/resources/config/";

    private ConfigurationLoader configLoader;

    @BeforeEach
    void beforeEach() {
        configLoader = ConfigurationLoader.builder().build();
    }

    @Test
    void testIndentToString() throws Exception {

        var loader = ConfigurationLoader.builder()
                .variablesFile(cfgPath("vlt_item7.vm"))
                .build();
        var str = SystemUtil.callWithProperty("date", "2024-08-20",
                () -> loader.toString(cfgPath("vlt_indent.yaml")));
        // "varB" should not be resolved as it comes from an #include
        // directive (as opposed to parse)
        assertThat(StringUtils.remove(str, '\r')).isEqualTo(
                """
                        title: $title
                        date: $date
                        title: template title
                        date: 2024-08-20
                        depth1_include_vlt_item2:
                          title: $title
                          date: $date
                          key: value
                          depth_vlt_item2:
                            title: $title
                            date: $date
                        depth1_parse_vlt_item2:
                          key: value
                          depth_vlt_item2:
                            title: template title
                            date: 2024-08-20
                        multi_depth_vlt_item2:
                          multi_depth_vlt_item2_1:
                            key: value
                            depth_vlt_item2:
                              title: $title
                              date: $date
                            key: value
                            depth_vlt_item2:
                              title: template title
                              date: 2024-08-20
                        recursive_depth_vlt_item3:
                          depth_vlt_item3:
                            key: value
                            title: $title
                            date: $date
                            title: template title
                            date: 2024-08-20
                            depth_vlt_item3_1:
                              tst_depth_3_1_1:
                                key: value
                                title: $title
                                date: $date
                                key: value
                              tst_depth_3_1_2:
                                title: template title
                                date: 2024-08-20
                                key: value
                                depth_vlt_item2:
                                  title: template title
                                  date: 2024-08-20
                                tst_depth_3_1_2-1
                                  key: value
                                  key: value
                        ifelse_loop_depth_vlt_item6:
                          Name: Alice
                          Age: 10
                          age: you are nothing.
                          Name: Bob
                          Age: 30
                          Feels: very old
                          Name: Charlie
                          Age: 50
                          Feels: very old
                            """);
    }

    private Path cfgPath(String path) {
        return Path.of(CFG_BASE_PATH + path);
    }

    //
    //    private CustomParseDirective customParseDirective;
    //    private InternalContextAdapter mockContext;
    //    private Node mockNode;
    //    private Writer mockWriter;
    //
    //    @BeforeEach
    //    public void setUp() {
    //        customParseDirective = new CustomParseDirective();
    //        mockContext = mock(InternalContextAdapter.class);
    //        mockNode = mock(Node.class);
    //        mockWriter = mock(Writer.class);
    //    }
    //
    //    @Test
    //    public void testGetName() {
    //        // Test the name of the directive
    //        assertEquals("parse", customParseDirective.getName());
    //    }
    //
    //    @Test
    //    public void testGetType() {
    //        // Test the type of directive (BLOCK)
    //        assertEquals(CustomParseDirective.BLOCK,
    //                customParseDirective.getType());
    //    }
    //
    //    @Test
    //    public void testRenderWithValidNode() throws Exception {
    //        // Setup: mock a valid child node that returns a file name
    //        Node childNode = mock(Node.class);
    //        when(mockNode.jjtGetChild(0)).thenReturn(childNode);
    //        when(childNode.value(mockContext)).thenReturn("validFile.vm");
    //
    //        // Act: render the directive
    //        boolean result =
    //                customParseDirective.render(mockContext, mockWriter, mockNode);
    //
    //        // Assert: rendering should succeed and return true
    //        assertTrue(result);
    //    }
    //
    //    @Test
    //    public void testRenderWithNullNode() throws Exception {
    //        // Setup: no child nodes (simulating an error scenario)
    //        when(mockNode.jjtGetChild(0)).thenReturn(null);
    //
    //        // Act and Assert: rendering should fail due to missing file
    //        Exception exception = assertThrows(RuntimeException.class, () -> {
    //            customParseDirective.render(mockContext, mockWriter, mockNode);
    //        });
    //
    //        assertTrue(exception.getMessage().contains("No file specified"));
    //    }
    //
    //    @Test
    //    public void testRenderWithInvalidFile() throws Exception {
    //        // Setup: mock child node that returns an invalid file name
    //        Node childNode = mock(Node.class);
    //        when(mockNode.jjtGetChild(0)).thenReturn(childNode);
    //        when(childNode.value(mockContext)).thenReturn("invalidFile.vm");
    //
    //        // Act and Assert: rendering should throw an exception for invalid file
    //        Exception exception = assertThrows(RuntimeException.class, () -> {
    //            customParseDirective.render(mockContext, mockWriter, mockNode);
    //        });
    //
    //        assertTrue(exception.getMessage().contains("Unable to parse"));
    //    }
    //
    //    @Test
    //    public void testRenderWithIOException() throws Exception {
    //        // Setup: mock an IOException scenario
    //        Node childNode = mock(Node.class);
    //        when(mockNode.jjtGetChild(0)).thenReturn(childNode);
    //        when(childNode.value(mockContext)).thenReturn("validFile.vm");
    //        doThrow(new RuntimeException("IO Error")).when(mockWriter).flush();
    //
    //        // Act and Assert: rendering should fail with an IO exception
    //        Exception exception = assertThrows(RuntimeException.class, () -> {
    //            customParseDirective.render(mockContext, mockWriter, mockNode);
    //        });
    //
    //        assertTrue(exception.getMessage().contains("IO Error"));
    //    }
    //
    //    @Test
    //    public void testRenderWithEmptyFile() throws Exception {
    //        // Setup: child node returns an empty string as the file name
    //        Node childNode = mock(Node.class);
    //        when(mockNode.jjtGetChild(0)).thenReturn(childNode);
    //        when(childNode.value(mockContext)).thenReturn("");
    //
    //        // Act and Assert: rendering should fail due to empty file name
    //        Exception exception = assertThrows(RuntimeException.class, () -> {
    //            customParseDirective.render(mockContext, mockWriter, mockNode);
    //        });
    //
    //        assertTrue(exception.getMessage().contains("No file specified"));
    //    }
    //
    //    @Test
    //    public void testRenderWithMultipleNodes() throws Exception {
    //        // Setup: simulate multiple nodes, rendering the first
    //        Node firstChildNode = mock(Node.class);
    //        Node secondChildNode = mock(Node.class);
    //
    //        when(mockNode.jjtGetChild(0)).thenReturn(firstChildNode);
    //        when(firstChildNode.value(mockContext)).thenReturn("validFile.vm");
    //
    //        when(mockNode.jjtGetChild(1)).thenReturn(secondChildNode);
    //        when(secondChildNode.value(mockContext)).thenReturn("anotherFile.vm");
    //
    //        // Act: render the directive
    //        boolean result =
    //                customParseDirective.render(mockContext, mockWriter, mockNode);
    //
    //        // Assert: rendering should succeed with the first node
    //        assertTrue(result);
    //    }
    //
    //    @Test
    //    public void testRenderWithInvalidNodeType() throws Exception {
    //        // Setup: node returns an unexpected object instead of a file name
    //        Node childNode = mock(Node.class);
    //        when(mockNode.jjtGetChild(0)).thenReturn(childNode);
    //        when(childNode.value(mockContext)).thenReturn(123); // Invalid file type (Integer)
    //
    //        // Act and Assert: rendering should fail due to invalid node value type
    //        Exception exception = assertThrows(RuntimeException.class, () -> {
    //            customParseDirective.render(mockContext, mockWriter, mockNode);
    //        });
    //
    //        assertTrue(
    //                exception.getMessage().contains("Invalid type for file name"));
    //    }
}
