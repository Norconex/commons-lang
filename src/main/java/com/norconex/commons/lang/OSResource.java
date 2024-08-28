/* Copyright 2018-2022 Norconex Inc.
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
package com.norconex.commons.lang;

import org.apache.commons.lang3.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Provides an abstraction over operating-system specific resources
 * (e.g. paths).
 * Simply a convenience class saving you from checking for the OS yourself
 * to figure out which resource to use, for major OSes.
 * </p>
 * <p>
 * A different resource can be specified for all OSes but only one
 * will actually get set, based on the currently detected OS.
 * OSes are mutually exclusive. For instance, Unix does not include Linux
 * or Mac operating systems.
 * </p>
 *
 * @param <T> resource type
 * @since 2.0.0
 */
@Slf4j
public class OSResource<T> {

    private T resource;

    public OSResource<T> win(T resource) {
        if (SystemUtils.IS_OS_WINDOWS) {
            this.resource = resource;
        }
        return this;
    }

    public OSResource<T> unix(T resource) {
        if (SystemUtils.IS_OS_UNIX
                && !SystemUtils.IS_OS_LINUX
                && !SystemUtils.IS_OS_MAC) {
            this.resource = resource;
        }
        return this;
    }

    public OSResource<T> linux(T resource) {
        if (SystemUtils.IS_OS_LINUX) {
            this.resource = resource;
        }
        return this;
    }

    public OSResource<T> mac(T resource) {
        if (SystemUtils.IS_OS_MAC) {
            this.resource = resource;
        }
        return this;
    }

    public T get() {
        if (resource == null) {
            LOG.debug("No resource set for this operating system.");
        }
        return resource;
    }
}
