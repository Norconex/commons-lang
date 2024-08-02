/* Copyright 2023 Norconex Inc.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Valid
@JsonPropertyOrder({"make", "model", "year"})
public class AutomobileConfig {
    @JsonProperty("make")
    private String brand;
    private String model;
    @Min(1900)
    private int year;
}
