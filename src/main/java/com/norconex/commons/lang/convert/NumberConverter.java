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
package com.norconex.commons.lang.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.norconex.commons.lang.EqualsUtil;

/**
 * <p>
 * Number conversion.  Following types are supported:
 * </p>
 * <ul>
 *   <li>Byte</li>
 *   <li>byte</li>
 *   <li>Short</li>
 *   <li>short</li>
 *   <li>Integer</li>
 *   <li>int</li>
 *   <li>Float</li>
 *   <li>float</li>
 *   <li>Long</li>
 *   <li>long</li>
 *   <li>Double</li>
 *   <li>double</li>
 *   <li>BigInteger</li>
 *   <li>BigDecimal</li>
 * </ul>
 * @since 2.0.0
 */
public class NumberConverter extends AbstractConverter {

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T nullSafeToType(String value, Class<T> type) {
        String v = value.trim();
        Object o = null;
        if (EqualsUtil.equalsAny(type, Byte.class, byte.class)) {
            o = Byte.decode(v);
        } else if (EqualsUtil.equalsAny(type, Short.class, short.class)) {
            o = Short.decode(v);
        } else if (EqualsUtil.equalsAny(type, Integer.class, int.class)) {
            o = Integer.decode(v);
        } else if (EqualsUtil.equalsAny(type, Long.class, long.class)) {
            o = Long.decode(v);
        } else if (type.equals(BigInteger.class)) {
            o = new BigInteger(v);
        } else if (EqualsUtil.equalsAny(type, Float.class, float.class)) {
            o = Float.valueOf(v);
        } else if (EqualsUtil.equalsAny(type, Double.class, double.class)) {
            o = Double.valueOf(v);
        } else if (type.equals(BigDecimal.class) || type.equals(Number.class)) {
            o = new BigDecimal(v);
        } else {
            throw toTypeException(value, type);
        }
        return (T) o;
    }

    @Override
    protected String nullSafeToString(Object object) {
        return object.toString();
    }
}
