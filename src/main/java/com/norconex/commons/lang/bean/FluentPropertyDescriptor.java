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
package com.norconex.commons.lang.bean;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A property descriptor that that will return write methods for fluent APIs
 * (returning self).
 * @author Pascal Essiembre
 * @version 2.0.0
 */
public class FluentPropertyDescriptor extends PropertyDescriptor {
    public FluentPropertyDescriptor(String propertyName, Class<?> beanClass,
            String readMethodName, String writeMethodName)
            throws IntrospectionException {
        super(propertyName, beanClass, readMethodName, writeMethodName);
    }
    public FluentPropertyDescriptor(String propertyName, Class<?> beanClass)
            throws IntrospectionException {
        super(propertyName, beanClass);
    }
    public FluentPropertyDescriptor(String propertyName, Method readMethod,
            Method writeMethod) throws IntrospectionException {
        super(propertyName, readMethod, writeMethod);
    }
    public FluentPropertyDescriptor(PropertyDescriptor propertyDescriptor)
            throws IntrospectionException {
        this(propertyDescriptor.getName(),
                propertyDescriptor.getReadMethod(),
                propertyDescriptor.getWriteMethod());
    }

    @Override
    public synchronized Method getWriteMethod() {
        Class<?> beanClass = super.getReadMethod().getDeclaringClass();
        Method writeMethod = super.getWriteMethod();
        // if not found the traditional way, could be fluent.
        if (writeMethod == null) {
            Class<?> type = getPropertyType();
            Class<?>[] args = (type == null)
                    ? null : new Class<?>[] { type };
            String writeMethodName =
                    "set" + StringUtils.capitalize(getName());
            try {
                writeMethod = beanClass.getMethod(writeMethodName, args);
            } catch (NoSuchMethodException | SecurityException e ) {
                return null;
            } catch (NullPointerException e) {
                return null;
            }
            if (writeMethod != null &&
                    !writeMethod.getReturnType().equals(beanClass)) {
                writeMethod = null;
            }
            try {
                setWriteMethod(writeMethod);
            } catch (IntrospectionException e) {
                // swallow
            }
        }
        return writeMethod;
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}