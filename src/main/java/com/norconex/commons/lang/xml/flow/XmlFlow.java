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
import com.norconex.commons.lang.xml.Xml;

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
 * <h2>XML Syntax</h2>
 * <pre>
 * &lt;!-- Proceeds if condition is true. --&gt;
 * &lt;if&gt;
 *   &lt;!--
 *     A single XML "condition" or "conditions" (i.e., condition group).
 *     A condition group accepts an operator. Example:
 *     --&gt;
 *   &lt;conditions operator="[AND|OR]"&gt;
 *     &lt;!--
 *       Unless you have a default implementation configured, "condition"
 *       expects a "class" attribute pointing to a Predicate implementation.
 *       E.g.:
 *       --&gt;
 *     &lt;condition class="(a Predicate implementation)"/&gt;
 *     &lt;condition class="(a Predicate implementation)"/&gt;
 *   &lt;/conditions&gt;
 *   &lt;then&gt;
 *     &lt;!--
 *       Holds one or more XML elements with a "class" attribute pointing to a
 *       Consumer implementation. Executed when above condition or condition
 *       group evaluates to true. Can also contain nested if/ifNot blocks.
 *       --&gt;
 *   &lt;/then&gt;
 *   &lt;else&gt;
 *     &lt;!--
 *       Optional. Same as "then" above, but triggered if the condition
 *       evaluates to false. Can also contain nested if/ifNot blocks.
 *       --&gt;
 *   &lt;/else&gt;
 * &lt;/if&gt;
 *
 * &lt;!-- Proceed if condition is false. --&gt;
 * &lt;ifNot&gt;
 *   &lt;!-- Same as "if" tag --&gt;
 * &lt;/ifNot&gt;
 * </pre>
 * @param <T> type of the object to be submitted to the flow.
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
public final class XmlFlow<T> {

    public static final String DEFAULT_CONSUMERS_WRITE_TAG_NAME = "consumer";

    private final Class<? extends XmlFlowPredicateAdapter<T>> predicateAdapter;
    private final Class<? extends XmlFlowConsumerAdapter<T>> consumerAdapter;

    public XmlFlow() {
        this(null, null);
    }

    public XmlFlow(
            Class<? extends XmlFlowConsumerAdapter<T>> consumerAdapter,
            Class<? extends XmlFlowPredicateAdapter<T>> predicateAdapter) {
        this.consumerAdapter = consumerAdapter;
        this.predicateAdapter = predicateAdapter;
    }

    public Class<? extends XmlFlowConsumerAdapter<T>> getConsumerAdapter() {
        return consumerAdapter;
    }

    public Class<? extends XmlFlowPredicateAdapter<T>> getPredicateAdapter() {
        return predicateAdapter;
    }

    // parses "if", "ifNot" and any tag backed by "Consumer".
    public Consumer<T> parse(Xml xml) {
        if (xml == null) {
            return null;
        }
        List<Consumer<T>> consumers = new ArrayList<>();
        xml.forEach("*", x -> {
            Consumer<T> consumer = null;
            var tag = Tag.of(x.getName());
            if (tag == null) {
                consumer = parseConsumer(x);
            } else if (tag == Tag.IF) {
                var ifBlock = new XmlIf<>(this);
                ifBlock.loadFromXML(x);
                consumer = ifBlock;
            } else if (tag == Tag.IFNOT) {
                var ifNotBlock = new XmlIfNot<>(this);
                ifNotBlock.loadFromXML(x);
                consumer = ifNotBlock;
            } else {
                throw new XmlFlowException("<" + tag + "> is misplaced.");
            }
            consumers.add(consumer);
        });
        if (consumers.size() > 1) {
            return new Consumers<>(consumers);
        }
        if (consumers.size() == 1) {
            return consumers.get(0);
        }
        return null;
    }

    private Consumer<T> parseConsumer(Xml consumerXML) {
        Consumer<T> consumer = null;

        if (consumerAdapter != null) {
            // If a consumer adapter is set, use it to parse the XML
            try {
                //MAYBE: throw XMLValidationException if there are any errors?
                XmlFlowConsumerAdapter<T> c =
                        consumerAdapter.getDeclaredConstructor().newInstance();
                c.loadFromXML(consumerXML);
                consumer = c;
            } catch (Exception e) {
                throw new XmlFlowException("Consumer adapter "
                        + consumerAdapter.getName() + " could not resolve "
                        + "this XML: " + consumerXML, e);
            }

        } else {
            // if not set the XML is expected to have a "class" attribute
            // that resolves to Consumer<T>.
            consumer = consumerXML.toObjectImpl(Consumer.class, null);
        }
        if (consumer == null) {
            throw new XmlFlowException("XML element '" + consumerXML.getName()
                    + "' does not resolve to an implementation of "
                    + "java.util.function.Consumer. Add a class=\"\" attribute "
                    + "pointing to your predicate implementation, or initialize "
                    + "XMLFlow with an XMLFlowConsumerAdapter.");
        }
        return consumer;
    }

    /**
     * Writes a flow previously constructed by {@link #parse(Xml)} to XML.
     * @param xml the XML to write to
     * @param consumer the consumer flow to write
     */
    public void write(Xml xml, Consumer<T> consumer) {
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

    private void writeSingleConsumer(Xml xml, Consumer<T> consumer) {
        if (consumer instanceof XmlIfNot) {
            ((XmlIfNot<T>) consumer).saveToXML(xml.addElement("ifNot"));
        } else if (consumer instanceof XmlIf) {
            ((XmlIf<T>) consumer).saveToXML(xml.addElement("if"));
        } else {
            xml.addElement(DEFAULT_CONSUMERS_WRITE_TAG_NAME, consumer);
        }
    }
}