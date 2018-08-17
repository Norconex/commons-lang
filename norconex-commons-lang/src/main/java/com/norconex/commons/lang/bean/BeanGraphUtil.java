/* Copyright 2018 Norconex Inc.
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
package com.norconex.commons.lang.bean;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 2.0.0
 */
public final class BeanGraphUtil {

    private static final Logger LOG =
            LoggerFactory.getLogger(BeanGraphUtil.class);

    private BeanGraphUtil() {
        super();
    }

    public static <T> List<T> find(Object bean, Class<T> type) {
        Objects.requireNonNull(type, "Type cannot be null.");
        List<T> foundList = new ArrayList<>();
        visit(bean, foundList::add, type, new HashSet<>());
        return foundList;
    }

    public static void visit(Object bean, Consumer<Object> visitor) {
        visit(bean, visitor, null, new HashSet<>());
    }

    public static <T> void visit(
            Object bean, Consumer<T> visitor, Class<T> type) {
        visit(bean, visitor, type, new HashSet<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> void visit(Object bean,
            Consumer<T> visitor, Class<T> type, Set<Object> cache) {

        if (bean == null || cache.contains(bean)) {
            return;
        }
        cache.add(bean);

        if (type == null || type.isInstance(bean)) {
            visitor.accept((T) bean);
        }

        for (Object child : getChildren(bean)) {
            if (child instanceof Map) {
                for (Entry<Object, Object> entry :
                        ((Map<Object, Object>) child).entrySet()) {
                    visit(entry.getKey(), visitor, type, cache);
                    visit(entry.getValue(), visitor, type, cache);
                }
            } else if (child instanceof Collection) {
                for (Object obj : (Collection<Object>) child) {
                    visit(obj, visitor, type, cache);
                }
            } else {
                visit(child, visitor, type, cache);
            }
        }
    }

    public static List<Object> getChildren(Object bean) {
        if (bean == null) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>();
        for (PropertyDescriptor descriptor :
                PropertyUtils.getPropertyDescriptors(bean)) {
            final String name = descriptor.getName();
            if (descriptor.getReadMethod() != null && !"class".equals(name)) {
                try {
                    list.add(PropertyUtils.getProperty(bean, name));
                } catch (IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    LOG.error("Cannot get children of {} instance.",
                            bean.getClass().getName(), e);
                }
            }
        }
        return list;
    }
}