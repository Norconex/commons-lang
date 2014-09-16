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


/**
 * A logical step in the pipepline.  Typically an action to be executed.
 * @author Pascal Essiembre
 * @param <T> pipeline context type
 * @since 1.5.0
 */
public interface IPipelineStage<T> {

    /**
     * Executes this pipeline stage.  Implementors are encouraged to throw
     * {@link PipelineException} upon errors.
     * @param context pipeline context
     * @return whether to continue pipeline execution or stop (<code>true</code>
     *         to continue).
     */
    boolean execute(T context);
}
