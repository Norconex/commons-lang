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

/**
 * Exception thrown when pipeline could not be executed properly.
 * @author Pascal Essiembre
 * @since 1.5.0
 */
public class PipelineException extends RuntimeException {

    private static final long serialVersionUID = -805913995358009121L;

    public PipelineException() {
        super();
    }

    public PipelineException(String message) {
        super(message);
    }

    public PipelineException(Throwable cause) {
        super(cause);
    }

    public PipelineException(String message, Throwable cause) {
        super(message, cause);
    }

}
