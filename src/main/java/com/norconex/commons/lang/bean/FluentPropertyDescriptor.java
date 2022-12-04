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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * <p>
 * An implementation of {@link PropertyDescriptor} that is more relaxed
 * when deriving method names from property names.
 * Unless a method or method name is explicitly passed in a constructor,
 * this class will attempt to detect read and write methods with different
 * style variations (e.g., fluent or builder-style).
 * </p>
 * <p>
 * In addition, it supports read-only and write-only methods by default
 * with the short constructor form (no need to pass <code>null</code>
 * as constructor arguments for method or method names).
 * Extra methods are also provided to check for the existence of read or
 * write methods: {@link #isReadable()} and {@link #isWritable()}.
 * Shortcut methods are also provided to set or get a bean value:
 * {@link #readValue(Object bean)} and
 * {@link #writeValue(Object bean, Object value)}.
 * If a property has no read method (write-only), the property type will be
 * derived from the first matching setter encountered with a single parameter.
 * </p>
 * <p>
 * The following are examples of supported variations, in order of precedence
 * (for cases where their might be more than one variation on a class for
 * the same property).
 * </p>
 * {@nx.block #readables
 * <h3>Supported readable method styles (getters)</h3>
 * <ul>
 *   <li><code>Foo getFoo()</code></li>
 *   <li><code>boolean isFoo()</code>
 *   <li><code>Foo foo()</code></li>
 * </ul>
 * }
 *
 * {@nx.block #writables
 * <h3>Supported writable method styles (setters)</h3>
 * <ul>
 *   <li><code>void setFoo(Foo foo)</code></li>
 *   <li><code><i>Self</i> setFoo(Foo foo) //Self is "this" instance</code></li>
 *   <li><code>void foo(Foo foo)</code></li>
 *   <li><code><i>Self</i> foo(Foo foo) //Self is "this" instance</code></li>
 * </ul>
 * }
 *
 * @version 2.0.0
 */
public class FluentPropertyDescriptor extends PropertyDescriptor {

    /**
     * Creates a new property descriptor with the specified write and
     * read method names.
     * When a supplied method name is <code>null</code> or blank, it will
     * try to derive it using a few method name variations, as described
     * in this class documentation.
     * @param propertyName the bean property name
     * @param beanClass the bean class name
     * @param readMethodName optional read method name (getter)
     * @param writeMethodName optional write method name (setter)
     * @throws IntrospectionException problem creating property descriptor
     */
    public FluentPropertyDescriptor(
            String propertyName, Class<?> beanClass,
            String readMethodName, String writeMethodName)
                    throws IntrospectionException {
        super(propertyName,
                readMethod(beanClass, propertyName, readMethodName),
                writeMethod(beanClass, propertyName, writeMethodName));
    }

    /**
     * Creates a new property descriptor, trying to derive the
     * reader and writer methods using a few naming variations, as described
     * in this class documentation.
     * @param propertyName the bean property name
     * @param beanClass the bean class name
     * @throws IntrospectionException problem creating property descriptor
     */
    public FluentPropertyDescriptor(String propertyName, Class<?> beanClass)
            throws IntrospectionException {
        super(propertyName,
                readMethod(beanClass, propertyName, null),
                writeMethod(beanClass, propertyName, null));
    }

    /**
     * Creates a new property descriptor with the specified write and
     * read method. One (and only one) of the methods can be <code>null</code>
     * to have it automatically detected (if it exists).  If both methods
     * are provided, this constructor behaves the same as
     * {@link PropertyDescriptor#PropertyDescriptor(String, Method, Method)}.
     * @param propertyName the bean property name
     * @param readMethod read method
     * @param writeMethod write method
     * @throws IntrospectionException problem creating property descriptor
     * @throws NullPointerException if both read and write methods are
     *     <code>null</code>
     */
    public FluentPropertyDescriptor(
            String propertyName, Method readMethod, Method writeMethod)
                    throws IntrospectionException {
        super(propertyName,
                Optional.ofNullable(readMethod).orElseGet(() -> readMethod(
                        writeMethod.getDeclaringClass(), propertyName, null)),
                Optional.ofNullable(writeMethod).orElseGet(() -> writeMethod(
                        readMethod.getDeclaringClass(), propertyName, null)));
    }

    /**
     * Copy constructor.  If the supplied property descriptor has a
     * <code>null</code> read or write method, it will try to detect one
     * using a few method name variations.
     * @param propertyDescriptor the property descriptor to copy
     * @throws IntrospectionException problem creating property descriptor
     */
    public FluentPropertyDescriptor(PropertyDescriptor propertyDescriptor)
            throws IntrospectionException {
        this(propertyDescriptor.getName(),
                Optional.ofNullable(propertyDescriptor.getReadMethod())
                    .orElseGet(() -> readMethod(
                        propertyDescriptor.getWriteMethod().getDeclaringClass(),
                        propertyDescriptor.getName(),
                        null)),
                Optional.ofNullable(propertyDescriptor.getWriteMethod())
                    .orElseGet(() -> writeMethod(
                        propertyDescriptor.getReadMethod().getDeclaringClass(),
                        propertyDescriptor.getName(),
                        null)));
    }

    /**
     * Gets whether this described property can be read (has a reader method).
     * @return <code>true</code> if it can be read
     * @since 3.0.0
     */
    public boolean isReadable() {
        return getReadMethod() != null;
    }
    /**
     * Gets whether this described property can be written to
     * (has a writer method).
     * @return <code>true</code> if it can be written
     * @since 3.0.0
     */
    public boolean isWritable() {
        return getWriteMethod() != null;
    }

    /**
     * Reads the bean value matching the property descriptor.
     * @param <T> returned value type
     * @param bean bean to get property value from
     * @return a value
     * @since 3.0.0
     */
    public <T> T readValue(Object bean) {
        return BeanUtil.getValue(bean, this);
    }
    /**
     * Writes the bean value matching the property descriptor.
     * @param bean bean to write property to
     * @param value the value to write
     * @since 3.0.0
     */
    public void writeValue(Object bean, Object value) {
        BeanUtil.setValue(bean, getName(), value);
    }

    private static Method readMethod(
            Class<?> beanClass,
            String propertyName,
            String readMethodName) {
        if (StringUtils.isNotBlank(readMethodName)) {
            return MethodUtils.getAccessibleMethod(beanClass, readMethodName);
        }
        return BeanUtil.getReadMethod(beanClass, propertyName);
    }
    private static Method writeMethod(
            Class<?> beanClass,
            String propertyName,
            String writeMethodName) {
        if (StringUtils.isNotBlank(writeMethodName)) {
            var method = BeanUtil.getWriteMethod(
                    beanClass, writeMethodName, null, false);
            if (method == null) {
                // try fluent style
                method = BeanUtil.getWriteMethod(
                        beanClass, writeMethodName, null, true);
            }
            return method;
        }
        return BeanUtil.getWriteMethod(beanClass, propertyName);
    }
}