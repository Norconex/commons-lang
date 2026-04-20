package com.norconex.commons.lang.config.vlt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.SystemUtil;
import com.norconex.commons.lang.config.ConfigurationLoader;

class CustomParseDirectiveTest {

    private static final String CFG_BASE_PATH = "src/test/resources/config/";

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

}
