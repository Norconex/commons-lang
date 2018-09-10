/* Copyright 2018 Norconex Inc.
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
package com.norconex.commons.lang.convert;

/**
 * Runtime exception indicating a conversion error.
 * @author Pascal Essiembre
 * @see Converter
 * @since 2.0.0
 */
public class ConverterException extends RuntimeException {

    private static final long serialVersionUID = -6879301747242838385L;

    public ConverterException() {
        super();
    }
    public ConverterException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    public ConverterException(String message, Throwable cause) {
        super(message, cause);
    }
    public ConverterException(String message) {
        super(message);
    }
    public ConverterException(Throwable cause) {
        super(cause);
    }
}
