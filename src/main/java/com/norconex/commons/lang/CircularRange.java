/* Copyright 2017-2022 Norconex Inc.
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
package com.norconex.commons.lang;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.Range;

import lombok.EqualsAndHashCode;

/**
 * <p>
 * A range from a possible set of values that rolls over when defined circular
 * start or end is reached. Because the range is circular, there is no
 * concept of before and after and maximum range can be smaller than minimum.
 * This class is otherwise similar to Apache Commons Lang {@link Range} class.
 * </p>
 * <p>
 * Even though this class implements {@link Serializable}, it makes no
 * guarantee about supplied arguments. If serialization is important to you,
 * make sure supplied arguments are serializable.
 * </p>
 * @param <T> the range type
 * @author Pascal Essiembre
 * @since 1.14.0
 */
@EqualsAndHashCode
public final class CircularRange<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @EqualsAndHashCode.Exclude
    private final Comparator<T> comparator; //NOSONAR
    private final T minimum; //NOSONAR
    private final T maximum; //NOSONAR
    private final T circleStart; //NOSONAR
    private final T circleEnd; //NOSONAR
    private transient String toString;

    @SuppressWarnings("unchecked")
    private CircularRange(
            final T circleStart, final T circleEnd,
            final T minimum, final T maximum, final Comparator<T> comp) {
        if (circleStart == null || circleEnd == null) {
            throw new IllegalArgumentException(String.format(
                    "Circular boundaries must not be null: circleStart=%s"
                            + ", circleEnd=%s", circleStart, circleEnd));
        }
        if (minimum == null || maximum == null) {
            throw new IllegalArgumentException(String.format(
                    "Elements in a range must not be null: minimum=%s"
                            + ", maximum=%s", minimum, maximum));
        }
        if (comp == null) {
            this.comparator = ComparableComparator.INSTANCE;
        } else {
            this.comparator = comp;
        }
        if (!inNormalRange(minimum, circleStart, circleEnd)
                || !inNormalRange(maximum, circleStart, circleEnd)) {
            throw new IllegalArgumentException(String.format(
                    "Elements in a range must fit between circular start/end: "
                  + "circleStart=%s, circleEnd=%s, minimum=%s, maximum=%s",
                          circleStart, circleEnd, minimum, maximum));
        }
        if (compare(circleEnd, circleStart) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Circular start must be smaller than or equal to circlar "
                    + "end: circleStart=%s, circleEnd=%s",
                            circleStart, circleEnd));
        }
        this.circleStart = circleStart;
        this.circleEnd = circleEnd;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * <p>Obtains a range using the specified element as both the minimum
     * and maximum in this range and as both circular start and end.</p>
     *
     * <p>The range uses the natural ordering of the elements to determine where
     * values lie in the range.</p>
     *
     * @param <T> the type of the elements in this range
     * @param element  the value to use for this range, not null
     * @return the range object, not null
     * @throws IllegalArgumentException if the element is null
     * @throws ClassCastException if the element is not {@code Comparable}
     */
    public static <T extends Comparable<T>> CircularRange<T> is(
            final T element) {
        return between(element, element, null);
    }

    /**
     * <p>Obtains a range using the specified element as both the minimum
     * and maximum in this range and as both circular start and end.</p>
     *
     * <p>The range uses the specified {@code Comparator} to determine where
     * values lie in the range.</p>
     *
     * @param <T> the type of the elements in this range
     * @param element  the value to use for this range, must not be {@code null}
     * @param comparator  the comparator to be used, null for natural ordering
     * @return the range object, not null
     * @throws IllegalArgumentException if the element is null
     * @throws ClassCastException if using natural ordering and the elements
     *         are not {@code Comparable}
     */
    public static <T> CircularRange<T> is(
            final T element, final Comparator<T> comparator) {
        return between(element, element, comparator);
    }

    /**
     * <p>Obtains a range with the specified minimum and maximum values also
     * serving as circular start and end (all inclusive).</p>
     *
     * <p>The range uses the natural ordering of the elements to determine where
     * values lie in the range.</p>
     *
     * <p>The arguments must be passed in order (min,max).
     *
     * @param <T> the type of the elements in this range
     * @param fromInclusive  the first value that defines the edge of
     *        the range, inclusive
     * @param toInclusive  the second value that defines the edge of
     *        the range, inclusive
     * @return the range object, not null
     * @throws IllegalArgumentException
     *             if either element is null or not in order
     * @throws ClassCastException if the elements are not {@code Comparable}
     */
    public static <T extends Comparable<T>> CircularRange<T> between(
            final T fromInclusive, final T toInclusive) {
        return between(fromInclusive, toInclusive, null);
    }

    /**
     * <p>Obtains a range with the specified minimum and maximum values also
     * serving as circular start and end (all inclusive).</p>
     *
     * <p>The range uses the specified {@code Comparator} to determine where
     * values lie in the range.</p>
     *
     * <p>The arguments must be passed in order (min,max).</p>
     *
     * @param <T> the type of the elements in this range
     * @param fromInclusive  the first value that defines the edge of the
     *        range, inclusive
     * @param toInclusive  the second value that defines the edge of the
     *        range, inclusive
     * @param comparator  the comparator to be used, null for natural ordering
     * @return the range object, not null
     * @throws IllegalArgumentException if either element is null
     * @throws ClassCastException if using natural ordering and the elements
     *         are not {@code Comparable}
     */
    public static <T> CircularRange<T> between(
            final T fromInclusive, final T toInclusive,
            final Comparator<T> comparator) {
        return between(fromInclusive, toInclusive,
                fromInclusive, toInclusive, comparator);
    }

    /**
     * <p>Obtains a range with the specified minimum and maximum values
     * and circular start and end values (all inclusive).</p>
     *
     * <p>The range uses the natural ordering of the elements to determine where
     * values lie in the range.</p>
     *
     * <p>The circle arguments must be passed in order (min,max).  The
     * range arguments can be passed in any order (min,max or max,min).
     * The order will be respected.
     *
     * @param <T> the type of the elements in this range
     * @param rangeFromInclusive  the first value that defines the edge of
     *        the range, inclusive
     * @param rangeToInclusive  the second value that defines the edge of
     *        the range, inclusive
     * @param circleStartInclusive the value that defines the circular start,
     *        inclusive
     * @param circleEndInclusive the value that defines the circular end,
     *        inclusive
     * @return the range object, not null
     * @throws IllegalArgumentException
     *             if either element is null or not in order
     * @throws ClassCastException if the elements are not {@code Comparable}
     */
    public static <T extends Comparable<T>> CircularRange<T> between(
            final T circleStartInclusive,
            final T circleEndInclusive,
            final T rangeFromInclusive,
            final T rangeToInclusive) {
        return between(circleStartInclusive, circleEndInclusive,
                rangeFromInclusive, rangeToInclusive, null);
    }

    /**
     * <p>Obtains a range with the specified minimum and maximum values
     * and circular start and end values (all inclusive).</p>
     *
     * <p>The range uses the specified {@code Comparator} to determine where
     * values lie in the range.</p>
     *
     * <p>The circle arguments must be passed in order (min,max).  The
     * range arguments can be passed in any order (min,max or max,min).
     * The order will be respected.
     *
     * @param <T> the type of the elements in this range
     * @param rangeFromInclusive  the first value that defines the edge of the
     *        range, inclusive
     * @param rangeToInclusive  the second value that defines the edge of the
     *        range, inclusive
     * @param circleStartInclusive the value that defines the circular start,
     *        inclusive
     * @param circleEndInclusive the value that defines the circular end,
     *        inclusive
     * @param comparator  the comparator to be used, null for natural ordering
     * @return the range object, not null
     * @throws IllegalArgumentException if either element is null
     * @throws ClassCastException if using natural ordering and the elements
     *         are not {@code Comparable}
     */
    public static <T> CircularRange<T> between(
            final T circleStartInclusive,
            final T circleEndInclusive,
            final T rangeFromInclusive,
            final T rangeToInclusive,
            final Comparator<T> comparator) {
        return new CircularRange<>(
                circleStartInclusive, circleEndInclusive,
                rangeFromInclusive, rangeToInclusive, comparator);
    }

    /**
     * <p>Obtains a new range with the specified circular start and end
     * values (both inclusive). The range values and comparator are the same.
     * </p>
     * @param circleStartInclusive the value that defines the circular start,
     *        inclusive
     * @param circleEndInclusive the value that defines the circular end,
     *        inclusive
     * @return the range object, not null
     * @throws IllegalArgumentException if either element is null
     */
    public CircularRange<T> withCircularBoundaries(
            final T circleStartInclusive, final T circleEndInclusive) {
        return new CircularRange<>(
                circleStartInclusive, circleEndInclusive,
                getMinimum(), getMaximum(), getComparator());
    }
    /**
     * <p>Obtains a new range with the specified minimum and maximum range
     * values (both inclusive). The circular start and end are the same.</p>
     * @param rangeFromInclusive  the first value that defines the edge of the
     *        range, inclusive
     * @param rangeToInclusive  the second value that defines the edge of the
     *        range, inclusive
     * @return the range object, not null
     * @throws IllegalArgumentException if either element is null
     */
    public CircularRange<T> withRange(
            final T rangeFromInclusive, final T rangeToInclusive) {
        return new CircularRange<>(
                getCircleStart(), getCircleEnd(),
                rangeFromInclusive, rangeToInclusive, getComparator());
    }

    // Accessors
    //--------------------------------------------------------------------

    /**
     * <p>Gets the minimum value in this range.</p>
     * @return the minimum value in this range, not null
     */
    public T getMinimum() {
        return minimum;
    }
    /**
     * <p>Gets the maximum value in this range.</p>
     * @return the maximum value in this range, not null
     */
    public T getMaximum() {
        return maximum;
    }
    /**
     * <p>Gets the start value of this circular range.</p>
     * @return the start value of this circular range, not null
     */
    public T getCircleStart() {
        return circleStart;
    }
    /**
     * <p>Gets the end value of this circular range.</p>
     * @return the end value of this circular range, not null
     */
    public T getCircleEnd() {
        return circleEnd;
    }
    /**
     * <p>Gets the comparator being used to determine if objects are
     * within the range.</p>
     *
     * <p>Natural ordering uses an internal comparator implementation, thus this
     * method never returns null. See {@link #isNaturalOrdering()}.</p>
     *
     * @return the comparator being used, not null
     */
    public Comparator<T> getComparator() {
        return comparator;
    }

    /**
     * <p>Whether or not the CircularRange is using the natural ordering of
     * the elements.</p>
     *
     * <p>Natural ordering uses an internal comparator implementation, thus this
     * method is the only way to check if a null comparator was specified.</p>
     *
     * @return true if using natural ordering
     */
    public boolean isNaturalOrdering() {
        return comparator == ComparableComparator.INSTANCE;
    }

    /**
     * <p>Whether or not the range rolls over the circular end. Basically,
     * if the range maximum is smaller than the minimum.</p>
     * @return <code>true</code> if rolling over
     */
    public boolean isRolling() {
        return compare(minimum, maximum) > -1;
    }

    // Element tests
    //--------------------------------------------------------------------

    /**
     * <p>Checks whether the specified element occurs within this range.</p>
     *
     * @param element  the element to check for, null returns false
     * @return true if the specified element occurs within this range
     */
    public boolean contains(final T element) {
        if (element == null) {
            return false;
        }
        if (!isRolling()) {
            return inNormalRange(element, minimum, maximum);
        }
        return inNormalRange(element, minimum, circleEnd)
                || inNormalRange(element, circleStart, maximum);
    }

    /**
     * <p>Checks whether this range starts with the specified element.</p>
     *
     * @param element  the element to check for, null returns false
     * @return true if the specified element occurs within this range
     */
    public boolean isStartedBy(final T element) {
        if (element == null) {
            return false;
        }
        return compare(element, minimum) == 0;
    }

    /**
     * <p>Checks whether this range ends with the specified element.</p>
     *
     * @param element  the element to check for, null returns false
     * @return true if the specified element occurs within this range
     */
    public boolean isEndedBy(final T element) {
        if (element == null) {
            return false;
        }
        return compare(element, maximum) == 0;
    }

    // CircularRange tests
    //--------------------------------------------------------------------

    /**
     * <p>Checks whether this range contains all the elements of the
     *    specified range.</p>
     *
     * <p>This method may fail if the ranges have two different comparators
     *    or element types.</p>
     *
     * @param otherRange  the range to check, null returns false
     * @return true if this range contains the specified range
     * @throws RuntimeException if ranges cannot be compared
     */
    public boolean containsRange(final CircularRange<T> otherRange) {
        if (otherRange == null) {
            return false;
        }

        // Normal vs Normal
        if (!isRolling() && !otherRange.isRolling()) {
            return contains(otherRange.minimum)
                    && contains(otherRange.maximum);
        }

        // 9 10 11 12 1 2 3 4
        //   10 11 12 1 2
        // Rolling vs Rolling
        if (isRolling() && otherRange.isRolling()) {
            return inNormalRange(otherRange.minimum, minimum, circleEnd)
                    && inNormalRange(otherRange.maximum, circleStart, maximum);
        }

        // 9 10 11 12 1 2 3 4
        //              2 3
        //   10 11
        // Rolling vs normal
        if (isRolling() && !otherRange.isRolling()) {
            return (inNormalRange(otherRange.minimum, minimum, circleEnd)
                    && inNormalRange(otherRange.maximum, minimum, circleEnd))
                || (inNormalRange(otherRange.minimum, circleStart, maximum)
                    && inNormalRange(otherRange.maximum, circleStart, maximum));
        }

        // Normal vs rolling is always false
        return false;
    }

    /**
     * <p>Checks whether this range is overlapped by the specified range.</p>
     *
     * <p>Two ranges overlap if there is at least one element in common.</p>
     *
     * <p>This method may fail if the ranges have two different comparators or
     * element types.</p>
     *
     * @param otherRange  the range to test, null returns false
     * @return true if the specified range overlaps with this
     *  range; otherwise, {@code false}
     * @throws RuntimeException if ranges cannot be compared
     */
    public boolean isOverlappedBy(final CircularRange<T> otherRange) {
        if (otherRange == null) {
            return false;
        }
        return otherRange.contains(minimum)
            || otherRange.contains(maximum)
            || contains(otherRange.minimum);
    }

    private int compare(T o1, T o2) {
        return comparator.compare(o1, o2);
    }
    private boolean inNormalRange(T element, T min, T max) {
        return compare(element, min) > -1 && compare(element, max) < 1;
    }

    /**
     * <p>Gets the range as a {@code String}.</p>
     *
     * <p>The format of the String is
     * '[<i>min</i>..<i>max</i>](<i>start</i>..<i>end</i>)'.</p>
     *
     * @return the {@code String} representation of this range
     */
    @Override
    public String toString() {
        if (toString == null) {
            toString = "[" + minimum + ".." + maximum
                     + "](" + circleStart + ".." + circleEnd + ")";
        }
        return toString;
    }

    /**
     * <p>Formats the receiver using the given format.</p>
     *
     * <p>This uses {@link java.util.Formattable} to perform the formatting.
     * Five variables may be used to embed the minimum, maximum, circular start,
     * circular end and comparator.
     * Use {@code %1$s} for the minimum element,
     * {@code %2$s} for the maximum element,
     * {@code %3$s} for the circular start,
     * {@code %4$s} for the circular end,
     * and {@code %5$s} for the comparator.
     * The default format used by {@code toString()} is
     * {@code [%1$s..%2$s](%3$s..%4$s)}.</p>
     *
     * @param format  the format string, optionally containing {@code %1$s},
     *        {@code %2$s}, {@code %3$s}, {@code %4$s} and {@code %3$s},
     *        not null
     * @return the formatted string, not null
     */
    public String toString(final String format) {
        return String.format(format, minimum, maximum,
                circleStart, circleEnd, comparator);
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings({"rawtypes", "unchecked"})
    private enum ComparableComparator implements Comparator {
        INSTANCE;
        /**
         * Comparable based compare implementation.
         *
         * @param obj1 left hand side of comparison
         * @param obj2 right hand side of comparison
         * @return negative, 0, positive comparison value
         */
        @Override
        public int compare(final Object obj1, final Object obj2) {
            return ((Comparable) obj1).compareTo(obj2);
        }
    }
}
