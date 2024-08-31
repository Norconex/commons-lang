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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.norconex.commons.lang.xml.XmlValidationError.Severity;

/**
 * Collects XML validation errors.
 * @since 2.0.0
 */
public class ErrorHandlerCapturer implements ErrorHandler {
    private final Class<?> clazz;
    private final List<XmlValidationError> errors;

    public ErrorHandlerCapturer() {
        this(null, null);
    }

    public ErrorHandlerCapturer(List<XmlValidationError> errors) {
        this(null, errors);
    }

    public ErrorHandlerCapturer(Class<?> clazz) {
        this(clazz, null);
    }

    public ErrorHandlerCapturer(
            Class<?> clazz, List<XmlValidationError> errors) {
        super();
        this.clazz = clazz;
        this.errors = errors == null ? new ArrayList<>() : errors;
    }

    public List<XmlValidationError> getErrors() {
        return errors;
    }

    public int clear() {
        int size = errors.size();
        errors.clear();
        return size;
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        errors.add(new XmlValidationError(Severity.WARNING, msg(e)));
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        errors.add(new XmlValidationError(Severity.ERROR, msg(e)));
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        errors.add(new XmlValidationError(Severity.FATAL, msg(e)));
    }

    private String msg(SAXParseException e) {
        if (clazz == null) {
            return "[XML] " + e.getMessage();
        }
        return "[XML] " + clazz.getSimpleName() + ": " + e.getMessage();
    }
}
