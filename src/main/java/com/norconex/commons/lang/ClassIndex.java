/* Copyright 2026 Norconex Inc.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts the compiled classes of a package into a class index. Put it on a
 * package's {@code package-info.java}. The annotation processor
 * {@link ClassIndexProcessor} then writes
 * {@value ClassFinder#INDEX_RESOURCE} into the compiled output, listing every
 * concrete class of the compilation. {@link ClassFinder} reads that listing
 * instead of opening the jar and walking its entries, and it finds the classes
 * even when they sit in a nested jar, where a scan of the classpath cannot see
 * them.
 * <p>
 * The index lists all concrete (non-abstract, non-interface) classes, not only
 * the subtypes of one base type, so a lookup for any supertype finds the same
 * classes a scan would.
 * </p>
 *
 * @since 3.2.0
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PACKAGE)
public @interface ClassIndex {
}
