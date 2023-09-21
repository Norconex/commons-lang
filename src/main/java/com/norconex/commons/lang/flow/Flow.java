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

import java.util.function.Consumer;

import lombok.Data;

// DELETE ME?  useless except maybe if we need a type to distinguish
// for (de)serialization?
// Maybe needed if we need a concrete type for Jackson.
@Data
public class Flow<T> implements Consumer<T> {

    private final Consumer<T> consumer;

    @Override
    public void accept(T t) {
        if (consumer != null) {
            consumer.accept(t);
        }
    }

    //TODO consider having a builder where statements and handlers
    // can be added in a chained manner. That would make it easier on devs.

    // with a "build()" for nested builders that would go back up?




//    private Consumer<T> consumer;
//
//    @JsonAnyGetter
//    @JsonAnySetter
//    private Map<String, Object> blah = new HashMap<>();

//    public Map<String, Object> getAddress() {
//        return blah;
//    }
//
//    @Override
//    public void accept(T t) {
//        consumer.accept(t);
//    }


}
