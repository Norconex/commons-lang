/* Copyright 2017 Norconex Inc.
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
import org.xml.sax.SAXParseException;

/**
 * SAX {@link ErrorHandler} which stores SAX Exceptions that can later 
 * be retrieved as {@link List}.  Does not throw exceptions.
 * @author Pascal Essiembre
 * @since 1.13.0
 */
public class ListErrorHandler implements ErrorHandler {

    private final List<SAXParseException> errors = new ArrayList<>();
    private final List<SAXParseException> warnings = new ArrayList<>();
    private final List<SAXParseException> fatalErrors = new ArrayList<>();
    
    @Override
    public void error(SAXParseException exception) {
        errors.add(exception);
    }
    @Override
    public void fatalError(SAXParseException exception) {
        fatalErrors.add(exception);
    }
    @Override
    public void warning(SAXParseException exception) {
        warnings.add(exception);
    }
    
    public List<SAXParseException> getErrors() {
        return errors;
    }
    public List<String> getErrorMessages() {
        return getMessages(errors);
    }
    public List<SAXParseException> getFatalErrors() {
        return fatalErrors;
    }
    public List<String> getFatalErrorMessages() {
        return getMessages(fatalErrors);
    }
    public List<SAXParseException> getWarnings() {
        return warnings;
    }
    public List<String> getWarningMessages() {
        return getMessages(warnings);
    }
    public List<SAXParseException> getAll() {
        List<SAXParseException> all = new ArrayList<>();
        all.addAll(fatalErrors);
        all.addAll(errors);
        all.addAll(warnings);
        return all;
    }
    public List<String> getAllMessages() {
        return getMessages(getAll());
    }
    public boolean isEmpty() {
        return errors.isEmpty() && fatalErrors.isEmpty() && warnings.isEmpty();
    }
    public int size() {
        return errors.size() + fatalErrors.size() + warnings.size();
    }
    
    private List<String> getMessages(List<SAXParseException> exceptions) {
        List<String> msgs = new ArrayList<>();
        for (SAXParseException e : exceptions) {
            msgs.add(e.getLocalizedMessage());
        }
        return msgs;
    }
}
