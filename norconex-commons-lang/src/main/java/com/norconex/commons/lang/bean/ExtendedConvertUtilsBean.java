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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.DateConverter;

/**
 * <p>
 * Extends {@link BeanUtilsBean} to support mapping of additional types as
 * well as collections and arrays.  Additional types:
 * </p>
 * <ul>
 *   <li>{@link LocalDateTimeConverter}</li>
 *   <li>{@link LocaleConverter}</li>
 * </ul>
 * <p>
 * Also, {@link DateConverter} is replaced with {@link EpochDateConverter}
 * which converts dates to their EPOCH values instead.
 * </p>
 * <p>
 * By default {@link ConversionException} is be thrown if conversion failed.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see ConvertUtilsBean
 */
public class ExtendedConvertUtilsBean extends ConvertUtilsBean {

    public ExtendedConvertUtilsBean() {
        super();
        register(true, false, 0);
        register(new LocalDateTimeConverter(), LocalDateTime.class);
        register(new LocaleConverter(), Locale.class);
        register(new EpochDateConverter(), Date.class);

        //TODO add PathConverter
    }
}
