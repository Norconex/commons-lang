/* Copyright 2018-2023 Norconex Inc.
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

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.ObjectUtils.Null;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.GenericConverter;
import com.norconex.commons.lang.map.Properties;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Bean/object utilities.
 * </p>
 *
 * @since 2.0.0
 * @see FluentPropertyDescriptor
 */
@Slf4j
public final class BeanUtil {

    private BeanUtil() {}


    //--------------------------------------------------------------------------
    // Bean methods
    //--------------------------------------------------------------------------

    /**
     * <p>
     * Gets a list of fluent property descriptors from the supplied bean.
     * Only beans and properties meeting these conditions will have their
     * property descriptors extracted:
     * </p>
     * <ul>
     *   <li>Bean class must have a public, no-argument constructor</li>
     *   <li>Property field must be private</li>
     *   <li>Property must have at least one public read or write method.</li>
     * </ul>
     * <p>
     * If there is an error in retrieving a properties, it will be
     * silently skipped.
     * </p>
     * <p>
     * Contrary to regular Java beans, the supplied class does not have
     * to implement {@link Serializable}.
     * </p>
     * <p>
     * Since 3.0.0, properties descriptors with read-only or write-only methods
     * are supported and method name variations will be used to detect
     * read and write methods.
     * </p>
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     *
     * @param bean the bean to get property descriptors from
     * @return list of property descriptors or an empty list if the supplied
     *     bean does not qualify or is <code>null</code>.
     */
    public static List<FluentPropertyDescriptor> getPropertyDescriptors(
            Object bean) {
        if (bean == null) {
            return Collections.emptyList();
        }
        return getPropertyDescriptors(bean.getClass());
    }

