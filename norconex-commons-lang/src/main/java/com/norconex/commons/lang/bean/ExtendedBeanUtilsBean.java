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
package com.norconex.commons.lang.bean;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.collections4.iterators.SingletonIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;

/**
 * Extends {@link BeanUtilsBean} to support collections and arrays which
 * can be mapped to both collection or array. Also adds necessary
 * converters to ensure conversion
 * of all data types supported by {@link Properties}.
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see Properties#storeToBean(Object)
 */
public class ExtendedBeanUtilsBean extends BeanUtilsBean {

    private static final Logger LOG = LoggerFactory.getLogger(
            ExtendedBeanUtilsBean.class);

    public ExtendedBeanUtilsBean() {
        super(new ExtendedConvertUtilsBean());
    }

    @Override
    public void copyProperty(Object bean, String name, Object value)
            throws IllegalAccessException, InvocationTargetException {
        // Because convert(Object, Class) does not have access to the bean,
        // it cannot find the declared generic type of collections.
        // So we find it here and wrap the supplied bean in a concrete type
        // that can be mapped to a sourceIterable converter.
        PropertyDescriptor pd;
        try {
            pd = PropertyUtils.getPropertyDescriptor(bean, name);
        } catch (NoSuchMethodException e) {
            LOG.warn("No property '{}' found on bean.", name);
            return;
        }

        Object enhancedValue = value;
        Class<?> setterType = pd.getPropertyType();

        if (Collection.class.isAssignableFrom(setterType)) {
            Method m = pd.getWriteMethod();
            if (m == null) {
                LOG.warn("No write method found for property: {}", name);
                return;
            }
            enhancedValue = new IterableWrapper(
                    value, getGenericCollectionType(m));
        } else if (setterType.isArray()) {
            enhancedValue = new IterableWrapper(
                    value, setterType.getComponentType());
        }
        super.copyProperty(bean, name, enhancedValue);
    }

    @Override
    protected Object convert(Object value, Class<?> type) {
        if (value instanceof IterableWrapper) {
            return convertIterable((IterableWrapper) value, type);
        }
        return super.convert(value, type);
    }

    public static Class<?> getGenericCollectionType(Method m) {
        Type[] genericParamTypes = m.getGenericParameterTypes();
        if (ArrayUtils.isEmpty(genericParamTypes)) {
            return null;
        }
        for (Type genericParamType : genericParamTypes) {
            if (genericParamType instanceof ParameterizedType) {
                Type[] params = ((ParameterizedType) genericParamType)
                        .getActualTypeArguments();
                if (ArrayUtils.isNotEmpty(params)) {
                    return (Class<?>) params[0];
                }
            }
        }
        return null;
    }

    // Make sure source and target are compatible
    private Object convertIterable(IterableWrapper w, Class<?> type) {

        Collection<Object> targetCollection = null;
        if (Set.class.isAssignableFrom(type)) {
            targetCollection = new HashSet<>();
        } else {
            // assume Collection/List or Array (converted later)
            targetCollection = new ArrayList<>();
        }

        Iterator<?> it = w.getSourceIterator();
        while (it.hasNext()) {
            Object obj = it.next();
            Object convertedObj = super.convert(obj, w.targetObjectType);
            targetCollection.add(convertedObj);
        }

        if (type.isArray()) {
            Object array = Array.newInstance(
                    w.targetObjectType, targetCollection.size());
            int cnt = 0;
            for (Object obj : targetCollection) {
                Array.set(array, cnt, obj);
                cnt++;
            }
            return array;
        }
        return targetCollection;
    }

    class IterableWrapper {
        private final Object sourceObject;
        private final Class<?> targetObjectType;
        public IterableWrapper(Object sourceObject, Class<?> targetObjectType) {
            super();
            this.sourceObject = sourceObject;
            this.targetObjectType = targetObjectType;
        }
        private Iterator<?> getSourceIterator() {
            if (sourceObject instanceof Collection) {
                return ((Collection<?>) sourceObject).iterator();
            }
            if (sourceObject.getClass().isArray()) {
                return new ArrayIterator<>(sourceObject);
            }
            return new SingletonIterator<>(sourceObject);
        }
    }
}
