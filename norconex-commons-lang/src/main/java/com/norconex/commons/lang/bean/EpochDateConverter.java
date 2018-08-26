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

import java.util.Date;

import org.apache.commons.beanutils.converters.AbstractConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Date} converter.
 * Version of {@link org.apache.commons.beanutils.converters.DateConverter}
 * that uses the EPOCH value for string conversion.  
 * @since 2.0.0
 * @see ExtendedBeanUtilsBean
 */
public class EpochDateConverter extends AbstractConverter {

    private static final Logger LOG = LoggerFactory.getLogger(
            EpochDateConverter.class);
    
    @Override
    protected Class<?> getDefaultType() {
        return Date.class;
    }
    @Override
    protected <T> T convertToType(Class<T> type, Object value)
            throws Throwable {
        if (value == null) {
            return null;
        }
        try {
            return type.cast(
                    new Date(Long.parseLong(value.toString())));
        } catch (NumberFormatException e) {
            LOG.error("Could not parse date (not EPOCH?): {}", value);
            return null;
        }
    }
    @Override
    protected String convertToString(Object value) throws Throwable {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return Long.toString(((Date) value).getTime());
        }
        return value.toString();
    }
}
