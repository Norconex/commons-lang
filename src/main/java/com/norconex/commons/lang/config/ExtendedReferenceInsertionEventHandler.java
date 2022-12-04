/* Copyright 2020 Norconex Inc.
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
package com.norconex.commons.lang.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.apache.velocity.context.Context;

import com.norconex.commons.lang.SystemUtil;

/**
 * Velocity reference insertion event handler that when triggered,
 * will try to resolve the reference against system properties or
 * environment variables (in that order) if it could not resolved the normal
 * way (before returning default value, if one was specified in the reference).
 * @since 2.0.0
 */
public class ExtendedReferenceInsertionEventHandler
        implements ReferenceInsertionEventHandler  {

    @Override
    public Object referenceInsert(
            Context context, String reference, Object value) {

        String ref = StringUtils.trimToEmpty(reference);
        ref = StringUtils.strip(ref, "${}").trim();
        ref = StringUtils.substringBefore(ref, "|").trim();

        Object val = context.get(ref);

        // if it exists as env/property, replace.
        String replacement = SystemUtil.getEnvironmentOrProperty(ref);
        if (replacement != null) {
            val = replacement;
        }

        // if still null, return original value
        if (val == null) {
            val = value;
        }

        return val;
    }
}
