/* Copyright 2022 Norconex Inc.
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
package com.norconex.commons.lang.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.net.ProxySettings;

class ClasspathResourceResolverTest {

    @Test
    void testClasspathResourceResolver() throws IOException {
        assertThatNoException().isThrownBy(
                ClasspathResourceResolver::new);
        var crr =
                new ClasspathResourceResolver(ProxySettings.class);
        var input = crr.resolveResource(
                "http://www.w3.org/2001/XMLSchema",
                null,
                null,
                "MockHost.xsd",
                "file:///com/norconex/commons/lang/xml/mock/MockProxySettings.xsd");

        assertThat(input).isNotNull();
    }

}
