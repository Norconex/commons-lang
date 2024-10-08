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

import java.nio.charset.Charset;

/**
 * {@link Charset} converter.
 * @since 2.0.0
 */
public class CharsetConverter extends AbstractConverter {

    @Override
    protected String nullSafeToString(Object object) {
        return object.toString();
    }

    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        try {
            return type.cast(Charset.forName(value));
        } catch (RuntimeException e) {
            throw toTypeException(value, type, e);
        }
    }

    /**
     * JSON (Jackson) serializer using this converter.
     * @since 3.0.0
     */
    public static class JsonSerializer
            extends GenericJsonSerializer<Charset> {
    }

    /**
     * JSON (Jackson) deserializer using this converter.
     * @since 3.0.0
     */
    public static class JsonDeserializer
            extends GenericJsonDeserializer<Charset> {
        public JsonDeserializer() {
            super(Charset.class);
        }
    }

    /**
     * XML (JAXB) adapter using this converter.
     * @since 3.0.0
     */
    public static class XmlAdapter extends GenericXmlAdapter<Charset> {
        public XmlAdapter() {
            super(Charset.class);
        }
    }
}
