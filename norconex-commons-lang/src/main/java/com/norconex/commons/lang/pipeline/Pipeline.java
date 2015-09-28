/* Copyright 2014 Norconex Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Represent a very simple pipeline container for a list of executable tasks 
 * called "pipeline stages" (defined using
 * {@link IPipelineStage}).
 * This pipeline class can also be used a pipeline stage to create 
 * pipe hierarchies.
 * For more sophisticated work flow needs, consider using a more advanced 
 * framework such as Norconex JEF.
 * @author Pascal Essiembre
 * @param <T> pipeline context type
 * @since 1.5.0
 */
//TODO make it implement List?
public class Pipeline<T> implements IPipelineStage<T> {

    private static final Logger LOG = LogManager.getLogger(Pipeline.class);
    
    private final List<IPipelineStage<T>> stages = new ArrayList<>();
    
    /**
     * Constructor.
     */
    public Pipeline() {
    }
    /**
     * Creates a new pipeline with the specified stages.
     * @param stages the stages to execute
     */
    public Pipeline(List<IPipelineStage<T>> stages) {
        this.stages.addAll(stages);
    }

    /**
     * Gets the pipeline stages.
     * @return the pipeline stages
     */
    public List<IPipelineStage<T>> getStages() {
        return Collections.unmodifiableList(stages);
    }

    /**
     * Adds stages to the pipeline.
     * @param stages pipeline stages to add
     * @return this instance, for chaining
     */
    public Pipeline<T> addStages(List<IPipelineStage<T>> stages) {
        this.stages.addAll(stages);
        return this;
    }
    /**
     * Adds a stage to the pipeline.
     * @param stage pipeline stage to add
     * @return this instance, for chaining
     */
    public Pipeline<T> addStage(IPipelineStage<T> stage) {
        this.stages.add(stage);
        return this;
    }

    public void clearStages() {
        stages.clear();
    }
    
    @Override
    public boolean execute(T context) {
        for (IPipelineStage<T> stage : stages) {
            if (!stage.execute(context)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Pipeline execution stopped at stage: " + stage);
                }
                return false;
            }
        }
        return true;
    }

}
