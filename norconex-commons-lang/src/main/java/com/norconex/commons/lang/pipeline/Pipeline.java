/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public boolean execute(T context) throws PipelineException {
        for (IPipelineStage<T> stage : stages) {
            if (!stage.execute(context)) {
                return false;
            }
        }
        return true;
    }

}
