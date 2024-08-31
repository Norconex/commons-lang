/* Copyright 2019 Norconex Inc.
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

import java.util.Arrays;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.norconex.commons.lang.xml.XmlValidationError.Severity;

/**
 * Fails upon encountering first XML validation errors by throwing an
 * {@link XmlValidationException}.
 * as error.
 * @since 2.0.0
 */
public class ErrorHandlerFailer implements ErrorHandler {
    private final Class<?> clazz;

    public ErrorHandlerFailer(Class<?> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        fail(e, Severity.WARNING);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        fail(e, Severity.ERROR);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        fail(e, Severity.FATAL);
    }

    private void fail(SAXParseException e, Severity severity) {
        String msg = "[XML] ";
        if (clazz == null) {
            msg += e.getMessage();
        } else {
            msg += clazz.getSimpleName() + ": " + e.getMessage();
        }
        throw new XmlValidationException(
                Arrays.asList(new XmlValidationError(severity, msg)));
    }
}