    /**
     * <p>
     * Gets a list of fluent property descriptors from the supplied bean class.
     * Only classes and properties meeting these conditions will have their
     * property descriptors extracted:
     * </p>
     * <ul>
     *   <li>Bean class must have a public, no-argument constructor</li>
     *   <li>Property field must be private</li>
     *   <li>Property must have at least one public read or write method.</li>
     * </ul>
     * <p>
     * If there is an error in retrieving a properties, it will be silently
     * skipped.
     * </p>
     * <p>
     * Contrary to regular Java beans, the supplied class does not have
     * to implement {@link Serializable}.
     * </p>
     * <p>
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * </p>
     *
     * @param beanClass the bean class to get property descriptors from
     * @return list of property descriptors or an empty list if the supplied
     *     bean does not qualify or is <code>null</code>.
     * @since 3.0.0
     */
    public static List<FluentPropertyDescriptor> getPropertyDescriptors(
            Class<?> beanClass) {
        if (beanClass == null
                || String.class.equals(beanClass)
                || ClassUtils.isPrimitiveOrWrapper(beanClass)) {
            return Collections.emptyList();
        }
        Constructor<?> constructor = null;
        try {
            constructor = beanClass.getConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            // swallow
        }
        if (constructor == null) {
            LOG.debug("Could not find an accessible public no-arg "
                    + "constructor on {}.", beanClass);
            return Collections.emptyList();
        }
        return FieldUtils.getAllFieldsList(beanClass).stream()
            .filter(f -> Modifier.isPrivate(f.getModifiers())
                    && !Modifier.isNative(f.getModifiers())
                    && !Modifier.isStatic(f.getModifiers()))
            .map(f -> doGetFluentPropertyDescriptor(beanClass, f))
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Performs shallow copies of source object property values
     * over the corresponding target properties whose values are
     * <code>null</code>.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param <T> the type of source and target objects
     * @param target the target object
     * @param source the source object
     */
    public static <T> void copyPropertiesOverNulls(T target, T source) {
        if (source == null || target == null) {
            return;
        }
        visitProperties(source, (obj, pd) -> {
            if (obj != source) {
                return false;
            }
            var name = pd.getName();
            // only copy if target property is null and is writable
            if (getValue (target, name) == null && isWritable(target, name)) {
                setValue(target, name, getValue(source, name));
            }
            return true;
        });
    }

    /**
     * Performs shallow copies of source object property values
     * over the corresponding target properties.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param <T> the type of source and target objects
     * @param target the target object
     * @param source the source object
     */
    public static <T> void copyProperties(T target, T source) {
        if (source == null || target == null) {
            return;
        }
        BeanUtil.visitProperties(source, (obj, pd) -> {
            if (obj != source) {
                return false;
            }
            var name = pd.getName();
            BeanUtil.setValue(target, name, BeanUtil.getValue(source, name));
            return true;
        });
    }

    /**
     * Clones the given object by creating a new instance of the same type and
     * performing shallow copies of the given object property values
     * over the corresponding target properties of the new object.
     * The object type must have an empty constructor.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param <T> the type of the object being cloned
     * @param bean the object being cloned
     * @return the cloned object
     * @throws BeanException error cloning the bean
     */
    public static <T> T clone(T bean) {
        if (bean == null) {
            return bean;
        }
        try {
            // If a primitive or String (immutable), return as is.
            if (bean instanceof String
                    || ClassUtils.isPrimitiveOrWrapper(bean.getClass())) {
                return bean;
            }
            @SuppressWarnings("unchecked")
            var newBean =
                    (T) bean.getClass().getDeclaredConstructor().newInstance();
            copyProperties(newBean, bean);
            return newBean;
        } catch (Exception e) {
            throw new BeanException("Cannot clone bean.", e);
        }
    }

    /**
     * Checks for differences between two beans and returns different
     * property values as a formatted string.
     *
     * @param <T> the type of the objects being checked for differences
     * @param bean1 the first object
     * @param bean2 the second object
     * @return a formatted string highlighting any differences
     */
    public static <T> String diff(T bean1, T bean2) {
        //MAYBE: Visit all properties keeping trail, or nesting level
        var b1 = graphLeavesAsBag(bean1);
        var b2 = graphLeavesAsBag(bean2);

        Collection<String> left = CollectionUtils.removeAll(b1, b2);
        Collection<String> right = CollectionUtils.removeAll(b2, b1);

        Bag<String> allDiffs = new TreeBag<>((s1, s2) -> {
            // compare keys
            var comp = substringBefore(s1.substring(1), "=")
                    .compareTo(substringBefore(s2.substring(1), "="));
            if (comp == 0) {
                // compare left or right
                comp = s1.substring(0, 1).compareTo(s2.substring(0, 1));
            }
            if (comp == 0) {
                // compare value
                comp = substringAfter(s1, "=")
                        .compareTo(substringAfter(s2, "="));
            }
            return comp;
        });

        for (String diff : left) {
            allDiffs.add("< " + diff);
        }
        for (String diff : right) {
            allDiffs.add("> " + diff);
        }
        return StringUtils.join(allDiffs, "\n");
    }

    /**
     * Gets child property beans of the supplied objects (nested beans).
     * For a bean object to be returned as a child, it needs to be accessible
     * via a read method (getter).
     * @param bean the object to obtain child beans from
     * @return list of child beans
     */
    public static List<Object> getChildren(Object bean) {
        if (bean == null) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>();
        for (PropertyDescriptor desc : getPropertyDescriptors(bean)) {
            final var name = desc.getName();
            if (desc.getReadMethod() != null && !"class".equals(name)) {
                list.add(getValue(bean, name));
            }
        }
        return list;
    }

    /**
     * Gets whether the supplied object has any children (nested beans).
     * That is, it has at least one property with a read method.
     * @param bean the bean to test for children
     * @return <code>true</code> if the object has child beans
     */
    public static boolean hasChildren(Object bean) {
        if (bean == null) {
            return false;
        }
        for (PropertyDescriptor desc : getPropertyDescriptors(bean)) {
            final var name = desc.getName();
            if (desc.getReadMethod() != null && !"class".equals(name)) {
                return true;
            }
        }
        return false;
    }


    //--------------------------------------------------------------------------
    // Property methods
    //--------------------------------------------------------------------------

    /**
     * Gets the type of specified object property based on its
     * property descriptor.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param bean the object
     * @param propertyName the property name
     * @return object property type or <code>null</code> if not a bean property
     *     (e.g., has no read or write method)
     * @throws BeanException on error obtaining the property type
     */
    public static Class<?> getPropertyType(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        try {
            var p = new FluentPropertyDescriptor(propertyName, bean.getClass());
            if (p.getReadMethod() == null && p.getWriteMethod() == null) {
                return null;
            }
            return p.getPropertyType();
        } catch (IntrospectionException | IllegalArgumentException e) {
            throw accessorException("get property type",
                    propertyName, bean.getClass().getCanonicalName(), e);
        }
    }

    /**
     * Gets the generic type of a class property. Useful with collections or
     * others classes holding a single single parameterized type.
     * @param beanClass the object class
     * @param propertyName the property name
     * @return object property generic type
     * @throws BeanException on error obtaining the property generic type
     */
    public static Class<?> getPropertyGenericType(
            Class<?> beanClass, String propertyName) {
        if (beanClass == null || propertyName == null) {
            return null;
        }
        try {
            var field = beanClass.getDeclaredField(propertyName);
            var type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                return (Class<?>)
                        ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            return null;
        } catch (NoSuchFieldException e) {
            throw accessorException("get generic type",
                    propertyName, beanClass.getCanonicalName(), e);
        }
    }

    /**
     * Gets the read method (getter) for a bean property.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param bean bean object
     * @param propertyName property name
     * @return read method, or {@link Null} if property is write-only
     * @since 3.0.0
     */
    public static Method getReadMethod(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        return getReadMethod(bean.getClass(), propertyName);
    }

    /**
     * Gets the read method (getter) for a bean property.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param beanClass object class
     * @param propertyName property name
     * @return read method, or {@link Null} if property is write-only
     * @since 3.0.0
     */
    public static Method getReadMethod(
            Class<?> beanClass, String propertyName) {
        if (beanClass == null || propertyName == null) {
            return null;
        }

        var readMethod = MethodUtils.getAccessibleMethod(
                beanClass, "get" + StringUtils.capitalize(propertyName));

        // try regular way, boolean
        if (readMethod == null) {
            readMethod = MethodUtils.getAccessibleMethod(
                    beanClass,
                    "is" + StringUtils.capitalize(propertyName));
            if (readMethod != null
                    && !readMethod.getReturnType().equals(Boolean.class)
                    && !readMethod.getReturnType().equals(Boolean.TYPE)) {
                readMethod = null;
            }
        }

        // if not found the standard way, try build-style (as is)
        if (readMethod == null) {
            readMethod = MethodUtils.getAccessibleMethod(
                    beanClass, propertyName);
        }

        return readMethod;
    }

    /**
     * Gets the write method (setter) for a bean property.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param bean object
     * @param propertyName property name
     * @return write method, or <code>null</code> if property is read-only
     * @since 3.0.0
     */
    public static Method getWriteMethod(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        return getWriteMethod(bean.getClass(), propertyName);
    }

    /**
     * Gets the write method (setter) for a bean property.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param beanClass object class
     * @param propertyName property name
     * @return write method, or {@link Null} if property is read-only
     * @since 3.0.0
     */
    public static Method getWriteMethod(
            Class<?> beanClass, String propertyName) {
        return getWriteMethod(beanClass, propertyName, null);
    }
    /**
     * Gets the write method (setter) for a bean property.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param beanClass object class
     * @param propertyName property name
     * @param propertyType property type, or <code>null</code> if to be detected
     * @return write method, or {@link Null} if property is read-only
     * @since 3.0.0
     */
    public static Method getWriteMethod(
            Class<?> beanClass, String propertyName, Class<?> propertyType) {

        if (beanClass == null || propertyName == null) {
            return null;
        }

        Method writeMethod;

        var methodName = "set" + StringUtils.capitalize(propertyName);

        // try regular way
        writeMethod = getWriteMethod(
                beanClass, methodName, propertyType, false);

        // try fluent-style, returning self
        if (writeMethod == null) {
            writeMethod = getWriteMethod(
                    beanClass, methodName, propertyType, true);
        }

        // try build-style, returning void
        if (writeMethod == null) {
            writeMethod = getWriteMethod(
                    beanClass, propertyName, propertyType, false);
        }

        // try build-style, returning self
        if (writeMethod == null) {
            writeMethod = getWriteMethod(
                    beanClass, propertyName, propertyType, true);
        }

        return writeMethod;
    }

    /**
     * Gets the value of a bean property. Returns <code>null</code> if the
     * property has no read method.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param <T> return value type
     * @param bean the object
     * @param propertyName the property name
     * @return object property value or <code>null</code> if no read method
     * @throws BeanException on error obtaining the property value
     * @see BeanUtil#isReadable(Object, String)
     */
    public static <T> T getValue(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        return getValue(
                getReadMethod(bean.getClass(), propertyName),
                bean,
                propertyName);
    }

    /**
     * Gets the value of a bean property based on a {@link PropertyDescriptor}.
     * This is a convenience method equivalent to invoking the method obtained
     * with {@link PropertyDescriptor#getReadMethod()} when not read-only.
     * @param <T> return value type
     * @param bean the object
     * @param propertyDescriptor the property descriptor
     * @return object property value
     * @throws BeanException on error obtaining the property value
     */
    public static <T> T getValue(
            Object bean, PropertyDescriptor propertyDescriptor) {
        if (bean == null || propertyDescriptor == null) {
            return null;
        }
        return getValue(
                propertyDescriptor.getReadMethod(),
                bean,
                propertyDescriptor.getName());
    }

    /**
     * Gets whether a given bean property has a read method for it
     * (getter).
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param bean the object
     * @param propertyName the property name
     * @return <code>true</code> if the object property as a getter method
     * @throws BeanException on error obtaining property information
     * @since 3.0.0, replaces now
     *      deprecated {@link #isGettable(Object, String)}
     */
    public static boolean isReadable(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return false;
        }
        return isReadable(bean.getClass(), propertyName);
    }

