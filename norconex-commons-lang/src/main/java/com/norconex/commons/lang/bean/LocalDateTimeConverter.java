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

import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link LocalDateTime} converter.
 * @since 2.0.0
 * @see ExtendedBeanUtilsBean
 */
public class LocalDateTimeConverter extends AbstractConverter {
    @Override
    protected Class<?> getDefaultType() {
        return LocalDateTime.class;
    }
    @Override
    protected <T> T convertToType(Class<T> type, Object value)
            throws Throwable {
        if (value == null) {
            return null;
        }
        return type.cast(LocalDateTime.parse(value.toString()));
    }
}
