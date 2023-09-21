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
package com.norconex.commons.lang.flow.module;

import java.io.IOException;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableConsumer;

import com.fasterxml.jackson.databind.JsonNode;

final class FlowUtil {

    private FlowUtil() {}

    static void forEachFields(
            JsonNode node, FailableBiConsumer<String, JsonNode, IOException> c)
                    throws IOException {
        for(var it = node.fields(); it.hasNext(); ) {
            var f = it.next();
            c.accept(f.getKey(), f.getValue());
        }
    }

    static void forEachElements(
            JsonNode node, FailableConsumer<JsonNode, IOException> c)
                    throws IOException {
        for(var it = node.elements(); it.hasNext(); ) {
            c.accept(it.next());
        }
    }

    static void forEachArrayNodes(
            JsonNode node, FailableConsumer<JsonNode, IOException> c)
                    throws IOException {
        if (node.isArray()) {
            for (JsonNode n : node) {
                c.accept(n);
            }
        } else {
            c.accept(node);
        }
    }

    // goes through all fields (like "forEachFeilds") of objects contained
    // in an array.  Useful also when you have 1-element array like this:
    // [ { myobj1: {}, myobj2: {}, myobj3: {} } ]
    static void forEachArrayObjectFields(
            JsonNode node, FailableBiConsumer<String, JsonNode, IOException> c)
                    throws IOException {
        forEachArrayNodes(node, n -> forEachFields(n, c));
    }

}