    /**
     * Gets whether a given class property has a read method for it
     * (getter).
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param beanClass a bean class
     * @param propertyName the property name
     * @return <code>true</code> if the object property as a getter method
     * @throws BeanException on error obtaining property information
     * @since 3.0.0
     */
    public static boolean isReadable(Class<?> beanClass, String propertyName) {
        if (beanClass == null || propertyName == null) {
            return false;
        }
        return getReadMethod(beanClass, propertyName) != null;
    }

    /**
     * Gets whether a given bean property has a getter method for it.
     * @param bean the object
     * @param propertyName the property name
     * @return <code>true</code> if the object property as a getter method
     * @throws BeanException on error obtaining property information
     * @deprecated Use {@link #isReadable(Object, String)} instead
     */
    @Deprecated(since = "3.0.0")
    public static boolean isGettable( //NOSONAR
            Object bean, String propertyName) {
        return isReadable(bean, propertyName);
    }

    /**
     * Sets the value of a bean property only if it is writable. Otherwise
     * does nothing.
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param bean the object
     * @param propertyName the property name
     * @param value object property value
     * @throws BeanException on error obtaining the property value
     * @see #isWritable(Object, String)
     */
    public static void setValue(
            @NonNull Object bean, @NonNull String propertyName, Object value) {
        try {
            var method = getWriteMethod(bean, propertyName);
            if (method != null) {
                method.invoke(bean, value);
            }
        } catch (Exception e) {
            throw accessorException("set value",
                    propertyName, bean.getClass().getCanonicalName(), e);
        }
    }

