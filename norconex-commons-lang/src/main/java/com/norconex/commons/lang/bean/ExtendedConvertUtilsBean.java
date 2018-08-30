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

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.DateConverter;

/**
 * <p>
 * Extends {@link ConvertUtilsBean} to support mapping of additional types as
 * well as collections and arrays.  Additional types:
 * </p>
 * <ul>
 *   <li>{@link EnumConverter}</li>
 *   <li>{@link LocalDateTimeConverter}</li>
 *   <li>{@link LocaleConverter}</li>
 *   <li>{@link PathConverter}</li>
 * </ul>
 * <p>
 * Also, {@link DateConverter} is replaced with {@link EpochDateConverter}
 * which converts dates to their EPOCH values instead.
 * </p>
 * <p>
 * By default {@link ConversionException} is thrown when conversion fails.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see ConvertUtilsBean
 * @see ExtendedBeanUtilsBean
 */
public class ExtendedConvertUtilsBean extends ConvertUtilsBean {

    public ExtendedConvertUtilsBean() {
        super();
        register(true, false, 0);
        register(new LocalDateTimeConverter(), LocalDateTime.class);
        register(new LocaleConverter(), Locale.class);
        register(new EpochDateConverter(), Date.class);
        register(new EnumConverter(), Enum.class);
        register(new PathConverter(), Path.class);
    }

    /**
     * Look up and return any registered {@link Converter} for the specified
     * destination class; if there is no registered Converter, return
     * <code>null</code>.
     *
     * @param clazz Class for which to return a registered Converter
     * @return The registered {@link Converter} or <code>null</code> if not found
     */
    @Override
    public Converter lookup(final Class<?> clazz) {
        if (clazz.isEnum()){
            return super.lookup(Enum.class);
        }
        return super.lookup(clazz);
    }
}
