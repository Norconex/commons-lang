/* Copyright 2022 Norconex Inc.
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

import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OSResourceTest {

    @Test
    void testOSResource() {
        OSResource<String> res = new OSResource<String>()
                .win("win")
                .unix("unix")
                .linux("linux")
                .mac("mac");
        assertThat("win".equals(res.get())).isEqualTo(IS_OS_WINDOWS);
        assertThat("unix".equals(res.get())).isEqualTo(
                IS_OS_UNIX && !IS_OS_LINUX && !IS_OS_MAC);
        assertThat("linux".equals(res.get())).isEqualTo(IS_OS_LINUX);
        assertThat("mac".equals(res.get())).isEqualTo(IS_OS_MAC);
        assertThat(new OSResource<>().get()).isNull();
    }
}
