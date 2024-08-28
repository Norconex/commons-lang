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
package com.norconex.commons.lang.convert;

/**
 * Converts from Object to String and from String to Object.
 * @since 2.0.0 (renamed from IConverter in version 3.0.0)
 */
public interface Converter {

    <T> T toType(String value, Class<T> type);

    default <T> T toType(String value, Class<T> type, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        T obj = toType(value, type);
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }

    String toString(Object object);

    default String toString(Object object, String defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        String value = toString(object);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
