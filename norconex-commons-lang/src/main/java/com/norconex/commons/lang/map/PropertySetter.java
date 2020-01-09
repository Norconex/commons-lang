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

import com.norconex.commons.lang.xml.XML;

/**
 * <p>Convenient way of handling the different ways a value (or values) can
 * be set on a {@link Properties} instance.
 * </p>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public enum PropertySetter {
    APPEND((p, k, v) -> {
        if (v instanceof List) {
            p.addList(k, (List<?>) v);
        } else {
            p.add(k, v);
        }
    }),
    PREPEND((p, k, v) -> {
        List<Object> list = new ArrayList<>(p.getStrings(k));
        if (v instanceof List) {
            list.addAll(0, (List<?>) v);
        } else {
            list.add(0, v);
        }
        p.setList(k, list);
    }),
    REPLACE((p, k, v) -> {
        if (v instanceof List) {
            p.setList(k, (List<?>) v);
        } else {
            p.set(k, v);
        }
    }),
    // only if not set
    OPTIONAL((p, k, v) -> {
        if (CollectionUtils.isEmpty(p.get(k))) {
            if (v instanceof List) {
                p.setList(k, (List<?>) v);
            } else {
                p.set(k, v);
            }
        }
    });

    private final Strategy s;
    private PropertySetter(Strategy s) {
        this.s = s;
    }
    public void apply(Properties properties, String key, Object value) {
        this.s.apply(properties, key, value);
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
    // if null, returns default value of APPEND
    //TODO change to orAppend(...) ?
    public static PropertySetter orDefault(PropertySetter setter) {
        if (setter == null) {
            return APPEND;
        }
        return setter;
    }
    // Gets from XML, returns default value if not defined
    public static PropertySetter fromXML(XML xml, PropertySetter defaultValue) {
        return xml.getEnum("@onSet", PropertySetter.class, defaultValue);
    }
    public static void toXML(XML xml, PropertySetter setter) {
        xml.setAttribute("onSet", setter);
    }

    @FunctionalInterface
    interface Strategy {
        void apply(Properties properties, String key, Object value);
    }
}