/* Copyright 2024 Norconex Inc.
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

import java.io.IOException;
import java.time.ZoneId;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

/**
 * {@link ZoneIdConverter} converter.
 * @since 3.0.0
 */
public class ZoneIdConverter extends AbstractConverter {

    @Override
    protected String nullSafeToString(Object object) {
        return object.toString();
    }

    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        return type.cast(ZoneId.of(value.trim()));
    }

    /**
     * JSON (Jackson) serializer using this converter.
     */
    public static class JsonSerializer
            extends GenericJsonSerializer<ZoneId> {
        @Override
        public void serialize(ZoneId value,
                JsonGenerator gen, SerializationContext sp) {
            gen.writeString(new ZoneIdConverter().toString(value));
        }
    }

    /**
     * JSON (Jackson) deserializer using this converter.
     */
    public static class JsonDeserializer
            extends GenericJsonDeserializer<ZoneId> {
        public JsonDeserializer() {
            super(ZoneId.class);
        }
    }

    /**
     * XML (JAXB) adapter using this converter.
     */
    public static class XmlAdapter extends GenericXmlAdapter<ZoneId> {
        public XmlAdapter() {
            super(ZoneId.class);
        }

        @Override
        public String marshal(ZoneId obj) {
            return new ZoneIdConverter().toString(obj);
        }

        @Override
        public ZoneId unmarshal(String value) {
            return new ZoneIdConverter().toType(value, ZoneId.class);
        }
    }
}
