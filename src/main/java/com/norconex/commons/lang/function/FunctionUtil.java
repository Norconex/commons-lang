package com.norconex.commons.lang.function;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Function-related utility methods
 * @since 2.0.0
 */
public final class FunctionUtil {

    private FunctionUtil() {}

    @SafeVarargs
    public static <T> Consumers<T> allConsumers(Consumer<T>... consumers) {
        return allConsumers(
                consumers == null ? null : Arrays.asList(consumers));
    }
    public static <T> Consumers<T> allConsumers(
            Collection<Consumer<T>> consumers) {
        return new Consumers<>(consumers == null
                ? Collections.emptyList() : consumers);
    }

    @SafeVarargs
    public static <T> Predicates<T> allPredicates(Predicate<T>... predicates) {
        return allPredicates(
                predicates == null ? null : Arrays.asList(predicates));
    }
    public static <T> Predicates<T> allPredicates(
            Collection<Predicate<T>> predicates) {
        return new Predicates<>(predicates == null
                ? Collections.emptyList() : predicates, false);
    }
    @SafeVarargs
    public static <T> Predicates<T> anyPredicates(Predicate<T>... predicates) {
        return anyPredicates(
                predicates == null ? null : Arrays.asList(predicates));
    }
    public static <T> Predicates<T> anyPredicates(
            Collection<Predicate<T>> predicates) {
        return new Predicates<>(predicates == null
                ? Collections.emptyList() : predicates, true);
    }

    public static <T> Consumer<T> predicatedConsumer(
            Predicate<T> predicate, Consumer<T> consumer) {
        return PredicatedConsumer.ifTrue(predicate, consumer);
    }
}
