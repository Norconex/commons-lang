/* Copyright 2020-2023 Norconex Inc.
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
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.commons.lang3.reflect.ConstructorUtils;

/**
 * Class-related utility methods.
 * @since 2.0.0
 */
public final class ClassUtil {

    private ClassUtil() {
    }

    /**
     * Gets a type annotation, considering super classes.  If more than
     * one class in the hierarchy has an annotation of the same type, the
     * first one matching is returned.
     * @param <A> annotation type
     * @param annotatedClass annotated class
     * @param annotationClass the annotation class we are looking for
     * @return an annotation, or <code>null</code> if none is found
     */
    public static <A extends Annotation> A getAnnotation(
            Class<?> annotatedClass, Class<A> annotationClass) {
        Class<?> cls = annotatedClass;
        while (cls != null) {
            var a = cls.getAnnotation(annotationClass);
            if (a != null) {
                return a;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    /**
     * Create a new instance of this class the with supplied arguments
     * and returns an an unchecked exception in case of failure.
     * In case of multiple constructors with the same number
     * of arguments, it will try to find the one matching the argument types.
     *
     * @param clazz the class to instantiate
     * @param args constructor arguments
     * @return new instance of the class or <code>null</code>
     *     if class is <code>null</code>
     * @throws IllegalArgumentException wrapper exception for checked
     *     exceptions thrown when the class cannot be instantiated with the
     *     supplied arguments
     * @since 3.0.0
     */
    public static <T> T newInstance(Class<T> clazz, Object... args) {
        if (clazz == null) {
            return null;
        }
        try {
            return ConstructorUtils.invokeConstructor(clazz, args);
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | InstantiationException e) {
            throw new IllegalArgumentException(
                    ("Could not instantiate class %s with the following "
                            + "arguments: %s.").formatted(
                                    clazz.getCanonicalName(),
                                    Arrays.toString(args)));
        }
    }
}
