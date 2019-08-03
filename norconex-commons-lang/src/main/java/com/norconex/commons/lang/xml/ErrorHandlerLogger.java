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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Logs XML validation warnings and errors. XML "fatal" errors are logged
 * as error.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class ErrorHandlerLogger implements ErrorHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(ErrorHandlerLogger.class);

    private final Class<?> clazz;
    public ErrorHandlerLogger(Class<?> clazz) {
        super();
        this.clazz = clazz;
    }
    @Override
    public void warning(SAXParseException e) throws SAXException {
        LOG.warn(msg(e));
    }
    @Override
    public void error(SAXParseException e) throws SAXException {
        LOG.error(msg(e));
    }
    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        LOG.error(msg(e));
    }
    private String msg(SAXParseException e) {
        if (clazz == null) {
            return "[XML] " + e.getMessage();
        }
        return "[XML] " + clazz.getSimpleName() + ": " + e.getMessage();
    }
}
