package com.norconex.commons.lang.io;

import java.util.regex.Pattern;

/**
 * Filters lines of text read from an InputStream decorated with 
 * {@link FilteredInputStream}, based on a given regular expression.
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public class RegexInputStreamFilter implements IInputStreamFilter {

    private final Pattern pattern;
    
    private RegexInputStreamFilter(String regex) {
        super();
        this.pattern = Pattern.compile(regex);
    }

    private RegexInputStreamFilter(Pattern pattern) {
        super();
        this.pattern = pattern;
    }

    @Override
    public boolean accept(String string) {
        return pattern.matcher(string).matches();
    }

}
