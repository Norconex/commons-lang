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
 * @since 2.0.0
 * @see Xml#validate(Class)
 */
public class XmlValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<XmlValidationError> errors = new ArrayList<>();
    private final transient Xml xml;

    /**
     * Constructor.
     * @param errors configuration errors (e.g. schema validation errors)
     */
    public XmlValidationException(List<XmlValidationError> errors) {
        this(errors, null);
    }

    /**
     * Constructor.
     * @param errors configuration errors (e.g. schema validation errors)
     * @param xml the XML containing validation errors
     */
    public XmlValidationException(
            List<XmlValidationError> errors, Xml xml) {
        super(CollectionUtils.isEmpty(errors)
                ? "Unspecified validation errors."
                : errors.size() + " validation error(s). First one: "
                        + errors.get(0).getMessage());
        if (!CollectionUtils.isEmpty(errors)) {
            this.errors.addAll(errors);
        }
        this.xml = xml;
    }

    public List<XmlValidationError> getErrors() {
        return errors;
    }

    public Xml getXml() {
        return xml;
    }
}
