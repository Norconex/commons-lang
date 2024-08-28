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

import java.io.Serializable;

import lombok.Data;
import lombok.Getter;

/**
 * A configuration error resulting from validation.  A configuration error
 * may or may not prevent some parts of a configuration to be loaded.
 * The severity is only used as indicator.
 * @since 2.0.0
 * @see XML#validate(Class)
 */
//MAYBE: rename to ValidationError and maybe add source + generic as well? (XML)
@Data
@Getter
public class XMLValidationError implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Severity {
        WARNING, ERROR, FATAL
    }

    private final Severity severity;
    private final String message;

    public XMLValidationError(Severity severity, String message) {
        this.severity = severity;
        this.message = message;
    }
}