    /**
     * Gets whether a given bean property has a write method for it (setter).
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param bean the object
     * @param propertyName the property name
     * @return <code>true</code> if the object property as a setter method
     * @throws BeanException on error obtaining property information
     * @deprecated Use {@link #isWritable(Object, String)} instead.
     */
    @Deprecated(since = "3.0.0")
    public static boolean isSettable( //NOSONAR
            Object bean, String propertyName) {
        return isWritable(bean, propertyName);
    }

    /**
     * Gets whether a given bean property has a write method for it (setter).
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param bean the object
     * @param propertyName the property name
     * @return <code>true</code> if the object property as a write method
     * @throws BeanException on error obtaining property information
     * @since 3.0.0
     */
    public static boolean isWritable(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return false;
        }
        return isWritable(bean.getClass(), propertyName);
    }

    /**
     * Gets whether a given bean property has a write method for it (setter).
     * Supports method name variations for accessors.
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#writables}
     * {@nx.include com.norconex.commons.lang.bean.FluentPropertyDescriptor#readables}
     * @param beanClass bean class
     * @param propertyName property name
     * @return <code>true</code> if the object property as a write method
     * @throws BeanException on error obtaining property information
     * @since 3.0.0
     */
    public static boolean isWritable(Class<?> beanClass, String propertyName) {
        if (beanClass == null || propertyName == null) {
            return false;
        }
        return getWriteMethod(beanClass, propertyName) != null;
    }


    //--------------------------------------------------------------------------
    // Map/Properties methods
    //--------------------------------------------------------------------------

    /**
     * Converts a bean to a map where the keys are the object property names
     * and values are the object property values.
     * @param bean the object to convert to a map.
     * @return the converted a map
     * @throws BeanException on error converting bean to map
     */
    public static Map<String, Object> toMap(Object bean) {
        if (bean == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<>();
        try {
            var descs = Introspector.getBeanInfo(
                    bean.getClass()).getPropertyDescriptors();
            for (PropertyDescriptor desc : descs) {
                var read = desc.getReadMethod();
                var write = desc.getWriteMethod();
                if (read != null && write != null) {
                    map.put(desc.getName(), read.invoke(bean));
                }
            }
        } catch (IntrospectionException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new BeanException(String.format(
                    "Could not convert bean to map for bean type \"%s\".",
                    bean.getClass().getCanonicalName()), e);
        }
        return map;
    }

    /**
     * Converts a bean to a {@link Properties} instance where the keys are
     * the object property names and values are the object property values,
     * converted to string, with support for arrays and collections.
     * @param bean the object to convert to properties.
     * @param ignoredProperties properties to skip (not converted)
     * @return the converted properties
     * @throws BeanException on error converting bean to properties
     */
    public static Properties toProperties(
            Object bean, String... ignoredProperties) {
        //MAYBE: accept boolean argument for non-case-sensitive keys
        // consider using only 1 of this one vs Properties#loadFromBean
        if (bean == null) {
            return new Properties();
        }

        var props = new Properties();
        for (Entry<String, Object> en : toMap(bean).entrySet()) {
            var key = en.getKey();
            var value = en.getValue();
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
                props.add(key, GenericConverter.convert(value));
            }
        }
        return props;
    }


    //--------------------------------------------------------------------------
    // Visitor/Graph methods
    //--------------------------------------------------------------------------

    /**
     * Finds a list of objects matching the given type by traversing
     * the supplied object graph (i.e., itself and its child objects,
     * recursively).
     * @param <T> the type of objects to find
     * @param bean the bean on which to find matches
     * @param type class instance of the type to find
     *     (must not be <code>null</code>)
     * @return a list of objects matching the supplied type
     */
    public static <T> List<T> find(Object bean, @NonNull Class<T> type) {
        List<T> foundList = new ArrayList<>();
        doVisit(VisitArgs.<T>beanVisitor()
                .bean(bean)
                .visitor(foundList::add)
                .type(type)
                .build());
        return foundList;
    }

    /**
     * Visits all objects by traversing the supplied object graph
     * (i.e., itself and its child objects, recursively).
     * @param bean the bean to visit
     * @param visitor a consumer invoked for all objects visited
     */
    public static void visitAll(Object bean, Consumer<Object> visitor) {
        // does not return anything. same as predicate always returning true
        doVisit(VisitArgs.beanVisitor()
                .bean(bean)
                .visitor(b -> {visitor.accept(b); return true;})
                .build());
    }

    /**
     * Visits all objects matching the given type by traversing the
     * supplied object graph
     * (i.e., itself and its child objects, recursively).
     * @param <T> the type of objects to visit
     * @param bean the bean to visit
     * @param visitor a consumer invoked for all objects visited
     * @param type class instance of the type to find
     */
    public static <T> void visitAll(
            Object bean, Consumer<T> visitor, Class<T> type) {
        doVisit(VisitArgs.<T>beanVisitor()
                .bean(bean)
                .visitor(b -> {visitor.accept(b); return true;})
                .type(type)
                .build());
    }

    /**
     * Visits objects by traversing the supplied object graph
     * (i.e., itself and its child objects, recursively) for as long
     * as the predicate returns <code>true</code>. The visiting stops
     * the moment a <code>false</code> value is returned.
     * @param bean the bean to visit
     * @param visitor a predicate invoked for all objects visited
     * @return <code>true</code> if the visit was complete (all predicate
     *     invocations returned <code>true</code>).
     */
    public static boolean visit(Object bean, Predicate<Object> visitor) {
        return doVisit(VisitArgs.beanVisitor()
                .bean(bean)
                .visitor(visitor)
                .build());
    }

    /**
     * Visits objects matching the given type by traversing the
     * supplied object graph
     * (i.e., itself and its child objects, recursively) for as long
     * as the predicate returns <code>true</code>. The visiting stops
     * the moment a <code>false</code> value is returned.
     * @param <T> the type of objects to visit
     * @param bean the bean to visit
     * @param visitor a predicate invoked for all objects visited
     * @param type class instance of the type to find
     * @return <code>true</code> if the visit was complete (all predicate
     *     invocations returned <code>true</code>).
     */
    public static <T> boolean visit(
            Object bean, Predicate<T> visitor, Class<T> type) {
        return doVisit(VisitArgs.<T>beanVisitor()
                .bean(bean)
                .visitor(visitor)
                .type(type)
                .build());
    }

    /**
     * Visits all properties of the supplied object by traversing its object
     * graph (i.e., the supplied object properties and its child object
     * properties, recursively).
     * For each properties, the supplied visitor takes both the property
     * descriptor and the object instance on which the property was found.
     * @param bean the bean on which visit properties
     * @param visitor a consumer invoked for all properties visited
     */
    public static void visitAllProperties(
            Object bean, BiConsumer<Object, PropertyDescriptor> visitor) {
        // does not return anything... same as predicate always returning true
        doVisitProperties(VisitArgs.propertyVisitor()
                .bean(bean)
                .visitor((b, p) -> {visitor.accept(b, p); return true;})
                .build());
    }

    /**
     * Visits all properties of objects matching the given type
     * starting with the supplied object, traversing its object
     * graph (i.e., the supplied object properties and its child object
     * properties, recursively).
     * For each properties, the supplied visitor takes both the property
     * descriptor and the object instance on which the property was found.
     * @param <T> the type of objects to visit
     * @param bean the bean on which visit properties
     * @param visitor a consumer invoked for all properties visited
     * @param type class instance of the type to find
     */
    @SuppressWarnings("unchecked")
    public static <T> void visitAllProperties(Object bean,
            BiConsumer<T, PropertyDescriptor> visitor, Class<T> type) {
        doVisitProperties(VisitArgs.<T>propertyVisitor()
                .bean(bean)
                .visitor((b, p) -> {visitor.accept((T) b, p); return true;})
                .type(type)
                .build());
    }

    /**
     * Visits all properties of the supplied object by traversing its object
     * graph (i.e., the supplied object properties and its child object
     * properties, recursively) for as long as the predicate returns
     * <code>true</code>. The visiting stops
     * the moment a <code>false</code> value is returned.
     * For each properties, the supplied visitor takes both the property
     * descriptor and the object instance on which the property was found.
     * @param bean the bean to visit
     * @param visitor a predicate invoked for all properties visited
     * @return <code>true</code> if the visit was complete (all predicate
     *     invocations returned <code>true</code>).
     */
    public static boolean visitProperties(
            Object bean, BiPredicate<Object, PropertyDescriptor> visitor) {
        return doVisitProperties(VisitArgs.propertyVisitor()
                .bean(bean)
                .visitor(visitor)
                .build());
    }

    /**
     * Visits all properties of objects matching the given type
     * starting with the supplied object, traversing its object
     * graph (i.e., the supplied object properties and its child object
     * properties, recursively) for as long as the predicate returns
     * <code>true</code>. The visiting stops
     * the moment a <code>false</code> value is returned.
     * For each properties, the supplied visitor takes both the property
     * descriptor and the object instance on which the property was found.
     * @param <T> the type of objects to visit
     * @param bean the bean to visit
     * @param visitor a predicate invoked for all properties visited
     * @param type class instance of the type to find
     * @return <code>true</code> if the visit was complete (all predicate
     *     invocations returned <code>true</code>).
     */
    public static <T> boolean visitProperties(
            Object bean, BiPredicate<Object, PropertyDescriptor> visitor,
            Class<T> type) {
        return doVisitProperties(VisitArgs.<T>propertyVisitor()
                .bean(bean)
                .visitor(visitor)
                .type(type)
                .build());
    }


    //--------------------------------------------------------------------------
    // Private/package methods
    //--------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <T> boolean doVisit(VisitArgs<Predicate<T>, T> args) {
        if (args.isBeanNullOrCached()) {
            return true;
        }
        if (args.isTypeNullOrBeanInstance()
                && !args.visitor.test((T) args.bean)) {
            return false;
        }
        
        if (args.visitIfMap(BeanUtil::doVisitMap)
                && args.visitIfCollection(BeanUtil::doVisitCollection)) {
            for (Object child : getChildren(args.bean)) {
                var childArgs = args.withBean(child);
                if (!childArgs.visitIfMap(BeanUtil::doVisitMap)
                        || !childArgs.visitIfCollection(BeanUtil::doVisitCollection)
                        || !doVisit(childArgs)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private static <T> boolean doVisitMap(
            Map<Object, Object> map,
            VisitArgs<Predicate<T>, T> args) {
        for (Entry<Object, Object> entry : map.entrySet()) {
            if (!doVisit(args.withBean(entry.getKey()))
                    || !doVisit(args.withBean(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private static <T> boolean doVisitCollection(
            Collection<Object> collection,
            VisitArgs<Predicate<T>, T> args) {
        for (Object obj : collection) {
            if (!doVisit(args.withBean(obj))) {
                return false;
            }
        }
        return true;
    }

    private static <T> boolean doVisitProperties(
            VisitArgs<BiPredicate<Object, PropertyDescriptor>, T> args) {
        if (args.isBeanNullOrCached()) {
            return true;
        }

        if (args.isTypeNullOrBeanInstance()) {
            for (PropertyDescriptor desc : getPropertyDescriptors(args.bean)) {
                if (!args.visitor.test(args.bean, desc)) {
                    return false;
                }
            }
        }

        if (args.visitIfMap(BeanUtil::doVisitPropertiesMap)
                && args.visitIfCollection(
                        BeanUtil::doVisitPropertiesCollection)) {
            for (Object child : getChildren(args.bean)) {
                var childArgs = args.withBean(child);
                if (!childArgs.visitIfMap(BeanUtil::doVisitPropertiesMap)
                        || !childArgs.visitIfCollection(
                                BeanUtil::doVisitPropertiesCollection)
                        || !doVisitProperties(childArgs)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private static <T> boolean doVisitPropertiesMap(
            Map<Object, Object> map,
            VisitArgs<BiPredicate<Object, PropertyDescriptor>, T> args) {
        for (Entry<Object, Object> entry : map.entrySet()) {
            if (!doVisitProperties(args.withBean(entry.getKey()))
                    || !doVisitProperties(args.withBean(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }
    private static <T> boolean doVisitPropertiesCollection(
            Collection<Object> collection,
            VisitArgs<BiPredicate<Object, PropertyDescriptor>, T> args) {
        for (Object obj : collection) {
            if (!doVisitProperties(args.withBean(obj))) {
                return false;
            }
        }
        return true;
    }

    private static Bag<String> graphLeavesAsBag(Object bean) {
        var hashCode = "#hashCode:";
        Bag<String> bag = new HashBag<>();
        if (bean != null) {
            bag.add(bean.getClass().getSimpleName()
                    + hashCode + bean.hashCode());
        }
        visitAllProperties(bean, (o, p) -> {
            var key = o.getClass().getSimpleName()
                    + "." + p.getName() + " = ";
            var value = getValue(o, p);
            var line = key;
            if (!hasChildren(value)) {
                line += Objects.toString(value);
                if (value != null) {
                    line += hashCode + value.hashCode();
                }
            } else if (value == null) {
                line += "<null>";
            } else {
                if (value.getClass().isArray()) {
                    line += "<Array";
                } else if (value instanceof Collection) {
                    line += "<Collection";
                } else if (value instanceof Map) {
                    line += "<Map";
                } else {
                    line += "<Object";
                }
                line += hashCode + value.hashCode() + ">";
            }
            bag.add(line);
        });
        return bag;
    }

    private static BeanException accessorException(
            String action, String property, String className, Exception e) {
        return new BeanException(String.format(
                "Could not %s for property \"%s\" on bean type \"%s\".",
                action, property, className), e);
    }

    static Method getWriteMethod(
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

    private static FluentPropertyDescriptor doGetFluentPropertyDescriptor(
            @NonNull Class<?> beanClass, @NonNull Field field) {
        try {
            var readMethod = getReadMethod(beanClass, field.getName());
            var writeMethod = getWriteMethod(
                    beanClass, field.getName(), field.getType());
            if (ObjectUtils.allNull(readMethod, writeMethod)) {
                LOG.debug("""
                    Cannot get descriptor for property "{}" of bean\s\
                    class "{}". Property has no read or\s\
                    write methods.""",
                        field.getName(), beanClass.getName());
                return null;
            }
            return new FluentPropertyDescriptor(
                    field.getName(), readMethod, writeMethod);
        } catch (IntrospectionException e) {
            LOG.debug("Cannot get descriptor for property \"{}\" of bean "
                    + "class \"{}\".",
                    field.getName(), beanClass.getName(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(Method method, Object bean, String property) {
        // We don't attempt to get the value if transient
        if (method == null
                || bean == null
                || method.getAnnotation(Transient.class) != null) {
            return null;
        }
        try {
            var value = (T) method.invoke(bean);
            // If it equals to self, we do not consider it a bean property.
            // Plus, it may be a source of infinite loop, so we do not return
            // it.
            if (Objects.equals(bean, value)) {
                return null;
            }
            return value;
        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw accessorException("get value",
                    property, bean.getClass().getCanonicalName(), e);
        }
    }

    //--- Inner classes --------------------------------------------------------

    @Builder(access = AccessLevel.PRIVATE)
    private static class VisitArgs<V, T> {
        private final Object bean;
        private final V visitor;
        private final Class<T> type;
        @Builder.Default
        private Set<Object> cache =
                Collections.newSetFromMap(new IdentityHashMap<>());

        static <T> VisitArgsBuilder<Predicate<T>, T> beanVisitor() {
            return new VisitArgsBuilder<>();
        }
        static <T> VisitArgsBuilder<
                BiPredicate<Object, PropertyDescriptor>, T> propertyVisitor() {
            return new VisitArgsBuilder<>();
        }

        VisitArgs<V, T> withBean(Object bean) {
            return new VisitArgs<>(bean, visitor, type, cache);
        }

        boolean isBeanNullOrCached() {
            return bean == null || !cache.add(bean);
        }
        boolean isTypeNullOrBeanInstance() {
            return type == null || type.isInstance(bean);
        }

        @SuppressWarnings("unchecked")
        boolean visitIfMap(
                BiPredicate<Map<Object, Object>, VisitArgs<V, T>> predicate) {
            if (bean instanceof Map) {
                return predicate.test((Map<Object, Object>) bean, this);
            }
            // continue if not a map
            return true;
        }
        @SuppressWarnings("unchecked")
        boolean visitIfCollection(
                BiPredicate<Collection<Object>, VisitArgs<V, T>> predicate) {
            if (bean instanceof Collection) {
                return predicate.test((Collection<Object>) bean, this);
            }
            // continue if not a collection
            return true;
        }
    }
}