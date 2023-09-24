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
package com.norconex.commons.lang.flow;

import java.util.List;
import java.util.function.Predicate;

import com.norconex.commons.lang.function.Predicates;

import lombok.Data;
import lombok.NonNull;

@Data
public class Group<T> extends Predicates<T> {
    private static final long serialVersionUID = 1L;

    public Group(boolean any) {
        super(any);
    }

    @SuppressWarnings("unchecked")
    public static <T> Group<T> anyOf(@NonNull Predicate<T>... predicates) {
        var allOf = new Group<T>(true);
        allOf.addAll(List.of(predicates));
        return allOf;
    }

    @SuppressWarnings("unchecked")
    public static <T> Group<T> ofAll(@NonNull Predicate<T>... predicates) {
        var allOf = new Group<T>(false);
        allOf.addAll(List.of(predicates));
        return allOf;
    }
}
