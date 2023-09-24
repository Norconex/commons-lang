/* Copyright 2023 Norconex Inc.
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
package com.norconex.commons.lang.flow.module;

import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.bean.BeanMapper.FlowPredicateAdapter;
import com.norconex.commons.lang.bean.BeanMapper.FlowConsumerAdapter;
import com.norconex.commons.lang.flow.module.FlowDeserializer.FlowDeserContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
final class FlowUtil {

    private FlowUtil() {}

    static void logOpen(FlowDeserContext ctx, String nodeName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(StringUtils.repeat("  ", ctx.incrementDepth())
                    + "<" + nodeName + ">");
        }
    }
    static void logClose(FlowDeserContext ctx, String nodeName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(StringUtils.repeat("  ", ctx.decrementDepth())
                    + "</" + nodeName + ">");
        }
    }
    static void logBody(FlowDeserContext ctx, Object obj) {
        if (LOG.isDebugEnabled()) {
            var resolved = obj;
            if (obj instanceof FlowConsumerAdapter<?> fica) {
                resolved = fica.getConsumerAdaptee();
            } else if (obj instanceof FlowPredicateAdapter<?> fca) {
                resolved = fca.getPredicateAdaptee();
            }
            LOG.debug(StringUtils.repeat("  ", ctx.getDepth()) + resolved);
        }
    }
}
