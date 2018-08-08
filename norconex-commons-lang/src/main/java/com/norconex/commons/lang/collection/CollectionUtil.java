/* Copyright 2018 Norconex Inc.
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
package com.norconex.commons.lang.collection;

import java.util.Arrays;
import java.util.Collection;

/**
 * Collection-related utility methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class CollectionUtil {

    private CollectionUtil() {
        super();
    }

    /**
     * Sets all values of the source collection into the target one.
     * Same as doing a "clear", followed by "addAll", but it also checks
     * for <code>null</code> and will not clear/add if the source
     * collection is the same instance as the target one.
     * If target is <code>null</code>, invoking this method has no effect.
     * If source is <code>null</code> will clear the target collection
     * @param target target collection
     * @param source source collection
     */
    public static <T> void setAll(Collection<T> target, Collection<T> source) {
        if (target == null || target == source) {
            return;
        }
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }
    /**
     * Sets all values of the source array into the target collection.
     * Same as doing a "clear", followed by "addAll" after converting the
     * array to a list. It also checks
     * for <code>null</code>.
     * If target is <code>null</code>, invoking this method has no effect.
     * If source is <code>null</code> will clear the target collection
     * @param target target collection
     * @param source source collection
     */
    @SafeVarargs
    public static <T> void setAll(Collection<T> target, T... source) {
        if (source == null) {
            return;
        }
        setAll(target, Arrays.asList(source));
    }
}
