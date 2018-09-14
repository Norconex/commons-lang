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

import java.util.Objects;

/**
 * <p>
 * Adds default behaviors common to most converters, including
 * checking for <code>null</code> values and wrapping exceptions
 * in {@link ConverterException}.
 * </p>
 *
 * @author Pascal Essiembre
 * @see ConverterException
 * @since 2.0.0
 */
public abstract class AbstractConverter implements IConverter {

    @Override
    public final <T> T toType(String value, Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        if (value == null) {
            return null;
        }
        try {
            return nullSafeToType(value, type);
        } catch (ConverterException e) {
            throw e;
        } catch (Exception e) {
            throw toTypeException(value, type, e);
        }
    }

    @Override
    public final String toString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return nullSafeToString(object);
        } catch (ConverterException e) {
            throw e;
        } catch (Exception e) {
            throw toStringException(object, e);
        }
    }

    protected abstract <T> T nullSafeToType(
            String value, Class<T> type) throws Exception;
    protected abstract String nullSafeToString(
            Object object) throws Exception;

    protected ConverterException toUnsupportedTypeException(Object obj) {
        return new ConverterException(
                "Type " + obj.getClass().getSimpleName()
              + " is not supported by this converter ("
              + getClass().getSimpleName() + ").");
    }

    protected ConverterException toTypeException(
            String value, Class<?> type) {
        return toTypeException(value, type, null);
    }
    protected ConverterException toTypeException(
            String value, Class<?> type, Exception e) {
        return new ConverterException(String.format(
                "Cannot convert string \"%s\" to type \"%s\".",
                value, type), e);
    }
    protected ConverterException toStringException(Object object) {
        return toStringException(object, null);
    }
    protected ConverterException toStringException(Object object, Exception e) {
        return new ConverterException(String.format(
                "Cannot convert object of type \"%s\" to String.",
                object.getClass().getName()), e);
    }
}
