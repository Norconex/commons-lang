/* Copyright 2020 Norconex Inc.
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

import java.lang.annotation.Annotation;

/**
 * Class-related utility methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class ClassUtil {

    private ClassUtil() {
        super();
    }

    /**
     * Gets a type annotation, considering super classes.  If more than
     * one class in the hierarchy has an annotation of the same type, the
     * first one matching is returned.
     * @param <A> annotation type
     * @param annotatedClass annotation class
     * @param annotationClass class we search for an annotatation
     * @return an annotation, or <code>null</code> if none is found
     */
    public static <A extends Annotation> A getAnnotation(
            Class<?> annotatedClass, Class<A> annotationClass) {
        Class<?> cls = annotatedClass;
        while (cls != null) {
            A a = cls.getAnnotation(annotationClass);
            if (a != null) {
                return a;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }
}
