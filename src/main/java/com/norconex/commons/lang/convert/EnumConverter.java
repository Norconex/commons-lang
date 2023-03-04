/* Copyright 2018-2023 Norconex Inc.
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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
        var trimmed = value.trim();
        return match(type, t -> matchToString(t, trimmed, false))
            .or(() -> match(type, t -> matchName(t, trimmed, false)))
            .or(() -> match(type, t -> matchToString(t, trimmed, true)))
            .or(() -> match(type, t -> matchName(t, trimmed, true)))
            .orElseThrow(() -> new ConverterException(String.format(
                "\"%s\" is not an enum value of %s", value, type)));
    }

    private static <T> boolean matchName(
            T t, String value, boolean stripNonAlphanum) {
        return matchToString(((Enum<?>) t).name(), value, stripNonAlphanum);
    }
    private static <T> boolean matchToString(
            T t, String value, boolean stripNonAlphanum) {
        var typeStr = t.toString();
        var valStr = value;
        if (stripNonAlphanum) {
            typeStr = typeStr.replaceAll("[^a-zA-Z0-9]", "");
            valStr = valStr.replaceAll("[^a-zA-Z0-9]", "");
        }
        return typeStr.equalsIgnoreCase(valStr);
    }

    private static <T> Optional<T> match(Class<T> type, Predicate<T> p) {
        return Stream.of(type.getEnumConstants())
            .filter(p)
            .findFirst();
    }
}
