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
package com.norconex.commons.lang.time;

import java.util.Locale;

/**
 * Formats a single duration unit according to supplied locale
 * and plurality flag.
 * @since 3.0.0 (renamed from v2.x IDurationFormatter)
 * @see DurationFormatter
 */
@FunctionalInterface
public interface DurationUnitFormatter {
    /**
     * Format a duration unit
     * @param unit duration unit
     * @param locale locale
     * @param plural <code>true</code> if plural
     * @return formatted unit
     */
    String format(DurationUnit unit, Locale locale, boolean plural);
}