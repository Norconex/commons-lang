/* Copyright 2018-2021 Norconex Inc.
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
import java.beans.Statement;
import java.beans.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
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


    public static <T> T getValue(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        try {
            return getValue(bean, new FluentPropertyDescriptor(
                    propertyName, bean.getClass()));
        } catch (IntrospectionException e) {
            throw new BeanException("Could not get value for property \""
                    + propertyName + "\" on bean type \"."
                    + bean.getClass().getCanonicalName() + "\"", e);
        }
    }
    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object bean, PropertyDescriptor property) {
        if (bean == null || property == null) {
            return null;
        }
        try {
            // If child is equal to parent, it may be a source of
            // infinite loop, so we do not return it.  We do the same
            // if the property is transient.
            Method getter = property.getReadMethod();
            if (getter.getAnnotation(Transient.class) != null) {
                return null;
            }
            T value = (T) getter.invoke(bean);
            if (Objects.equals(bean, value)) {
                return null;
            }
            return value;
        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new BeanException("Could not get value for property \""
                    + property.getName() + "\" on bean type \"."
                    + bean.getClass().getCanonicalName() + "\"", e);
        }
    }

    public static void setValue(
            Object bean, String propertyName, Object value) {
        Objects.requireNonNull(bean, "bean must not be null");
        Objects.requireNonNull(propertyName, "propertyName must not be null");

        try {
            Statement stmt = new Statement(
                    bean, "set" + StringUtils.capitalize(propertyName),
                    new Object[]{value});
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
            LOG.trace("Could not get information for property \"{}\" on "
                    + "bean type \"{}\".",
                    propertyName, bean.getClass().getCanonicalName(), e);
            return false;
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
            LOG.trace("Could not get information for property \"{}\" on "
                    + "bean type \"{}\".",
                    propertyName, bean.getClass().getCanonicalName(), e);
            return false;
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
    //MAYBE: accept boolean argument for non-case-sensitive keys
    // consider using only 1 of this one vs Properties#loadFromBean
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
        visit(bean, foundList::add, type, newCache());
        return foundList;
    }

    // do not have to return anything... same as predicate always returning true
    public static void visitAll(Object bean, Consumer<Object> visitor) {
        visit(bean,
                b -> {visitor.accept(b); return true;}, null, newCache());
    }
    public static <T> void visitAll(
            Object bean, Consumer<T> visitor, Class<T> type) {
        visit(bean,
                b -> {visitor.accept(b); return true;}, type, newCache());
    }

    public static boolean visit(Object bean, Predicate<Object> visitor) {
        return visit(bean, visitor, null, newCache());
    }
    public static <T> boolean visit(
            Object bean, Predicate<T> visitor, Class<T> type) {
        return visit(bean, visitor, type, newCache());
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
    private static Set<Object> newCache() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public static List<Object> getChildren(Object bean) {
        if (bean == null) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>();
        for (PropertyDescriptor desc : getPropertyDescriptors(bean)) {
            final String name = desc.getName();
            if (desc.getReadMethod() != null && !"class".equals(name)) {
                list.add(getValue(bean, name));
            }
        }
        return list;
    }

    public static boolean hasChildren(Object bean) {
        if (bean == null) {
            return false;
        }
        for (PropertyDescriptor desc : getPropertyDescriptors(bean)) {
            final String name = desc.getName();
            if (desc.getReadMethod() != null && !"class".equals(name)) {
                return true;
            }
        }
        return false;
    }

    // do not have to return anything... same as predicate always returning true
    public static void visitAllProperties(
            Object bean, BiConsumer<Object, PropertyDescriptor> visitor) {
        visitProperties(bean, (b, p) -> {
            visitor.accept(b, p); return true;}, null, newCache());
    }
    @SuppressWarnings("unchecked")
    public static <T> void visitAllProperties(Object bean,
            BiConsumer<T, PropertyDescriptor> visitor, Class<T> type) {
        visitProperties(bean, (b, p) -> {
            visitor.accept((T) b, p); return true;}, type, newCache());
    }

    public static boolean visitProperties(
            Object bean, BiPredicate<Object, PropertyDescriptor> visitor) {
        return visitProperties(bean, visitor, null, newCache());
    }

    public static <T> boolean visitProperties(
            Object bean, BiPredicate<Object, PropertyDescriptor> visitor,
            Class<T> type) {
        return visitProperties(bean, visitor, type, newCache());
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
            for (PropertyDescriptor desc : getPropertyDescriptors(bean)) {
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

    public static List<PropertyDescriptor> getPropertyDescriptors(Object bean) {
        return getPropertyDescriptors(bean, false);
    }
    // lenient means to include properties that have just a setter
    // or getter.  This can easily cause trouble in other code.
    // Make public if there is really a need for it, else remove arg.
    private static List<PropertyDescriptor> getPropertyDescriptors(
            Object bean, boolean lenient) {
        try {
            List<PropertyDescriptor> pdList = new ArrayList<>();
            for (PropertyDescriptor pd : Introspector.getBeanInfo(
                    bean.getClass()).getPropertyDescriptors()) {
                FluentPropertyDescriptor fpd = new FluentPropertyDescriptor(pd);
                if (fpd.getReadMethod() != null
                        && fpd.getWriteMethod() != null) {
                    pdList.add(fpd);
                }
            }
            return pdList;
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
        visitProperties(source, (obj, pd) -> {
            if (obj != source) {
                return false;
            }
            String name = pd.getName();
            if (getValue(target, name) == null) {
                setValue(target, name, getValue(source, name));
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
            // If a primitive or String (immutable), return as is.
            if (bean instanceof String
                    || ClassUtils.isPrimitiveOrWrapper(bean.getClass())) {
                return bean;
            }

            @SuppressWarnings("unchecked")
            T newBean = (T) bean.getClass().newInstance();
            copyProperties(newBean, bean);
            return newBean;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeanException("Cannot clone bean.", e);
        }
    }

    public static <T> String diff(T bean1, T bean2) {
        //MAYBE: Visit all properties keeping trail, or nesting level
        Bag<String> b1 = graphLeavesAsBag(bean1);
        Bag<String> b2 = graphLeavesAsBag(bean2);

        Collection<String> left = CollectionUtils.removeAll(b1, b2);
        Collection<String> right = CollectionUtils.removeAll(b2, b1);

        Bag<String> allDiffs = new TreeBag<>((s1, s2) -> {
            // compare keys
            int comp = substringBefore(s1.substring(1), "=")
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
    private static Bag<String> graphLeavesAsBag(Object bean) {
        Bag<String> bag = new HashBag<>();
        visitAllProperties(bean, (o, p) -> {
            String key = o.getClass().getSimpleName()
                    + "." + p.getName() + " = ";
            Object value = getValue(o, p);
            String line = key;
            if (!hasChildren(value)) {
                line += Objects.toString(value);
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
                line += "#hashCode:" + value.hashCode() + ">";
            }
            bag.add(line);
        });
        return bag;
    }

    /**
     * Gets a bean write method, with supports for fluent API (returning self).
     * @param bean object
     * @param propertyName property name
     * @return method, or <code>null</code> in no writable method is found
     */
    public static Method getWriteMethod(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        try {
            FluentPropertyDescriptor pd =
                    new FluentPropertyDescriptor(propertyName, bean.getClass());
            return pd.getWriteMethod();
        } catch (IntrospectionException e) {
            LOG.error("Cannot get write method from {}.",
                    bean.getClass().getName(), e);
        }
        return null;
    }
}