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
package com.norconex.commons.lang.bean.stubs;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * One property for each situation a default can be in, so that what is
 * written for each of them is pinned down.
 */
@Data
@Accessors(chain = true)
public class ClearableDefaults {

    public static final String DEFAULT_TEXT = "default-text";
    public static final int DEFAULT_PRIMITIVE = 5;
    public static final Integer DEFAULT_BOXED = 7;
    public static final List<String> DEFAULT_LIST = List.of("a", "b");
    public static final String[] DEFAULT_ARRAY = { "x", "y" };

    /** A non-null default, which a configuration can clear with null. */
    private String text = DEFAULT_TEXT;

    /** A property with no default: null by default. */
    private String noDefault = null;

    private int primitive = DEFAULT_PRIMITIVE;

    private Integer boxed = DEFAULT_BOXED;

    /** A default list, which can be cleared with null or with an empty list. */
    private List<String> list = new ArrayList<>(DEFAULT_LIST);

    private String[] array = DEFAULT_ARRAY.clone();

    /** A default object of its own, itself with defaults. */
    private AutomobileConfig object =
            new AutomobileConfig().setModel("Corolla");

    /**
     * An explicit per-property inclusion wins over the mapper's: a null here is
     * never written, even though the default is non-null.
     */
    @JsonInclude(Include.NON_NULL)
    private String neverNull = "never-null-default";

    /** Always written, whatever it holds. */
    @JsonInclude(Include.ALWAYS)
    private String always = "always-default";

    // this one is just so there is always something written
    private String reference = "ref";
}
