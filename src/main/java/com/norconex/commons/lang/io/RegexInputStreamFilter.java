/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.commons.lang.io;

import java.util.regex.Pattern;

/**
 * Filters lines of text read from an InputStream decorated with 
 * {@link FilteredInputStream}, based on a given regular expression.
 * @author Pascal Essiembre
 * @see Pattern
 */
public class RegexInputStreamFilter implements IInputStreamFilter {

    private final Pattern pattern;
    
    /**
     * Constructor.
     * @param regex regular expression
     */
    public RegexInputStreamFilter(String regex) {
        super();
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Constructor.
     * @param pattern regular expression pattern
     */
    public RegexInputStreamFilter(Pattern pattern) {
        super();
        this.pattern = pattern;
    }

    @Override
    public boolean accept(String string) {
        return pattern.matcher(string).matches();
    }

}
