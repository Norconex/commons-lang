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
package com.norconex.commons.lang;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Exception-related utility methods.
 * @since 2.0.0
 */
public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    /**
     * <p>Gets a formatted string made of short messages summarizing all
     * exceptions objects in the exception chain, starting with and including
     * the supplied exception.</p>
     *
     * <p>Each messages returned are in the form
     * {ClassNameWithoutPackage}: {ThrowableMessage}.
     * Exceptions with <code>null</code> messages are
     * returned as empty strings.</p>
     *
     * <p>This method handles infinite loops. A <code>null</code> exception
     * returns an empty string (never <code>null</code>).</p>
     *
     * @param throwable  the throwable to inspect, may be <code>null</code>
     * @return formatted string or empty string, never <code>null</code>
     */
    public static String getFormattedMessages(Throwable throwable) {
        int cnt = 0;
        StringBuilder b = new StringBuilder();
        for (String msg : getMessageList(throwable)) {
            if (cnt > 0) {
                b.append('\n');
            }
            cnt++;
            b.append(StringUtils.repeat(' ', cnt * 2) + "→ " + msg);
        }
        return b.toString();
    }

    /**
     * <p>Gets a list of short messages summarizing all exceptions objects
     * in the exception chain, starting with and including the supplied
     * exception.</p>
     *
     * <p>Each messages returned are in the form
     * {ClassNameWithoutPackage}: {ThrowableMessage}.
     * Exceptions with <code>null</code> messages are
     * returned as empty strings.</p>
     *
     * <p>This method handles infinite loops and never returns
     * <code>null</code>.</p>
     *
     * @param throwable  the throwable to inspect, may be <code>null</code>
     * @return the list of messages, never <code>null</code>
     */
    public static List<String> getMessageList(Throwable throwable) {
        List<String> msgs = new ArrayList<>();
        for (Throwable t : ExceptionUtils.getThrowableList(throwable)) {
            msgs.add(ExceptionUtils.getMessage(t));
        }
        return msgs;
    }
}
