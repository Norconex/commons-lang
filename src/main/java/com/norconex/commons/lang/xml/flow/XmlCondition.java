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

import com.norconex.commons.lang.function.Predicates;
import com.norconex.commons.lang.xml.Xml;
import com.norconex.commons.lang.xml.XmlConfigurable;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a single "condition" tag or a "conditions" tag with nested
 * "condition" and/or "conditions".
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode
class XmlCondition<T> implements XmlConfigurable, Predicate<T> {

    enum Operator {
        AND("AND", "ALL", "&&"),
        OR("OR", "ANY", "||");

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

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final XmlFlow<T> flow;

    private Predicate<T> predicate;

    XmlCondition(XmlFlow<T> flow) {
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
    public void loadFromXML(Xml xml) {
        predicate = loadConditionFromXML(xml);
    }

    @Override
    public void saveToXML(Xml xml) {
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

    private void writeGroupPredicate(Xml xml, Predicates<T> predicates) {
        xml.setAttribute("operator", predicates.isAny()
                ? Operator.OR.toString()
                : Operator.AND.toString());
        predicates.forEach(p -> {
            if (p instanceof Predicates) {
                writeGroupPredicate(
                        xml.addElement("conditions"), (Predicates<T>) p);
            } else {
                writeSinglePredicate(xml.addElement("condition"), p);
            }
        });
    }

    private void writeSinglePredicate(Xml xml, Predicate<T> predicate) {
        xml.replace(new Xml(xml.getName(), predicate));
    }

    private Predicate<T> loadConditionFromXML(Xml xml) {
        Predicate<T> p;
        if (Tag.CONDITIONS.is(xml.getName())) {
            // Condition group
            p = parseConditionGroup(xml);
        } else if (Tag.CONDITION.is(xml.getName())) {
            // Single condition
            p = parseSingleCondition(xml);
        } else {
            throw new XmlFlowException(
                    "Only 'conditions' and 'condition' are accepted "
                            + "in an 'if' or 'ifNot' block.");
        }
        return p;
    }

    Predicate<T> parseConditionGroup(Xml xml) {
        List<Predicate<T>> predicateList = new ArrayList<>();
        var operator = Operator.of(xml.getString("@operator"));
        xml.forEach("*", x -> predicateList.add(loadConditionFromXML(x)));
        if (predicateList.size() == 1) {
            return predicateList.get(0);
        }
        return new Predicates<>(predicateList, Operator.OR == operator);
    }

    Predicate<T> parseSingleCondition(Xml predicateXML) {
        Predicate<T> p = null;
        if (flow.getPredicateAdapter() != null) {
            // If a predicate adapter is set, use it to parse the XML
            try {
                //MAYBE: throw XMLValidationException if there are any errors?
                XmlFlowPredicateAdapter<T> adapter =
                        flow.getPredicateAdapter()
                                .getDeclaredConstructor()
                                .newInstance();
                adapter.loadFromXML(predicateXML);
                p = adapter;
            } catch (Exception e) {
                throw new XmlFlowException("Predicate adapter "
                        + flow.getPredicateAdapter().getName() + " could not "
                        + "resolve this XML: " + predicateXML, e);
            }
        } else {
            // if predicate adapter is not set, the XML is expected to have a
            // "class" attribute that resolves to Predicate<T>.
            p = predicateXML.toObjectImpl(Predicate.class, null);
        }
        if (p == null) {
            throw new XmlFlowException("XML element '" + predicateXML.getName()
                    + "' does not resolve to an implementation of "
                    + "java.util.function.Predicate. Add a class=\"\" attribute "
                    + "pointing to your predicate implementation, or initialize "
                    + "XMLFlow with an XMLFlowPredicateAdapter.");
        }
        return p;
    }
}
