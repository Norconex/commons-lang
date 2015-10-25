/* Copyright 2015 Norconex Inc.
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

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * <p>Convenient way of checking whether at least one value for a key
 * in a given {@link Properties} matches a regular expression. 
 * </p>
 * <p>
 * A <code>null</code> or empty regex value will try to match an 
 * empty key value.
 * </p>
 * @author Pascal Essiembre
 * @since 1.8.0
 */
public class PropertyMatcher {
    private final String key;
    private final String regex;
    private final Pattern pattern;
    private final boolean caseSensitive;
    public PropertyMatcher(
            String key, String regex, boolean caseSensitive) {
        super();
        this.key = key;
        this.regex = regex;
        this.caseSensitive = caseSensitive;
        if (regex != null) {
            if (caseSensitive) {
                this.pattern = Pattern.compile(regex);
            } else {
                this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            }
        } else {
            this.pattern = Pattern.compile(".*");
        }            
    }
    
    public String getKey() {
        return key;
    }
    public String getRegex() {
        return regex;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Whether this property matcher matches a key value in the given 
     * {@link Properties}.
     * @param properties the properties to look for a match
     * @return <code>true</code> if at least one value for the key matches
     * the matcher regular expression
     */
    public boolean matches(Properties properties) {
        if (properties == null) {
            return false;
        }
        Collection<String> values =  properties.getStrings(key);
        for (String value : values) {
            if (StringUtils.isBlank(regex) && StringUtils.isBlank(value)) {
                return true;
            }
            String safeVal = StringUtils.trimToEmpty(value);
            if (pattern.matcher(safeVal).matches()) {
                return true;
            }
        }
        return false;
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(key)
                .append(regex)
                .append(caseSensitive)
                .toHashCode();
    }
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PropertyMatcher)) {
            return false;
        }
        PropertyMatcher castOther = (PropertyMatcher) other;
        return new EqualsBuilder()
                .append(key, castOther.key)
                .append(regex, castOther.regex)
                .append(caseSensitive, castOther.caseSensitive)
                .isEquals();
    }
    @Override
    public String toString() {
        ToStringBuilder builder = 
                new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        builder.append("key", key);
        builder.append("regex", regex);
        builder.append("caseSensitive", caseSensitive);
        return builder.toString();
    }
}