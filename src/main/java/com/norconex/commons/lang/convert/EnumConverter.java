/* Copyright 2018-2020 Norconex Inc.
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
 * {@link Enum} converter.
 * @since 2.0.0
 */
public class EnumConverter extends AbstractConverter {

    @Override
    protected String nullSafeToString(Object object) {
        return object.toString();
    }

    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        String trimmed = value.trim();
        // Try to match with "toString()"
        for (T e : type.getEnumConstants()) {
            if (((Enum<?>) e).toString().equalsIgnoreCase(trimmed)) {
                return type.cast(e);
            }
        }
        // Try to match with "name()"
        for (T e : type.getEnumConstants()) {
            if (((Enum<?>) e).name().equalsIgnoreCase(trimmed)) {
                return type.cast(e);
            }
        }
        throw new ConverterException(String.format(
                "\"%s\" is not an enum value of %s", value, type));
    }
}
