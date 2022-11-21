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
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsExclude;
import org.apache.commons.lang3.builder.HashCodeExclude;
import org.apache.commons.lang3.builder.ToStringExclude;

import com.norconex.commons.lang.function.Predicates;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a single "condition" tag or a "conditions" tag with nested
 * "condition" and/or "conditions".
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
class XMLCondition<T> implements IXMLConfigurable, Predicate<T> {

    enum Operator {
        AND("AND", "ALL", "&&"),
        OR("OR", "ANY", "||")
        ;
        private final List<String> tokens = new ArrayList<>();
        Operator(String... tokens) {
            this.tokens.addAll(Arrays.asList(tokens));
        }
        @Override
        public String toString() {
            return tokens.get(0);
        }
        public static Operator of(String operator) {
            return OR.tokens.contains(
                    StringUtils.upperCase(operator)) ? OR : AND;
        }
    }

    @ToStringExclude @EqualsExclude @HashCodeExclude
    private final XMLFlow<T> flow;

    private Predicate<T> predicate;

    XMLCondition(XMLFlow<T> flow) {
        this.flow = flow;
    }

    boolean isGroup() {
        return predicate instanceof Predicates;
    }

    @Override
    public boolean test(T t) {
        if (t == null) {
            return false;
        }
        if (predicate == null) {
            return true;
        }
        return predicate.test(t);
    }

    @Override
    public void loadFromXML(XML xml) {
        this.predicate = loadConditionFromXML(xml);
    }

    @Override
    public void saveToXML(XML xml) {
        if (predicate == null || xml == null) {
            return;
        }
        if (isGroup()) {
            // Group of conditions
            writeGroupPredicate(xml, (Predicates<T>) predicate);
        } else {
            writeSinglePredicate(xml, predicate);
        }
    }

    private void writeGroupPredicate(XML xml, Predicates<T> predicates) {
        xml.setAttribute("operator", predicates.isAny()
                ? Operator.OR.toString() : Operator.AND.toString());
        predicates.forEach(p -> {
            if (p instanceof Predicates) {
                writeGroupPredicate(
                        xml.addElement("conditions"), (Predicates<T>) p);
            } else {
                writeSinglePredicate(xml.addElement("condition"), p);
            }
        });
    }
    private void writeSinglePredicate(XML xml, Predicate<T> predicate) {
        xml.replace(new XML(xml.getName(), predicate));
    }

    private Predicate<T> loadConditionFromXML(XML xml) {
        Predicate<T> p;
        if (Tag.CONDITIONS.is(xml.getName())) {
            // Condition group
            p = parseConditionGroup(xml);
        } else if (Tag.CONDITION.is(xml.getName())) {
            // Single condition
            p = parseSingleCondition(xml);
        } else {
            throw new XMLFlowException(
                    "Only 'conditions' and 'condition' are accepted "
                    + "in an 'if' or 'ifNot' block.");
        }
        return p;
    }

    Predicate<T> parseConditionGroup(XML xml) {
        List<Predicate<T>> predicateList = new ArrayList<>();
        Operator operator = Operator.of(xml.getString("operator"));
        xml.forEach("*", x -> predicateList.add(loadConditionFromXML(x)));
        if (predicateList.size() == 1) {
            return predicateList.get(0);
        }
        return new Predicates<>(predicateList, Operator.OR == operator);
    }

    Predicate<T> parseSingleCondition(XML predicateXML) {
        Predicate<T> p = null;
        if (flow.getPredicateAdapter() != null) {
            // If a predicate adapter is set, use it to parse the XML
            try {
                //MAYBE: throw XMLValidationException if there are any errors?
                IXMLFlowPredicateAdapter<T> adapter =
                        flow.getPredicateAdapter()
                            .getDeclaredConstructor()
                            .newInstance();
                adapter.loadFromXML(predicateXML);
                p = adapter;
            } catch (Exception e) {
                throw new XMLFlowException("Predicate adapter "
                        + flow.getPredicateAdapter().getName() + " could not "
                        + "resolve this XML: " + predicateXML, e);
            }
        } else {
            // if predicate adapter is not set, the XML is expected to have a
            // "class" attribute that resolves to Predicate<T>.
            p = predicateXML.toObjectImpl(Predicate.class, null);
        }
        if (p == null) {
            throw new XMLFlowException("XML element '" + predicateXML.getName()
            + "' does not resolve to an implementation of "
            + "java.util.function.Predicate. Add a class=\"\" attribute "
            + "pointing to your predicate implementation, or initialize "
            + "XMLFlow with an IXMLFlowPredicateAdapter.");
        }
        return p;
    }
}
