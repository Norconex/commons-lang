/* Copyright 2020-2021 Norconex Inc.
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
package com.norconex.commons.lang.collection;

import java.util.Iterator;

import org.apache.commons.collections4.iterators.AbstractIteratorDecorator;

/**
 * Counts the number of time {@link #next()} was invoked.
 * @author Pascal Essiembre
 * @param <T> iterator type
 * @since 2.0.0
 */
public class CountingIterator<T> extends AbstractIteratorDecorator<T> {

    private int count;

    public CountingIterator(Iterator<T> iterator) {
        super(iterator);
    }

    @Override
    public T next() {
        T t = super.next();
        count++;
        return  t;
    }

    public int getCount() {
        return count;
    }
}
