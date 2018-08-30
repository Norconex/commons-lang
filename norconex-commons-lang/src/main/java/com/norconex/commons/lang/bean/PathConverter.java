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
package com.norconex.commons.lang.bean;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link Path} converter.
 * @since 2.0.0
 * @see ExtendedBeanUtilsBean
 */
public class PathConverter extends AbstractConverter {

    /**
     * Construct a {@link Path} converter that throws
     * a <code>ConversionException</code> if an error occurs.
     */
    public PathConverter() {
        super();
    }

    /**
     * Construct a {@link Path} converter that returns
     * a default value if an error occurs.
     *
     * @param defaultValue The default value to be returned
     * if the value to be converted is missing or an error
     * occurs converting the value.
     */
    public PathConverter(final Object defaultValue) {
        super(defaultValue);
    }

    /**
     * Return the default type this <code>Converter</code> handles.
     * @return The default type this <code>Converter</code> handles.
     */
    @Override
    protected Class<?> getDefaultType() {
        return File.class;
    }

    /**
     * <p>Convert the input object into a {@link Path}.</p>
     * @param <T> The target type of the conversion.
     * @param type Data type to which this value should be converted.
     * @param value The input value to be converted.
     * @return The converted value.
     * @throws Throwable if an error occurs converting to the specified type
     */
    @Override
    protected <T> T convertToType(final Class<T> type, final Object value)
            throws Throwable {
        if (Path.class.equals(type)) {
            return type.cast(Paths.get(value.toString()));
        }
        throw conversionException(type, value);
    }
}
