/* Copyright 2026 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

class ZoneIdConverterTest {

    @Test
    void testConvert() {
        var converter = new ZoneIdConverter();

        assertThat(converter.toString(ZoneId.of("UTC"))).isEqualTo("UTC");
        assertThat(converter.toType(" Europe/Paris ", ZoneId.class))
                .isEqualTo(ZoneId.of("Europe/Paris"));
    }

    @Test
    void testJsonSerializerAndDeserializer() throws Exception {
        var holder = new ZoneIdHolder();
        holder.setZoneId(ZoneId.of("America/Toronto"));

        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(holder);

        assertThat(json).contains("\"zoneId\":\"America/Toronto\"");
        assertThat(mapper.readValue(json, ZoneIdHolder.class).getZoneId())
                .isEqualTo(holder.getZoneId());
    }

    @Test
    void testXmlAdapter() throws Exception {
        var adapter = new ZoneIdConverter.XmlAdapter();

        assertThat(adapter.marshal(ZoneId.of("UTC"))).isEqualTo("UTC");
        assertThat(adapter.unmarshal(" Canada/Atlantic "))
                .isEqualTo(ZoneId.of("Canada/Atlantic"));
    }

    @Data
    private static class ZoneIdHolder {
        @JsonSerialize(using = ZoneIdConverter.JsonSerializer.class)
        @JsonDeserialize(using = ZoneIdConverter.JsonDeserializer.class)
        private ZoneId zoneId;
    }
}