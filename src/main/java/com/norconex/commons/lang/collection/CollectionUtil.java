/* Copyright 2018-2020 Norconex Inc.
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
package com.norconex.commons.lang.collection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.convert.Converter;

/**
 * Collection-related utility methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class CollectionUtil {

    private CollectionUtil() {
        super();
    }

    /**
     * Adapts any object to a new non-null mutable List, regardless of the
     * nature of the object. If the object is a Collection or array,
     * it will be iterated over to populate the new list. If it is any other
     * object, it will become the first and only item in the newly created list.
     * If the object is <code>null</code> or a blank string,
     * an empty list is returned.
     * The supplied object must be a value or values of the expected type.
     * @param <T> the expected return type of the list
     * @param object object to adapt
     * @return adapted list
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> adaptedList(Object object) {
        ArrayList<T> list = new ArrayList<>();
        if (object == null || (object instanceof String
                && StringUtils.isBlank((String) object))) {
            return list;
        }
        if (object instanceof Collection) {
            list.addAll((Collection<T>) object);
        } else if (object.getClass().isArray()) {
            list.addAll(Arrays.asList((T[]) object));
        } else {
            list.add((T) object);
        }
        return list;
    }
    /**
     * Adapts any object to a new non-null mutable Set, regardless of the
     * nature of the object. If the object is a Collection or array,
     * it will be iterated over to populate the new set. If it is any other
     * object, it will become the first and only item in the newly created set.
     * If the object is <code>null</code> or a blank string,
     * an empty set is returned.
     * The supplied object must be a value or values of the expected type.
     * @param <T> the expected return type of the set
     * @param object object to adapt
     * @return adapted set
     */
    public static <T> Set<T> adaptedSet(Object object) {
        return new HashSet<>(adaptedList(object));
    }

    /**
     * Converts a collection to an array using the specified type.
     * @param c collection to convert
     * @param arrayType array type
     * @return the new array
     */
    public static Object toArray(Collection<?> c, Class<?> arrayType) {
        if (c == null) {
            return null;
        }

        Object array = Array.newInstance(arrayType, c.size());
        int i = 0;
        for (Object t : c) {
            Array.set(array, i++, t);
        }
        return array;
    }

    /**
     * Sets all values of the source collection into the target one.
     * Same as doing a "clear", followed by "addAll", but it also checks
     * for <code>null</code> and will not clear/add if the source
     * collection is the same instance as the target one.
     * If target is <code>null</code>, invoking this method has no effect.
     * If source is <code>null</code> will clear the target collection
     * @param target target collection
     * @param source source collection
     * @param <T> objects class type
     */
    public static <T> void setAll(Collection<T> target, Collection<T> source) {
        if (target == null || target == source) {
            return;
        }
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }
    /**
     * Sets all values of the source array into the target collection.
     * Same as doing a "clear", followed by "addAll" after converting the
     * array to a list. It also checks
     * for <code>null</code>.
     * If target is <code>null</code>, invoking this method has no effect.
     * If source is <code>null</code> or is an array containing only one
     * <code>null</code> element, it will clear the target collection
     * @param target target collection
     * @param source source collection
     * @param <T> objects class type
     */
    @SafeVarargs
    public static <T> void setAll(Collection<T> target, T... source) {
        if (target == null) {
            return;
        }
        if (source == null || (source.length == 1 && source[0] == null)) {
            target.clear();
        } else {
            setAll(target, Arrays.asList(source));
        }
    }

    /**
     * Sets all values of the source map into the target one.
     * Same as doing a "clear", followed by "putAll", but it also checks
     * for <code>null</code> and will not clear/add if the source
     * map is the same instance as the target one.
     * If target is <code>null</code>, invoking this method has no effect.
     * If source is <code>null</code> will clear the target map
     * @param target target map
     * @param source source map
     * @param <K> key type
     * @param <V> value type
     */
    public static <K,V> void setAll(Map<K,V> target, Map<K,V> source) {
        if (target == null || target == source) {
            return;
        }
        target.clear();
        if (source != null) {
            target.putAll(source);
        }
    }

    /**
     * Returns a fixed-size list backed by the specified array or
     * an empty list if the array is <code>null</code>. This is a null-safe
     * version of {@link Arrays#asList(Object...)}.
     * @param values the array by which the list will be backed
     * @param <T> objects class type
     * @return a list view of the specified array
     * @see #asListOrNull(Object...)
     */
    @SafeVarargs
    public static <T> List<T> asListOrEmpty(T... values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(values);
    }
    /**
     * Returns a fixed-size list backed by the specified array or
     * <code>null</code> if the array is <code>null</code>. This is a null-safe
     * version of {@link Arrays#asList(Object...)}.
     * A list is returned even if the array is empty or contains only
     * <code>null</code> values.
     * @param <T> objects class type
     * @param values the array by which the list will be backed
     * @return a list view of the specified array
     * @see #asListOrEmpty(Object...)
     */
    @SafeVarargs
    public static <T> List<T> asListOrNull(T... values) {
        if (values == null) {
            return null;
        }
        return Arrays.asList(values);
    }

    /**
     * Converts a list of objects to a list of strings using
     * default {@link Converter} instance.
     * If the supplied list is <code>null</code>, an empty string list
     * is returned.
     * @param values list to convert to a list of strings
     * @return list of strings
     */
    public static List<String> toStringList(Object... values) {
        return toStringList(asListOrNull(values));
    }
    /**
     * Converts a list of objects to a list of strings using
     * default {@link Converter} instance.
     * If the supplied list is <code>null</code>, an empty string list
     * is returned.
     * @param values list to convert to a list of strings
     * @return list of strings
     */
    public static List<String> toStringList(Collection<?> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream().map(
                Converter::convert).collect(Collectors.toList());
    }
    /**
     * Converts a list of strings to a list of objects matching
     * the return type.
     * If the supplied list is <code>null</code>, an empty list
     * is returned.
     * @param <T> objects class type
     * @param values list to convert to a list of the given type
     * @param targetClass target class
     * @return list
     */
    public static <T> List<T> toTypeList(
            List<String> values, Class<T> targetClass) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream().map(str -> Converter.convert(
                str, targetClass)).collect(Collectors.toList());
    }

    /**
     * Converts a list of strings to a list of objects matching
     * the return type.
     * If the supplied list is <code>null</code>, an empty list
     * is returned.
     * @param <T> target objects class type
     * @param values list to convert to a list of the given type
     * @param converter function converting string to type
     * @return list
     */
    public static <T> List<T> toTypeList(
            List<String> values, Function<String, T> converter) {
        if (values == null) {
            return Collections.emptyList();
        }
        //TODO is this method really working without accepting a type?
        // If so, modify Properties/XML accordingly
        return values.stream().map(converter).collect(Collectors.toList());
    }

    /**
     * Returns an unmodifiable view of the specified list. Convenience method
     * for doing with an array the same as
     * {@link Collections#unmodifiableList(List)}.
     * @param <T> target objects class type
     * @param values the values to convert to an unmodifiable list
     * @return unmodifiable list
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> unmodifiableList(T... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
    /**
     * Returns an unmodifiable view of the specified set. Convenience method
     * for doing with an array the same as
     * {@link Collections#unmodifiableSet(Set)}.
     * This method uses a {@link LinkedHashSet} to maintain order.
     * @param <T> target objects class type
     * @param values the values to convert to an unmodifiable list
     * @return unmodifiable list
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> unmodifiableSet(T... values) {
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(values)));
    }

    /**
     * Removes <code>null</code> entries in the given collection.  Only
     * useful for collection implementations allowing <code>null</code> entries.
     * @param c a collection
     */
    public static void removeNulls(Collection<?> c) {
        if (c == null) {
            return;
        }
        c.removeIf(Objects::nonNull);
    }

    /**
     * Removes blank strings in the given collection.
     * @param c a string collection
     */
    public static void removeBlanks(Collection<String> c) {
        if (c == null) {
            return;
        }
        c.removeIf(StringUtils::isBlank);
    }

    /**
     * Removes empty strings in the given collection.
     * @param c a string collection
     */
    public static void removeEmpties(Collection<String> c) {
        if (c == null) {
            return;
        }
        c.removeIf(StringUtils::isEmpty);
    }


    /**
     * Replaces all elements matching the source value with the target
     * value.
     * @param c a collection
     * @param source object to replace
     * @param target replacement
     * @param <T> elements type
     */
    public static <T> void replaceAll(Collection<T> c, T source, T target) {
        if (c == null) {
            return;
        }
        CollectionUtils.transform(
                c, e -> Objects.equals(e, source) ? target : e);
    }

    /**
     * Convert <code>null</code> entries to empty strings.
     * @param c a string collection
     */
    public static void nullsToEmpties(Collection<String> c) {
        if (c == null) {
            return;
        }
        CollectionUtils.transform(c, e -> Objects.equals(e, null) ? "" : e);
    }
    /**
     * Convert empty string entries to <code>null</code>.
     * @param c a string collection
     */
    public static void emptiesToNulls(Collection<String> c) {
        if (c == null) {
            return;
        }
        CollectionUtils.transform(c, e -> StringUtils.isEmpty(e) ? null : e);
    }
    /**
     * Convert blank string entries to <code>null</code>.
     * @param c a string collection
     */
    public static void blanksToNulls(Collection<String> c) {
        if (c == null) {
            return;
        }
        CollectionUtils.transform(c, e -> StringUtils.isBlank(e) ? null : e);
    }

    //TODO have a version that accepts a function that returns a string?
}
