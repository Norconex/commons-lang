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
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.EqualsExclude;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.HashCodeExclude;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.function.Predicates;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * Represents a single "condition" tag or a "conditions" tag with nested
 * "condition" and/or "conditions".
 * @author Pascal Essiembre
 * @since 2.0.0
 */
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
        // TODO check if predicate is instance of Predicates?

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
        xml.forEach("*", x -> {
            predicateList.add(loadConditionFromXML(x));
        });
        if (predicateList.size() == 1) {
            return predicateList.get(0);
        }
        return new Predicates<>(predicateList, Operator.OR == operator);
    }

    Predicate<T> parseSingleCondition(XML xml) {
        // 1. Try if it is implementing Predicate/IXMLConfigurable.
        Predicate<T> p = xml.toObjectImpl(Predicate.class, null);

        // 2. Else, use default predicate (if any) to resolve and parse it.
        if (p == null && flow.getDefaultPredicateType() != null) {
            try {
                //TODO throw XMLValidationException if there are any errors?
                p = flow.getDefaultPredicateType().newInstance();
                xml.populate(p);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new XMLFlowException(
                        "Could not create default predicate from "
                        + flow.getDefaultPredicateType().getName()
                        + " for XML: " + xml);
            }
        }
        if (p == null) {
            throw new XMLFlowException("'" + xml.getName()
                    + "' does not resolve to an implementation of "
                    + "java.util.function.Predicate.");
        }
        return p;
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
}
