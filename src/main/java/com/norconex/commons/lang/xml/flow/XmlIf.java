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

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsExclude;
import org.apache.commons.lang3.builder.HashCodeExclude;
import org.apache.commons.lang3.builder.ToStringExclude;

import com.norconex.commons.lang.xml.Xml;
import com.norconex.commons.lang.xml.XmlConfigurable;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>
 * Treats an XML block as being an "if" statement, using a mix of predicate
 * and consumer classes to create or be part of an execution "flow".
 * </p>
 * <p>
 * An "if" block expecting a "condition" or a "conditions" tag representing
 * a group of "condition". Followed by the condition is expected
 * a "then" tag to create or be part of an execution "flow" followed
 * with an optional "else" tag.
 * Conditions are defined with one or more predicates while the
 * "then/else" portion is made of consumers (which can hold nested if/ifNot).
 * </p>
 * <p>
 * The "ifNot" evaluates the conditions just like "if" does, but negates it.
 * </p>
 * <p>
 * The XML syntax below can be used create conditional statements
 * {@link Predicate} which trigger {@link Consumer} objects if evaluating
 * to <code>true</code>.
 * </p>
 * <h3>XML Syntax</h3>
 * {@nx.xml
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
 * }
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
class XmlIf<T> implements Consumer<T>, XmlConfigurable {

    @ToStringExclude
    @EqualsExclude
    @HashCodeExclude
    private final XmlFlow<T> flow;
    private XmlCondition<T> condition;
    private Consumer<T> thenConsumer;
    private Consumer<T> elseConsumer;

    XmlIf(XmlFlow<T> flow) {
        this.flow = flow;
    }

    public Predicate<T> getCondition() {
        return condition;
    }

    public Consumer<T> getThenConsumer() {
        return thenConsumer;
    }

    public Consumer<T> getElseConsumer() {
        return elseConsumer;
    }

    @Override
    public void accept(T t) {
        if (conditionPasses(t)) {
            if (thenConsumer != null) {
                thenConsumer.accept(t);
            }
        } else if (elseConsumer != null) {
            elseConsumer.accept(t);
        }
    }

    protected boolean conditionPasses(T t) {
        return condition.test(t);
    }

    @Override
    public void loadFromXML(Xml xml) {
        // There must be exactly one top-level child named "conditions"
        // or "condition".
        var bag = xml.getXMLList(Tag.CONDITIONS + "|" + Tag.CONDITION);
        if (bag.size() != 1) {
            throw new XmlFlowException("There must be exactly one of "
                    + "<conditions> or <condition> as a direct child element "
                    + "of <if> or <ifNot>. Got instead: \""
                    + StringUtils.abbreviate(xml.toString(0)
                            .replaceAll("[\n\r]", ""), 40)
                    + "\"");
        }
        var cond = new XmlCondition<T>(flow);
        cond.loadFromXML(bag.get(0));
        condition = cond;

        // There must be exactly one top-level child named "then"
        // and one optional "else".
        //MAYBE enforce in schema that there must be one "then".
        thenConsumer = flow.parse(xml.getXML(Tag.THEN.toString()));
        elseConsumer = flow.parse(xml.getXML(Tag.ELSE.toString()));
    }

    @Override
    public void saveToXML(Xml xml) {
        condition.saveToXML(xml.addElement(
                condition.isGroup() ? "conditions" : "condition"));
        if (thenConsumer != null) {
            flow.write(xml.addElement("then"), thenConsumer);
        }
        if (elseConsumer != null) {
            flow.write(xml.addElement("else"), elseConsumer);
        }
    }
}
