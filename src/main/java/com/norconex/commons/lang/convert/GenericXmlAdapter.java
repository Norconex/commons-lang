/* Copyright 2023 Norconex Inc.
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
package com.norconex.commons.lang.convert;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GenericXmlAdapter<T> extends XmlAdapter<String, T> {

    private final Class<T> type;

    @Override
    public String marshal(T obj) throws Exception {
        return GenericConverter.convert(obj);
    }

    @Override
    public T unmarshal(String value) throws Exception {
        return GenericConverter.convert(value, type);
    }
}
