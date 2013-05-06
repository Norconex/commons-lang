/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.io;

import java.util.regex.Pattern;

/**
 * Filters lines of text read from an InputStream decorated with 
 * {@link FilteredInputStream}, based on a given regular expression.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public class RegexInputStreamFilter implements IInputStreamFilter {

    private final Pattern pattern;
    
    public RegexInputStreamFilter(String regex) {
        super();
        this.pattern = Pattern.compile(regex);
    }

    public RegexInputStreamFilter(Pattern pattern) {
        super();
        this.pattern = pattern;
    }

    @Override
    public boolean accept(String string) {
        return pattern.matcher(string).matches();
    }

}
