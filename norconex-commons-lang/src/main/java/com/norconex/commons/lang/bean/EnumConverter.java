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

import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * {@link Enum} converter. Converts enum constants to lowercase strings
 * and strings can match enum constants without regard to case sensitivity
 * @since 2.0.0
 * @see ExtendedBeanUtilsBean
 */
public class EnumConverter extends AbstractConverter {

    public EnumConverter() {
        super();
    }
    public EnumConverter(Object defaultValue) {
        super(defaultValue);
    }

    @Override
    protected Class<?> getDefaultType() {
        return Enum.class;
    }
    @Override
    protected <T> T convertToType(Class<T> type, Object value)
            throws Throwable {
        if (value == null) {
            return null;
        }

        String strValue = value.toString().trim();
        for (T e : type.getEnumConstants()) {
            if (((Enum<?>) e).name().equalsIgnoreCase(strValue)) {
                return type.cast(e);
            }
        }

        throw conversionException(type, value);
    }
}
