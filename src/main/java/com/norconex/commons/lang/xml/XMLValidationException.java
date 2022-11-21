/* Copyright 2018-2022 Norconex Inc.
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

import org.apache.commons.collections4.CollectionUtils;

/**
 * Runtime exception for configuration related issues.
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see XML#validate(Class)
 */
public class XMLValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<XMLValidationError> errors = new ArrayList<>();
    private final transient XML xml;

    /**
     * Constructor.
     * @param errors configuration errors (e.g. schema validation errors)
     */
    public XMLValidationException(List<XMLValidationError> errors) {
        this(errors, null);
    }

    /**
     * Constructor.
     * @param errors configuration errors (e.g. schema validation errors)
     * @param xml the XML containing validation errors
     */
    public XMLValidationException(
            List<XMLValidationError> errors, XML xml) {
        super(CollectionUtils.isEmpty(errors)
                ? "Unspecified validation errors."
                : errors.size() + " validation error(s). First one: "
                        + errors.get(0).getMessage());
        if (!CollectionUtils.isEmpty(errors)) {
            this.errors.addAll(errors);
        }
        this.xml = xml;
    }

    public List<XMLValidationError> getErrors() {
        return errors;
    }
    public XML getXml() {
        return xml;
    }
}
