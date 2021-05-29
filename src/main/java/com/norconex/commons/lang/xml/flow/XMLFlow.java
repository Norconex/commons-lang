/* Copyright 2021 Norconex Inc.
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
package com.norconex.commons.lang.xml.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.function.FunctionUtil;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>
 * Treats an XML block as being a mix of predicate and consumer classes
 * to create an execution "flow".
 * The XML syntax below can be used create conditional
 * {@link Predicate} statements deciding whether to execute or not
 * {@link Consumer} objects.
 * </p>
 * <h3>XML Syntax</h3>
 * {@nx.xml
 * <!-- Proceeds if condition is true. -->
 * <if>
 *   <!--
 *     A single XML "condition" or "conditions" (i.e., condition group).
 *     A condition group accepts an operator. Example:
 *     -->
 *   <conditions operator="[AND|OR]">
 *     <!--
 *       Unless you have a default implementation configured, "condition"
 *       expects a "class" attribute pointing to a Predicate implementation.
 *       E.g.:
 *       -->
 *     <condition class="(a Predicate implementation)">
 *     <condition class="(a Predicate implementation)">
 *   </conditions>
 *   <then>
 *     <!--
 *       Holds one or more XML elements with a "class" attribute pointing to a
 *       Consumer implementation. Executed when above condition or condition
 *       group evaluates to true. Can also contain nested if/ifNot blocks.
 *       -->
 *   </then>
 *   <else>
 *     <!--
 *       Optional. Same as "then" above, but triggered if the condition
 *       evaluates to false. Can also contain nested if/ifNot blocks.
 *       -->
 *   </else>
 * </if>
 *
 * <!-- Proceed if condition is false. -->
 * <ifNot>
 *   <!-- Same as "if" tag -->
 * </ifNot>
 * }
 * @author Pascal Essiembre
 * @param <T> type of the object to be submitted to the flow.
 * @since 2.0.0
 */
public final class XMLFlow<T> {

    // condition
    private final Class<? extends Predicate<T>> defaultPredicateType;
    // then/else
    private final Class<? extends Consumer<T>> defaultConsumerType;

    private XMLFlow(Builder<T> b) {
        this.defaultPredicateType = b.defaultPredicateType;
        this.defaultConsumerType = b.defaultConsumerType;
    }

    public Class<? extends Consumer<T>> getDefaultConsumerType() {
        return defaultConsumerType;
    }
    public Class<? extends Predicate<T>> getDefaultPredicateType() {
        return defaultPredicateType;
    }

    /**
     * Alternative to invoking:
     * Builder&lt;SomeType&gt; builder = new XMLFlow.Builder&lt;SomeType&gt;();
     * @param <T> type of the object to be submitted to the flow.
     * @return this builder, for chaining
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // parses "if", "ifNot" and any tag backed by "Consumer".
    public Consumer<T> parse(XML xml) {
        if (xml == null) {
            return null;
        }
        List<Consumer<T>> consumers = new ArrayList<>();
        xml.forEach("*", x -> {
            Consumer<T> consumer = null;
            Tag tag = Tag.of(x.getName());
            if (tag == null) {
                consumer = parseConsumer(x);
            } else if (tag == Tag.IF) {
                XMLIf<T> ifBlock = new XMLIf<>(this);
                ifBlock.loadFromXML(x);
                consumer = ifBlock;
            } else if (tag == Tag.IFNOT) {
                XMLIfNot<T> ifNotBlock = new XMLIfNot<>(this);
                ifNotBlock.loadFromXML(x);
                consumer = ifNotBlock;
            } else {
                throw new XMLFlowException("<" + tag + "> is misplaced.");
            }
            consumers.add(consumer);
        });
        return FunctionUtil.allConsumers(consumers);
    }

    private Consumer<T> parseConsumer(XML consumerXML) {
        // 1. check if it is implementing Consumer/IXMLConfigurable.
        Consumer<T> consumer = consumerXML.toObjectImpl(Consumer.class, null);

        // 2. Use default consumer (if any) to resolve and parse it.
        if (consumer == null && defaultConsumerType != null) {
            try {
                //TODO throw XMLValidationException if there are any errors?
                consumer = defaultConsumerType.newInstance();
                consumerXML.populate(consumer);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new XMLFlowException(
                        "Could not create default consumer from "
                        + defaultConsumerType.getName() + " for XML: "
                        + consumerXML);
            }
        }
        if (consumer == null) {
            throw new XMLFlowException("'" + consumerXML.getName()
            + "' does not resolve to an implementation of "
            + "java.util.function.Consumer.");
        }
        return consumer;
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

    public static class Builder<T> {
        // condition
        private Class<? extends Predicate<T>> defaultPredicateType;
        // then/else
        private Class<? extends Consumer<T>> defaultConsumerType;

        private Builder() {}

        public Builder<T> defaultPredicateType(
                Class<? extends Predicate<T>> type) {
            this.defaultPredicateType = type;
            return this;
        }
        public Builder<T> defaultConsumerType(
                Class<? extends Consumer<T>> type) {
            this.defaultConsumerType = type;
            return this;
        }

        public XMLFlow<T> build() {
            return new XMLFlow<>(this);
        }
    }
}