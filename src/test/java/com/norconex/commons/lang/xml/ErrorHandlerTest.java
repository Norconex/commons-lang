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
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

class ErrorHandlerTest {
    @ParameterizedTest
    @ArgumentsSource(ArgsProvider.class)
    void testErrorHandlers(ErrorHandler eh, Asserter asserter) { //NOSONAR
        SAXParseException e = new SAXParseException("Dummy exception", null);
        asserter.accept(() -> {
            eh.warning(e);
            return eh;
        });
        asserter.accept(() -> {
            eh.error(e);
            return eh;
        });
        asserter.accept(() -> {
            eh.fatalError(e);
            return eh;
        });
    }

    static class ArgsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(
                ExtensionContext context) throws Exception {
            return Stream.of(
                    of(new ErrorHandlerCapturer(), (Asserter) t -> {
                        try {
                            assertThat(((ErrorHandlerCapturer) t.call())
                                    .getErrors().size()).isGreaterThan(0);
                        } catch (Exception e) {
                        }
                    }),
                    of(new ErrorHandlerLogger(ErrorHandlerTest.class),
                            (Asserter) t -> {
                                assertThatNoException()
                                        .isThrownBy(() -> t.call());
                            }),
                    of(new ErrorHandlerFailer(ErrorHandlerTest.class),
                            (Asserter) t -> {
                                assertThatException()
                                        .isThrownBy(() -> t.call());
                            }));
        }
    }

    interface Asserter extends Consumer<Callable<ErrorHandler>> {
    }
}
