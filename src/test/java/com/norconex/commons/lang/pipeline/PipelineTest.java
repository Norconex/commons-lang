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
package com.norconex.commons.lang.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PipelineTest {

    @SuppressWarnings({ "deprecation" })
    @Test
    void testPipeline() {
        final List<String> context = new ArrayList<>();

        Pipeline<List<String>> pipeline = new Pipeline<>(Arrays.asList(
                ctx -> {
                    ctx.add("1");
                    return true;
                }));
        pipeline.addStage(ctx -> {
            ctx.add("2");
            return true;
        });
        pipeline.addStages(Arrays.asList(
                ctx -> {
                    ctx.add("3");
                    return false;
                },
                ctx -> {
                    ctx.add("4");
                    return true;
                }));

        assertThat(pipeline.execute(context)).isFalse();
        assertThat(context).containsExactly("1", "2", "3");
        assertThat(pipeline.getStages()).hasSize(4);

        pipeline.clearStages();
        context.clear();
        assertThat(pipeline.getStages()).isEmpty();

        pipeline = new Pipeline<>();
        assertThat(pipeline.getStages()).isEmpty();
        assertThat(pipeline.execute(context)).isTrue();
        assertThat(pipeline.getStages()).isEmpty();
    }
}
