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

import java.awt.Dimension;
import java.util.regex.Pattern;

/**
 * <p>{@link Dimension} converter. Converted as strings by joining width and
 * height with an "x" (e.g., 640x480).  The conversion from strings supports
 * many formats, as long as two numeric groups are present (width and height,
 * respectively). If more than two groups of digits, only the first two
 * are kept. If only one value is present, it is used for both width and height.
 * Examples of supported formats:
 * </p>
 * <ul>
 *   <li>640x480</li>
 *   <li>640 480</li>
 *   <li>width:640, height:480</li>
 *   <li>aaa640bbb480ccc100</li>
 *   <li>1200</li>
 *   <li>size:1200px</li>
 * </ul>
 * @since 2.0.0
 */
public class DimensionConverter extends AbstractConverter {

    @Override
    protected String nullSafeToString(Object object) {
        if (object instanceof Dimension dim) {
            return dim.width + "x" + dim.height;
        }
        throw toUnsupportedTypeException(object);
    }

    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        Dimension d = null;
        var m = Pattern.compile("\\d+").matcher(value);
        if (m.find()) {
            var width = Integer.parseInt(m.group());
            if (m.find()) {
                d = new Dimension(width, Integer.parseInt(m.group()));
            } else {
                d = new Dimension(width, width);
            }
        }
        if (d == null) {
            throw toTypeException(value, type);
        }
        return type.cast(d);
    }

    /**
     * JSON (Jackson) serializer using this converter.
     * @since 3.0.0
     */
    public static class JsonSerializer
            extends GenericJsonSerializer<Dimension>{}

    /**
     * JSON (Jackson) deserializer using this converter.
     * @since 3.0.0
     */
    public static class JsonDeserializer
            extends GenericJsonDeserializer<Dimension> {
        public JsonDeserializer() {
            super(Dimension.class);
        }
    }

    /**
     * XML (JAXB) adapter using this converter.
     * @since 3.0.0
     */
    public static class XmlAdapter extends GenericXmlAdapter<Dimension> {
        public XmlAdapter() {
            super(Dimension.class);
        }
    }
}
