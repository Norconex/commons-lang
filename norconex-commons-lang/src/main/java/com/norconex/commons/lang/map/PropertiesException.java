/* Copyright 2010-2015 Norconex Inc.
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
package com.norconex.commons.lang.map;

/**
 * <code>Properties</code> exception.  Typically thrown when 
 * setting/getting invalid property values.
 * @author Pascal Essiembre
 * @see Properties
 */
public class PropertiesException extends RuntimeException {

    private static final long serialVersionUID = 3040976896770771979L;

    public PropertiesException(final String msg) {
        super(msg);
    }
    public PropertiesException(final Throwable cause) {
        super(cause);
    }
    public PropertiesException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
