/* Copyright 2018-2022 Norconex Inc.
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.convert.Converter;
import com.norconex.commons.lang.map.Properties;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Bean/object utilities.
 * @since 2.0.0
 */
@Slf4j
public final class BeanUtil {

    private BeanUtil() {}

    //--- Bean methods ---------------------------------------------------------

    /**
     * Gets a list of property descriptors for the supplied bean.
     * If there is an error in retrieving the properties, an empty
     * list will be returned and and error message will be logged.
     * @param bean the bean to get property descriptors from
     * @return list of property descriptors
     */
    public static List<PropertyDescriptor> getPropertyDescriptors(Object bean) {
        try {
            List<PropertyDescriptor> pdList = new ArrayList<>();
            for (PropertyDescriptor pd : Introspector.getBeanInfo(
                    bean.getClass()).getPropertyDescriptors()) {
                var fpd = new FluentPropertyDescriptor(pd);
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

    /**
     * Performs shallow copies of source object property values
     * over the corresponding target properties whose values are
     * <code>null</code>.
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
            if (getValue(target, name) == null) {
                setValue(target, name, getValue(source, name));
            }
            return true;
        });
    }

    /**
     * Performs shallow copies of source object property values
     * over the corresponding target properties.
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
     * @param <T> the type of the object being cloned
     * @param bean the object being cloned
     * @return the cloned object
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

    //--- Property methods -----------------------------------------------------

    /**
     * Gets the type of specified object property.
     * @param bean the object
     * @param propertyName the property name
     * @return object property type
     * @throws BeanException on error obtaining the property type
     */
    public static Class<?> getPropertyType(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        try {
            var p = new PropertyDescriptor(propertyName, bean.getClass());
            return p.getPropertyType();
        } catch (IntrospectionException | IllegalArgumentException e) {
            throw accessorException("get type",
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
     * Gets the value of a bean property, supporting "fluent" accessors.
     * @param bean the object
     * @param propertyName the property name
     * @return object property value
     * @throws BeanException on error obtaining the property value
     */
    public static <T> T getValue(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return null;
        }
        try {
            return getValue(bean, new FluentPropertyDescriptor(
                    propertyName, bean.getClass()));
        } catch (IntrospectionException e) {
            throw accessorException("get value",
                    propertyName, bean.getClass().getCanonicalName(), e);
        }
    }

    /**
     * Gets the value of a bean property based on a {@link PropertyDescriptor}.
     * @param bean the object
     * @param property the property descriptor
     * @return object property value
     * @throws BeanException on error obtaining the property value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object bean, PropertyDescriptor property) {
        if (bean == null || property == null) {
            return null;
        }
        try {
            // If child is equal to parent, it may be a source of
            // infinite loop, so we do not return it.  We do the same
            // if the property is transient.
            var getter = property.getReadMethod();
            if (getter.getAnnotation(Transient.class) != null) {
                return null;
            }
            var value = (T) getter.invoke(bean);
            if (Objects.equals(bean, value)) {
                return null;
            }
            return value;
        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw accessorException("get value",
                    property.getName(), bean.getClass().getCanonicalName(), e);
        }
    }

    /**
     * Sets the value of a bean property, supporting "fluent" accessors.
     * @param bean the object
     * @param propertyName the property name
     * @param value object property value
     * @throws BeanException on error obtaining the property value
     */
    public static void setValue(
            @NonNull Object bean, @NonNull String propertyName, Object value) {
        try {
            var stmt = new Statement(
                    bean, "set" + StringUtils.capitalize(propertyName),
                    new Object[]{value});
            stmt.execute();
        } catch (Exception e) {
            throw accessorException("set value",
                    propertyName, bean.getClass().getCanonicalName(), e);
        }
    }

    /**
     * Gets whether a given bean property has a setter method for it.
     * @param bean the object
     * @param propertyName the property name
     * @return <code>true</code> if the object property as a setter method
     * @throws BeanException on error obtaining property information
     */
    public static boolean isSettable(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return false;
        }
        try {
            var p = new PropertyDescriptor(propertyName, bean.getClass());
            return p.getWriteMethod() != null;
        } catch (IntrospectionException | IllegalArgumentException e) {
            LOG.trace("Could not get information for property \"{}\" on "
                    + "bean type \"{}\".",
                    propertyName, bean.getClass().getCanonicalName(), e);
            return false;
        }
    }

    /**
     * Gets whether a given bean property has a getter method for it.
     * @param bean the object
     * @param propertyName the property name
     * @return <code>true</code> if the object property as a getter method
     * @throws BeanException on error obtaining property information
     */
    public static boolean isGettable(Object bean, String propertyName) {
        if (bean == null || propertyName == null) {
            return false;
        }
        try {
            var p =
                    new PropertyDescriptor(propertyName, bean.getClass());
            return p.getReadMethod() != null;
        } catch (IntrospectionException | IllegalArgumentException e) {
            LOG.trace("Could not get information for property \"{}\" on "
                    + "bean type \"{}\".",
                    propertyName, bean.getClass().getCanonicalName(), e);
            return false;
        }
    }

    private static BeanException accessorException(
            String action, String property, String className, Exception e) {
        return new BeanException(String.format(
                "Could not %s for property \"%s\" on bean type \"%s\".",
                action, property, className), e);
    }


    //--- Map/Properties methods -----------------------------------------------

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
                props.add(key, Converter.convert(value));
            }
        }
        return props;
    }


    //--- Visitor/Graph methods ------------------------------------------------

    /**
     * Finds a list of objects matching the given type by traversing
     * the supplied object graph (i.e., itself and its child objects,
     * recursively).
     * @param <T> the type of objects to find
     * @param bean the bean on which to find matches
     * @param type class instance of the type to find
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
     * Gets child objects of the supplied bean.
     * @param bean the object to obtain child objects from
     * @return list of child objects
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
     * Gets whether the supplied object has any children. That is, it
     * has at least one property with a getter method.
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

    @SuppressWarnings("unchecked")
    private static <T> boolean doVisit(VisitArgs<Predicate<T>, T> args) {
        if (args.isBeanNullOrCached()) {
            return true;
        }
        if (args.isTypeNullOrBeanInstance()
                && !args.visitor.test((T) args.bean)) {
            return false;
        }
        for (Object child : getChildren(args.bean)) {
            var childArgs = args.withBean(child);
            if (!childArgs.visitIfMap(BeanUtil::doVisitMap)
                    || !childArgs.visitIfCollection(BeanUtil::doVisitCollection)
                    || !doVisit(childArgs)) {
                return false;
            }
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

        for (Object child : getChildren(args.bean)) {
            var childArgs = args.withBean(child);
            if (!childArgs.visitIfMap(BeanUtil::doVisitPropertiesMap)
                    || !childArgs.visitIfCollection(
                            BeanUtil::doVisitPropertiesCollection)
                    || !doVisitProperties(childArgs)) {
                return false;
            }
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
        Bag<String> bag = new HashBag<>();
        visitAllProperties(bean, (o, p) -> {
            var key = o.getClass().getSimpleName()
                    + "." + p.getName() + " = ";
            var value = getValue(o, p);
            var line = key;
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
            var pd =
                    new FluentPropertyDescriptor(propertyName, bean.getClass());
            return pd.getWriteMethod();
        } catch (IntrospectionException e) {
            LOG.error("Cannot get write method from {}.",
                    bean.getClass().getName(), e);
        }
        return null;
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
            // continue if not a map
            return true;
        }
    }
}