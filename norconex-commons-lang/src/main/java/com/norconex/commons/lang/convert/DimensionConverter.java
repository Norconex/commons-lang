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
package com.norconex.commons.lang.convert;

import java.awt.Dimension;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>{@link Dimension} converter. Converted as strings by joining width and
 * height with an "x" (e.g., 640x480).  Converting from strings supports
 * many format, as long as two numeric groups are present (width and height,
 * respectively). If more than two groups of digits, only the first two
 * are kept. Examples of supported formats:
 * </p>
 * <ul>
 *   <li>640x480</li>
 *   <li>640 480</li>
 *   <li>width:640, height:480</li>
 *   <li>aaa640bbb480ccc100</li>
 * </ul>
 * @since 2.0.0
 */
public class DimensionConverter extends AbstractConverter {

    @Override
    protected String nullSafeToString(Object object) {
        if (object instanceof Dimension) {
            Dimension d = (Dimension) object;
            return d.width + "x" + d.height;
        }
        throw new ConverterException(
                "Type " + object.getClass().getSimpleName()
              + " is not supported by this converter.");
    }

    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        Dimension d = null;
        Matcher m = Pattern.compile("\\d+").matcher(value);
        if (m.find()) {
            int width = Integer.parseInt(m.group());
            if (m.find()) {
                d = new Dimension(width, Integer.parseInt(m.group()));
            }
        }
        if (d == null) {
            throw toTypeException(value, type);
        }
        return type.cast(d);
    }
}
