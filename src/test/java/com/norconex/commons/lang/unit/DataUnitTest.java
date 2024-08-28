/* Copyright 2010-2022 Norconex Inc.
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
package com.norconex.commons.lang.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataUnitTest {

    @Test
    void testConversions() {
        Assertions.assertEquals(0, DataUnit.B.to(5, DataUnit.KB).intValue());
        Assertions.assertEquals(2000, DataUnit.KB.toBytes(2).intValue());
        Assertions.assertEquals(
                3, DataUnit.MIB.from(3072, DataUnit.KIB).intValue());

        // Test bytes vs bits
        Assertions.assertEquals(1024 * 8, DataUnit.KIB.toBits(1).intValue());

        assertThat(DataUnit.B.getName()).isEqualTo("byte");
        assertThat(DataUnit.EIB)
                .returns("exibyte", DataUnit::getName)
                .returns("exi", DataUnit::getPrefix)
                .returns("EiB", DataUnit::getSymbol)
                .returns(6, DataUnit::getGroupIndex)
                .returns(true, DataUnit::isByteUnit)
                .returns(true, DataUnit::isBinary)
                .returns(false, DataUnit::isDecimal);
        assertThat(DataUnit.KBIT.bits()).isEqualTo(1000);
        assertThat(DataUnit.KBIT.bytes()).isEqualTo(125);
        assertThat(DataUnit.KBIT.fromBits(2000))
                .isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(DataUnit.KBIT.fromBytes(250))
                .isEqualByComparingTo(BigDecimal.valueOf(2));

        assertThat(DataUnit.from("EIB")).isEqualTo(DataUnit.EIB);
        assertThat(DataUnit.EIB).hasToString("EiB");
        assertThat(DataUnit.from(null)).isNull();
    }
}
