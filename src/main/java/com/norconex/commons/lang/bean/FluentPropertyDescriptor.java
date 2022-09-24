/* Copyright 2020-2022 Norconex Inc.
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
package com.norconex.commons.lang.bean;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>
 * An implementation of {@link PropertyDescriptor} that is more relaxed.
 * Unless a method name is explicitly specified in a constructor, it will
 * attempt to detect read and write methods in different style variations
 * (e.g., fluent or builder-style).
 * </p>
 * <p>
 * In addition, it supports read-only and write-only methods by default
 * (no need to pass <code>null</code> constructor arguments for method names).
 * Extra methods are also provided to check for the existence of read or
 * write methods: {@link #isReadable()} and {@link #isWritable()}.
 * If a property has no read method (write only), the property type will be
 * derived from the first setter with a single parameter encountered.
 * </p>
 * <p>
 * The following are examples of supported variations, in order of precedence
 * (if more than one variation is used for the same property).
 * </p>
 * <h3>Getters</h3>
 * <ul>
 *   <li><code>Foo getFoo()</code></li>
 *   <li><code>boolean isFoo()</code>
 *   <li><code>Foo foo()</code></li>
 * </ul>
 *
 * <h3>Setters</h3>
 * <ul>
 *   <li><code>void setFoo(Foo foo)</code></li>
 *   <li><code><i>Self</i> setFoo(Foo foo) //Self is "this" instance</code></li>
 *   <li><code>void foo(Foo foo)</code></li>
 *   <li><code><i>Self</i> foo(Foo foo) //Self is "this" instance</code></li>
 * </ul>
 *
 * @author Pascal Essiembre
 * @version 2.0.0
 */
@EqualsAndHashCode
@ToString
public class FluentPropertyDescriptor extends PropertyDescriptor {

    private static class BeanClassHolder {
        private final InheritableThreadLocal<Class<?>> TL =
                new InheritableThreadLocal<>();
        Class<?> set(Class<?> cls) {
            TL.set(cls);
            return cls;
        }
        Class<?> get() {
            return TL.get();
        }
    }
    private static final BeanClassHolder beanClassHolder =
            new BeanClassHolder();
    private boolean readMethodSet;
    private boolean writeMethodSet;

    public FluentPropertyDescriptor(
            String propertyName, Class<?> beanClass,
            String readMethodName, String writeMethodName)
            throws IntrospectionException {
        super(propertyName, beanClassHolder.set(beanClass),
                readMethodName, writeMethodName);
    }
    public FluentPropertyDescriptor(String propertyName, Class<?> beanClass)
            throws IntrospectionException {
        super(propertyName, beanClassHolder.set(beanClass), null, null);
    }
    public FluentPropertyDescriptor(String propertyName, Method readMethod,
            Method writeMethod) throws IntrospectionException {
        super(propertyName, readMethod, writeMethod);
        if (readMethod != null) {
            beanClassHolder.set(readMethod.getDeclaringClass());
        } else if (writeMethod != null) {
            beanClassHolder.set(writeMethod.getDeclaringClass());
        }
    }
    public FluentPropertyDescriptor(PropertyDescriptor propertyDescriptor)
            throws IntrospectionException {
        this(propertyDescriptor.getName(),
                propertyDescriptor.getReadMethod(),
                propertyDescriptor.getWriteMethod());
    }

    @Override
    public synchronized Method getWriteMethod() {
        var writeMethod = super.getWriteMethod();

        // exists the standard way or already set, end now
        if (writeMethod != null || writeMethodSet) {
            writeMethodSet = true;
            return writeMethod;
        }

        // if not found the standard way, try variations

        Class<?> beanClass = beanClassHolder.get();
        // Reader is more likely to be unique so we get property class from it
        // if not read-only.
        var readMethod = getReadMethod();
        var propType = readMethod != null ? readMethod.getReturnType() : null;

        // if read method is null, try normal way (else, super would return it)
        if (readMethod == null) {
            var methodName = "set" + StringUtils.capitalize(getName());
            writeMethod = getAlternateWriteMethod(
                    beanClass, methodName, propType, false);
        }

        // try fluent-style, returning self
        if (writeMethod == null) {
            var methodName = "set" + StringUtils.capitalize(getName());
            writeMethod = getAlternateWriteMethod(
                    beanClass, methodName, propType, true);
        }

        // try build-style, returning void
        if (writeMethod == null) {
            writeMethod = getAlternateWriteMethod(
                    beanClass, getName(), propType, false);
        }

        // try build-style, returning self
        if (writeMethod == null) {
            writeMethod = getAlternateWriteMethod(
                    beanClass, getName(), propType, true);
        }

        try {
            setWriteMethod(writeMethod);
        } catch (IntrospectionException e) {
            //NOOP swallow
        }
        writeMethodSet = true;
        return writeMethod;
    }

    @Override
    public synchronized Method getReadMethod() {
        var readMethod = super.getReadMethod();

        // exists the standard way, or already set, end now
        if (readMethod != null || readMethodSet) {
            readMethodSet = true;
            return readMethod;
        }

        // if not found the standard way, try build-style
        readMethod = MethodUtils.getAccessibleMethod(
                beanClassHolder.get(), getName());

        try {
            setReadMethod(readMethod);
        } catch (IntrospectionException e) {
            //NOOP swallow
        }
        readMethodSet = true;
        return readMethod;
    }

    /**
     * Gets whether this described property can be read (has a reader method).
     * @return <code>true</code> if it can be read
     */
    public boolean isReadable() {
        return getReadMethod() != null;
    }
    /**
     * Gets whether this described property can be written to
     * (has a writer method).
     * @return <code>true</code> if it can be written
     */
    public boolean isWritable() {
        return getWriteMethod() != null;
    }

    /**
     * Initialize a new {@link FluentPropertyDescriptor} with the
     * supplied {@link PropertyDescriptor} unless it already
     * is an instance of {@link FluentPropertyDescriptor}.
     *
     * @param pd property descriptor
     * @return fluent property descriptor or <code>null</code> if
     *     the property descriptor is <code>null</code>
     * @throws IntrospectionException if we cannot convert to fluent instance
     */
    public static FluentPropertyDescriptor toFluent(PropertyDescriptor pd)
            throws IntrospectionException {
        if (pd == null) {
            return null;
        }
        if (pd instanceof FluentPropertyDescriptor) {
            return (FluentPropertyDescriptor) pd;
        }
        return new FluentPropertyDescriptor(pd);
    }

    private Method getAlternateWriteMethod(
            Class<?> beanClass,
            String methodName,
            Class<?> propType,
            boolean fluent) {
        // if propType is set, use it. Else, guess it, by picking the
        // first setter with a matching name.
        Method method = null;
        if (propType != null) {
            try {
                method = beanClass.getMethod(methodName, propType);
            } catch (NoSuchMethodException
                    | SecurityException
                    | NullPointerException e ) {
                // swallow
            }
        } else {
            // get first matching method name, having a single argument
            method = Stream.of(beanClass.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .filter(m -> m.getParameterCount() == 1)
                .findFirst()
                .orElse(null);
        }

        // if fluent is requested and is not fluent, we don't return it
        if (fluent && method != null
                && !method.getReturnType().equals(beanClass)) {
            method = null;
        }
        return method;
    }
}