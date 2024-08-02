/* Copyright 2020 Norconex Inc.
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
package com.norconex.commons.lang.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>Convenient way of handling the different ways a value (or values) can
 * be set on a {@link Properties} instance.
 * </p>
 *
 * {@nx.xml.usage #attributes
 * onSet="[append|prepend|replace|optional]"
 * }
 * <P>
 * The above is the recommended attribute for consuming classes to use
 * in XML configuration.
 * </p>
 *
 * @since 2.0.0
 */
public enum PropertySetter {
    /**
     * Inserts supplied value(s) at the end of an existing list of values
     * associated with the supplied key.
     * If there are no matching keys or the key has no associated value(s),
     * the supplied value(s) are set like a normal list addition.
     */
    APPEND(Properties::addList),
    /**
     * Inserts supplied value(s) at the beginning of an existing list of values.
     * If there are no matching keys or the key has no associated value(s),
     * the supplied value(s) are set like a normal list addition.
     */
    PREPEND((p, k, v) -> {
        List<Object> fullList = new ArrayList<>(p.getStrings(k));
        fullList.addAll(0, v);
        p.setList(k, fullList);
    }),
    /**
     * Replaces all value(s) already existing for a matching key with the
     * supplied one(s).
     * If there are no matching keys or the key has no associated value(s),
     * the supplied value(s) are set like a normal list addition.
     */
    REPLACE(Properties::setList),
    /**
     * Only set supplied value(s) if the supplied key does not exist or
     * if it does not have any values associated.
     */
    OPTIONAL((p, k, v) -> {
        if (CollectionUtils.isEmpty(p.get(k))) {
            p.setList(k, v);
        }
    });

    private final Strategy s;
    PropertySetter(Strategy s) {
        this.s = s;
    }

    /**
     * Applies the <code>PropertySetter</code> strategy on the supplied
     * properties with the given key and value. Collection or array values
     * are considered as such.
     * Supplying a <code>null</code> properties argument as no effect.
     * @param properties the properties to possibly add a key/value to
     * @param key the key on which we set value
     * @param value the value to possibly set
     */
    public void apply(Properties properties, String key, Object value) {
        if (properties == null) {
            return;
        }
        s.apply(properties, key, CollectionUtil.adaptedList(value));
    }

    public static PropertySetter from(
            String name, PropertySetter defaultSetter) {
        return Optional.ofNullable(from(name)).orElse(defaultSetter);
    }
    public static PropertySetter from(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (PropertySetter ps : PropertySetter.values()) {
            if (ps.name().equalsIgnoreCase(name)) {
                return ps;
            }
        }
        return null;
    }
    // if null, returns desired appender.
    public static PropertySetter orAppend(PropertySetter setter) {
        if (setter == null) {
            return APPEND;
        }
        return setter;
    }
    public static PropertySetter orOptional(PropertySetter setter) {
        if (setter == null) {
            return OPTIONAL;
        }
        return setter;
    }
    public static PropertySetter orPrepend(PropertySetter setter) {
        if (setter == null) {
            return PREPEND;
        }
        return setter;
    }
    public static PropertySetter orReplace(PropertySetter setter) {
        if (setter == null) {
            return REPLACE;
        }
        return setter;
    }

    // Gets from XML, returns default value if not defined
    public static PropertySetter fromXML(XML xml, PropertySetter defaultValue) {
        if (xml == null) {
            return defaultValue;
        }
        return xml.getEnum("@onSet", PropertySetter.class, defaultValue);
    }
    public static void toXML(XML xml, PropertySetter setter) {
        if (xml != null) {
            xml.setAttribute("onSet", setter);
        }
    }

    @FunctionalInterface
    interface Strategy {
        void apply(Properties properties, String key, List<Object> value);
    }
}