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
import org.apache.velocity.app.event.InvalidReferenceEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.util.introspection.Info;

import com.norconex.commons.lang.SystemUtil;

/**
 * Velocity invalid reference event handler that when triggered,
 * will try to resolve the reference
 * against system properties or environment variables (in that order).
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class InvalidReferenceExternalFallback
        implements InvalidReferenceEventHandler {

    @Override
    public Object invalidGetMethod(Context context, String reference,
            Object object, String property, Info info) {
        return SystemUtil.getEnvironmentOrProperty(
                StringUtils.strip(reference, "${}"));
    }

    @Override
    public boolean invalidSetMethod(Context context, String leftreference,
            String rightreference, Info info) {
        //NOOP
        return false;
    }

    @Override
    public Object invalidMethod(Context context, String reference,
            Object object, String method, Info info) {
        //NOOP
        return object;
    }
}
