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
package com.norconex.commons.lang.time;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * <p>
 * Use localized properties files ({@link ResourceBundle}) to get the
 * string representation of duration unit. Each properties file
 * is expected to have the following keys, their plural versions being
 * optional (if a plural unit is spelled the same as singular).
 * </p>
 * <p>
 * If a key is not found, the <code>toString()</code> version of the unit
 * will be returned.
 * If locale is <code>null</code>, the default locale will be use.
 * If the unit is <code>null</code>, <code>null</code> will be returned.
 * </p>
 * <ul>
 *   <li>year</li>
 *   <li>years</li>
 *   <li>month</li>
 *   <li>months</li>
 *   <li>week</li>
 *   <li>weeks</li>
 *   <li>day</li>
 *   <li>days</li>
 *   <li>hour</li>
 *   <li>hours</li>
 *   <li>minute</li>
 *   <li>minutes</li>
 *   <li>second</li>
 *   <li>seconds</li>
 *   <li>millisecond</li>
 *   <li>milliseconds</li>
 * </ul>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class RBDurationUnitFormatter implements IDurationUnitFormatter {

    public static final IDurationUnitFormatter FULL =
            new RBDurationUnitFormatter(RBDurationUnitFormatter
                    .class.getCanonicalName() + "-full");
    public static final IDurationUnitFormatter COMPACT =
            new RBDurationUnitFormatter(RBDurationUnitFormatter
                    .class.getCanonicalName() + "-compact");
    public static final IDurationUnitFormatter ABBREVIATED =
            new RBDurationUnitFormatter(RBDurationUnitFormatter
                    .class.getCanonicalName() + "-abbr");

    private final String baseName;
    private final ClassLoader classLoader;

    public RBDurationUnitFormatter(String baseName) {
        this(baseName, null);
    }
    public RBDurationUnitFormatter(String baseName, ClassLoader classLoader) {
        super();
        this.baseName = baseName;
        this.classLoader = classLoader == null
                ? getClass().getClassLoader() : classLoader;
    }

    @Override
    public String format(DurationUnit unit, Locale locale, boolean plural) {
        if (unit == null) {
            return null;
        }
        ResourceBundle bundle = getResourceBundle(locale);
        String key = unit.toString().toLowerCase();
        if (plural) {
            String pluralKey = key + 's';
            if (bundle.containsKey(pluralKey)) {
                return bundle.getString(pluralKey);
            }
        }
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return unit.toString();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
    public String getBaseName() {
        return baseName;
    }

    protected ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(
                getBaseName(), locale, getClassLoader());
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
        return new ReflectionToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}