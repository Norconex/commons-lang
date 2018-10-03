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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.Statement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.Converter;
import com.norconex.commons.lang.map.Properties;

/**
 * @since 2.0.0
 */
public final class BeanUtil {

    private static final Logger LOG = LoggerFactory.getLogger(BeanUtil.class);

    private BeanUtil() {
        super();
    }

    public static Class<?> getPropertyType(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        try {
            PropertyDescriptor p =
                    new PropertyDescriptor(propertyName, bean.getClass());
            return p.getPropertyType();
        } catch (IntrospectionException | IllegalArgumentException e) {
            throw new BeanException("Could not get type for property \""
                    + propertyName + "\" on bean type \"."
                    + bean.getClass().getCanonicalName() + "\"", e);
        }
    }

    // For collections or others that hold a single parameterized type
    public static Class<?> getPropertyGenericType(
            Class<?> beanClass, String propertyName) {
        if (beanClass == null || propertyName == null) {
            return null;
        }
        try {
            Field field = beanClass.getDeclaredField(propertyName);
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                return (Class<?>)
                        ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            return null;
        } catch (NoSuchFieldException e) {
            throw new BeanException("Could not get generic type for property \""
                    + propertyName + "\" on bean type \"."
                    + beanClass.getCanonicalName() + "\"", e);
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        try {
            PropertyDescriptor p =
                    new PropertyDescriptor(propertyName, bean.getClass());
            return (T) p.getReadMethod().invoke(bean);
        } catch (IntrospectionException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new BeanException("Could not get value for property \""
                    + propertyName + "\" on bean type \"."
                    + bean.getClass().getCanonicalName() + "\"", e);
        }
    }

    public static void setValue(
            Object bean, String propertyName, Object value) {
        Objects.requireNonNull(bean, "bean must not be null");
        Objects.requireNonNull(propertyName, "propertyName must not be null");

        try {
            Statement stmt = new Statement(
                    bean, "set" + StringUtils.capitalize(propertyName), new Object[]{value});
            stmt.execute();
        } catch (Exception e) {
            throw new BeanException("Could not set value \"" + value
                    + "\" for property \"" + propertyName
                    + "\" on bean type \""
                    + bean.getClass().getCanonicalName() + "\".", e);
        }
    }

    public static boolean isSettable(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return false;
        }
        try {
            PropertyDescriptor p =
                    new PropertyDescriptor(propertyName, bean.getClass());
            return p.getWriteMethod() != null;
        } catch (IntrospectionException | IllegalArgumentException e) {
            throw new BeanException("Could not get information for property \""
                    + propertyName + "\" on bean type \""
                    + bean.getClass().getCanonicalName() + "\".", e);
        }
    }
    public static boolean isGettable(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return false;
        }
        try {
            PropertyDescriptor p =
                    new PropertyDescriptor(propertyName, bean.getClass());
            return p.getReadMethod() != null;
        } catch (IntrospectionException | IllegalArgumentException e) {
            throw new BeanException("Could not get information for property \""
                    + propertyName + "\" on bean type \""
                    + bean.getClass().getCanonicalName() + "\".", e);
        }
    }

    public static Map<String, Object> toMap(Object bean) {
        if (bean == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<>();
        try {
            PropertyDescriptor[] descs = Introspector.getBeanInfo(
                    bean.getClass()).getPropertyDescriptors();
            for (PropertyDescriptor desc : descs) {
                Method read = desc.getReadMethod();
                Method write = desc.getWriteMethod();
                if (read != null && write != null) {
                    map.put(desc.getName(), read.invoke(bean));
                }
            }
        } catch (IntrospectionException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new BeanException(
                    "Could not convert bean to map for bean type \""
                    + bean.getClass().getCanonicalName() + "\".", e);
        }
        return map;
    }
    //TODO maybe accept boolean argument for non-case-sensitive keys
    //TODO consider using only 1 of this one vs Properties#loadFromBean
    public static Properties toProperties(
            Object bean, String... ignoredProperties) {
        if (bean == null) {
            return new Properties();
        }

        Properties props = new Properties();
        for (Entry<String, Object> en : toMap(bean).entrySet()) {
            String key = en.getKey();
            Object value = en.getValue();
            if (value == null
                    || ArrayUtils.contains(ignoredProperties, key)) {
                continue;
            }
            if (value.getClass().isArray()) {
                props.put(key, CollectionUtil.toStringList((Object[]) value));
            } else if (value instanceof Collection) {
                props.put(key,
                        CollectionUtil.toStringList((Collection<?>) value));
            } else {
                props.add(key, Converter.convert(value));
            }
        }
        return props;
    }


    //--- Graph-related methods ------------------------------------------------

    public static <T> List<T> find(Object bean, Class<T> type) {
        Objects.requireNonNull(type, "Type cannot be null.");
        List<T> foundList = new ArrayList<>();
        visit(bean, foundList::add, type, new HashSet<>());
        return foundList;
    }

    // do not have to return anything... same as predicate always returning true
    public static void visitAll(Object bean, Consumer<Object> visitor) {
        visit(bean,
                b -> {visitor.accept(b); return true;}, null, new HashSet<>());
    }
    public static <T> void visitAll(
            Object bean, Consumer<T> visitor, Class<T> type) {
        visit(bean,
                b -> {visitor.accept(b); return true;}, type, new HashSet<>());
    }

    public static boolean visit(Object bean, Predicate<Object> visitor) {
        return visit(bean, visitor, null, new HashSet<>());
    }
    public static <T> boolean visit(
            Object bean, Predicate<T> visitor, Class<T> type) {
        return visit(bean, visitor, type, new HashSet<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean visit(Object bean,
            Predicate<T> visitor, Class<T> type, Set<Object> cache) {

        if (bean == null || cache.contains(bean)) {
            return true;
        }
        cache.add(bean);

        if (type == null || type.isInstance(bean)) {
            if (!visitor.test((T) bean)) {
                return false;
            }
        }

        for (Object child : getChildren(bean)) {
            if (child instanceof Map) {
                for (Entry<Object, Object> entry :
                        ((Map<Object, Object>) child).entrySet()) {
                    if (!visit(entry.getKey(), visitor, type, cache)) {
                        return false;
                    }
                    if (!visit(entry.getValue(), visitor, type, cache)) {
                        return false;
                    }
                }
            } else if (child instanceof Collection) {
                for (Object obj : (Collection<Object>) child) {
                    if (!visit(obj, visitor, type, cache)) {
                        return false;
                    }
                }
            } else {
                if (!visit(child, visitor, type, cache)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<Object> getChildren(Object bean) {
        if (bean == null) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>();
        for (PropertyDescriptor desc : getProperties(bean)) {
            final String name = desc.getName();
            if (desc.getReadMethod() != null && !"class".equals(name)) {
                list.add(getValue(bean, name));
            }
        }
        return list;
    }

    // do not have to return anything... same as predicate always returning true
    public static void visitAllProperties(
            Object bean, BiConsumer<Object, PropertyDescriptor> visitor) {
        visitProperties(bean, (b, p) -> {
            visitor.accept(b, p); return true;}, null, new HashSet<>());
    }
    @SuppressWarnings("unchecked")
    public static <T> void visitAllProperties(Object bean,
            BiConsumer<T, PropertyDescriptor> visitor, Class<T> type) {
        visitProperties(bean, (b, p) -> {
            visitor.accept((T) b, p); return true;}, type, new HashSet<>());
    }

    public static boolean visitProperties(
            Object bean, BiPredicate<Object, PropertyDescriptor> visitor) {
        return visitProperties(bean, visitor, null, new HashSet<>());
    }

    public static <T> boolean visitProperties(
            Object bean, BiPredicate<Object, PropertyDescriptor> visitor,
            Class<T> type) {
        return visitProperties(bean, visitor, type, new HashSet<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean visitProperties(Object bean,
            BiPredicate<Object, PropertyDescriptor> visitor,
            Class<T> type, Set<Object> cache) {

        if (bean == null || cache.contains(bean)) {
            return true;
        }
        cache.add(bean);

        if (type == null || type.isInstance(bean)) {
            for (PropertyDescriptor desc : getProperties(bean)) {
                if (!visitor.test(bean, desc)) {
                    return false;
                }
            }
        }

        //to make this a method taking a function
        for (Object child : getChildren(bean)) {
            if (child instanceof Map) {
                for (Entry<Object, Object> entry :
                        ((Map<Object, Object>) child).entrySet()) {
                    if (!visitProperties(
                            entry.getKey(), visitor, type, cache)) {
                        return false;
                    }
                    if (!visitProperties(
                            entry.getValue(), visitor, type, cache)) {
                        return false;
                    }
                }
            } else if (child instanceof Collection) {
                for (Object obj : (Collection<Object>) child) {
                    if (!visitProperties(obj, visitor, type, cache)) {
                        return false;
                    }
                }
            } else {
                if (!visitProperties(child, visitor, type, cache)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<PropertyDescriptor> getProperties(Object bean) {
        return getProperties(bean, false);
    }
    // lenient means to include properties that have just a setter
    // or getter.  This can easily cause trouble in other code.
    // Make public if there is really a need for it, else remove arg.
    private static List<PropertyDescriptor> getProperties(
            Object bean, boolean lenient) {
        try {
            List<PropertyDescriptor> descs = Arrays.asList(
                    Introspector.getBeanInfo(
                            bean.getClass()).getPropertyDescriptors());
            if (!lenient) {
                return descs.stream().filter(pd ->
                    pd.getReadMethod() != null && pd.getWriteMethod() != null
                ).collect(Collectors.toList());
            } else {
                return descs;
            }

        } catch (IntrospectionException e) {
            LOG.error("Cannot get properties of {} instance.",
                    bean.getClass().getName(), e);
            return Collections.emptyList();
        }
    }

    // shallow
    public static <T> void copyPropertiesOverNulls(T target, T source) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.visitProperties(source, (obj, pd) -> {
            if (obj != source) {
                return false;
            }
            String name = pd.getName();
            if (BeanUtil.getValue(target, name) == null) {
                BeanUtil.setValue(target, name,
                        BeanUtil.getValue(source, name));
            }
            return true;
        });
    }

    // shallow
    public static <T> void copyProperties(T target, T source) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.visitProperties(source, (obj, pd) -> {
            if (obj != source) {
                return false;
            }
            String name = pd.getName();
            BeanUtil.setValue(target, name, BeanUtil.getValue(source, name));
            return true;
        });
    }

    // shallow
    public static <T> T clone(T bean) {
        if (bean == null) {
            return bean;
        }
        try {
            @SuppressWarnings("unchecked")
            T newBean = (T) bean.getClass().newInstance();
            copyProperties(newBean, bean);
            return newBean;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeanException("Cannot clone bean.", e);
        }
    }
}