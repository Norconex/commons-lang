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

import java.time.Duration;

import com.norconex.commons.lang.time.DurationParser;

import lombok.EqualsAndHashCode;

/**
 * {@link Duration} converter using milliseconds for string representation,
 * but can also parse back plain-English durations.
 * @since 2.0.0
 */
@EqualsAndHashCode
public class DurationConverter extends AbstractConverter {

    private final DurationParser parser = new DurationParser();

    @Override
    protected String nullSafeToString(Object object) {
        return Long.toString(((Duration) object).toMillis());
    }

    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        return type.cast(parser.parse(value.trim()));
    }
}
