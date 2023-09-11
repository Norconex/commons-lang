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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GenericConverter for {@link Path}.
 * @since 2.0.0
 */
public class PathConverter extends AbstractConverter {

    @Override
    protected String nullSafeToString(Object object) {
        return object.toString();
    }

    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        if (Path.class.equals(type)) {
            return type.cast(Paths.get(value));
        }
        throw new ConverterException(
                "Type " + type + " is not supported by this converter.");
    }

    /**
     * JSON (Jackson) serializer using this converter.
     * @since 3.0.0
     */
    public static class JsonSerializer
            extends GenericJsonSerializer<Path>{}

    /**
     * JSON (Jackson) deserializer using this converter.
     * @since 3.0.0
     */
    public static class JsonDeserializer
            extends GenericJsonDeserializer<Path> {
        public JsonDeserializer() {
            super(Path.class);
        }
    }

    /**
     * XML (JAXB) adapter using this converter.
     * @since 3.0.0
     */
    public static class XmlAdapter extends GenericXmlAdapter<Path> {
        public XmlAdapter() {
            super(Path.class);
        }
    }
}

