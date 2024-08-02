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
package com.norconex.commons.lang.bean.stubs;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class WithDefaultValues {
    private static final AutomobileConfig AUTOMOBILE =
            new AutomobileConfig().setModel("Corolla");

    public static final String DEFAULT_TEXT = "default-text";
    public static final int DEFAULT_NUMBER = 42;
    public static final AutomobileConfig DEFAULT_OBJECT = AUTOMOBILE;
    public static final List<String> DEFAULT_LIST = List.of("list-text");

    private String text = DEFAULT_TEXT;
    private int number = DEFAULT_NUMBER;
    private AutomobileConfig object = DEFAULT_OBJECT;
    private final List<String> collection = new ArrayList<>(DEFAULT_LIST);

    // this one is just so we can confirm something happened
    private String reference = null;
}
