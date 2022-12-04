/* Copyright 2021-2022 Norconex Inc.
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

import com.norconex.commons.lang.function.Consumers;
import com.norconex.commons.lang.function.FunctionUtil;
import com.norconex.commons.lang.xml.XML;

import lombok.EqualsAndHashCode;
import lombok.ToString;

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
 *     <condition class="(a Predicate implementation)"/>
 *     <condition class="(a Predicate implementation)"/>
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
 * @param <T> type of the object to be submitted to the flow.
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
public final class XMLFlow<T> {

    public static final String DEFAULT_CONSUMERS_WRITE_TAG_NAME = "consumer";

    private final Class<? extends XMLFlowPredicateAdapter<T>> predicateAdapter;
    private final Class<? extends XMLFlowConsumerAdapter<T>> consumerAdapter;

    public XMLFlow() {
        this(null, null);
    }
    public XMLFlow(
            Class<? extends XMLFlowConsumerAdapter<T>> consumerAdapter,
            Class<? extends XMLFlowPredicateAdapter<T>> predicateAdapter) {
        this.consumerAdapter = consumerAdapter;
        this.predicateAdapter = predicateAdapter;
    }

    public Class<? extends XMLFlowConsumerAdapter<T>> getConsumerAdapter() {
        return consumerAdapter;
    }
    public Class<? extends XMLFlowPredicateAdapter<T>> getPredicateAdapter() {
        return predicateAdapter;
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
        if (consumers.size() > 1) {
            return FunctionUtil.allConsumers(consumers);
        }
        if (consumers.size() == 1) {
            return consumers.get(0);
        }
        return null;
    }

    private Consumer<T> parseConsumer(XML consumerXML) {
        Consumer<T> consumer = null;

        if (consumerAdapter != null) {
            // If a consumer adapter is set, use it to parse the XML
            try {
                //MAYBE: throw XMLValidationException if there are any errors?
                XMLFlowConsumerAdapter<T> c =
                        consumerAdapter.getDeclaredConstructor().newInstance();
                c.loadFromXML(consumerXML);
                consumer = c;
            } catch (Exception e) {
                throw new XMLFlowException("Consumer adapter "
                        + consumerAdapter.getName() + " could not resolve "
                        + "this XML: " + consumerXML, e);
            }

        } else {
            // if not set the XML is expected to have a "class" attribute
            // that resolves to Consumer<T>.
            consumer = consumerXML.toObjectImpl(Consumer.class, null);
        }
        if (consumer == null) {
            throw new XMLFlowException("XML element '" + consumerXML.getName()
            + "' does not resolve to an implementation of "
            + "java.util.function.Consumer. Add a class=\"\" attribute "
            + "pointing to your predicate implementation, or initialize "
            + "XMLFlow with an XMLFlowConsumerAdapter.");
        }
        return consumer;
    }

    /**
     * Writes a flow previously constructed by {@link #parse(XML)} to XML.
     * @param xml the XML to write to
     * @param consumer the consumer flow to write
     */
    public void write(XML xml, Consumer<T> consumer) {
        if (consumer == null || xml == null) {
            return;
        }
        if (consumer instanceof Consumers) {
            // Group of consumers
            ((Consumers<T>) consumer).forEach(c -> writeSingleConsumer(xml, c));
        } else {
            // Single custom consumer
            writeSingleConsumer(xml, consumer);
        }
    }
    private void writeSingleConsumer(XML xml, Consumer<T> consumer) {
        if (consumer instanceof XMLIfNot) {
            ((XMLIfNot<T>) consumer).saveToXML(xml.addElement("ifNot"));
        } else if (consumer instanceof XMLIf) {
            ((XMLIf<T>) consumer).saveToXML(xml.addElement("if"));
        } else {
            xml.addElement(DEFAULT_CONSUMERS_WRITE_TAG_NAME, consumer);
        }
    }
}