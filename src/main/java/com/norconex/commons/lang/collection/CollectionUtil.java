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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.convert.GenericConverter;

import lombok.NonNull;

/**
 * Collection-related utility methods.
 * @since 2.0.0
 */
public final class CollectionUtil {

    private CollectionUtil() {
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
        var list = new ArrayList<T>();
        if (object == null ||
                (object instanceof String s && StringUtils.isBlank(s))) {
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
     * <p>
     * Converts a collection to an array using the specified type
     * (never <code>null</code>).
     * </p>
     * <p>
     * <b>Since 3.0.0</b>, passing a <code>null</code> collection argument will
     * will return an empty array of the supplied type.
     * </p>
     * @param c collection to convert or <code>null</code>
     * @param arrayType array type (must not be <code>null</code>)
     * @return the new array (never <code>null</code>)
     * @throws NullPointerException if arrayType is <code>null</code>
     */
    public static Object toArray(Collection<?> c, @NonNull Class<?> arrayType) {
        if (c == null) {
            return Array.newInstance(arrayType, 0);
        }
        var array = Array.newInstance(arrayType, c.size());
        var i = 0;
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
     * @return a list view of the specified array (never <code>null</code>)
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
            return null; //NOSONAR
        }
        return Arrays.asList(values);
    }

    /**
     * Converts a list of objects to an unmodifiable list of strings using
     * default {@link GenericConverter} instance.
     * If the supplied list is <code>null</code>, an empty string list
     * is returned.
     * @param values list to convert to a list of strings
     * @return list of strings
     */
    public static List<String> toStringList(Object... values) {
        return toStringList(asListOrNull(values));
    }
    /**
     * Converts a list of objects to an unmodifiable list of strings using
     * default {@link GenericConverter} instance.
     * If the supplied list is <code>null</code>, an empty string list
     * is returned.
     * @param values list to convert to a list of strings
     * @return list of strings
     */
    public static List<String> toStringList(Collection<?> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream().map(GenericConverter::convert).toList();
    }
    /**
     * Converts a list of strings to an unmodifiable list of objects matching
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
        return values.stream()
                .map(str -> GenericConverter.convert(str, targetClass))
                .toList();
    }

    /**
     * Converts a list of strings to an unmodifiable list of objects matching
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
        return values.stream().map(converter).toList();
    }

    /**
     * Returns an unmodifiable view of the specified list. Convenience method
     * for doing with an array the same as
     * {@link Collections#unmodifiableList(List)}. A <code>null</code>
     * array argument will return an empty list.
     * @param <T> target objects class type
     * @param values the values to convert to an unmodifiable list
     * @return unmodifiable list (never <code>null</code>)
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> unmodifiableList(T... values) {
        return Collections.unmodifiableList(
                values == null
                ? Collections.emptyList()
                : Arrays.asList(values));
    }
    /**
     * Returns an unmodifiable view of the specified set. Convenience method
     * for doing with an array the same as
     * {@link Collections#unmodifiableSet(Set)}. A <code>null</code>
     * array argument will return an empty set.
     * This method uses a {@link LinkedHashSet} to maintain order.
     * @param <T> target objects class type
     * @param values the values to convert to an unmodifiable list
     * @return unmodifiable list
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> unmodifiableSet(T... values) {
        return Collections.unmodifiableSet(
                values == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(Arrays.asList(values)));
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
        c.removeIf(Objects::isNull);
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

    /**
     * Create a non-null mutable list containing the combination of supplied
     * objects of the specified type. If the object is a Collection or array,
     * it will be iterated over to populate the new list (recursively).
     * Any <code>null</code> objects are ignored (not added to the returned
     * list).
     * Objects are returned in the order supplied.
     * @param objects objects to combine into a new list
     * @return a new non-null, mutable list
     * @since 3.0.0
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> unionList(Object... objects) {
        var list = new ArrayList<T>();
        if (ArrayUtils.isEmpty(objects)) {
            return list;
        }
        for (Object obj : objects) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof Collection) {
                list.addAll(unionList(((Collection<?>) obj).toArray()));
            } else if (obj.getClass().isArray()) {
                list.addAll(unionList((Object[]) obj));
            } else {
                list.add((T) obj);
            }
        }
        return list;
    }

    /**
     * Create a non-null mutable set containing the combination of supplied
     * objects of the specified type (minus duplicates). If the object is a
     * Collection or array, it will be iterated over to populate the new
     * set (recursively).
     * Any <code>null</code> objects are ignored (not added to the returned
     * set).
     * There are no guarantee on the order in which objects are returned.
     * @param objects objects to combine into a new set
     * @return a new non-null, mutable set
     * @since 3.0.0
     */
    public static <T> Set<T> unionSet(Object... objects) {
        return new HashSet<>(unionList(objects));
    }

}
