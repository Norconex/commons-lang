package com.norconex.commons.lang.file;

import java.util.Locale;

public final class ContentFamily {

    
    private ContentFamily(String[] contentTypes) {
        
        //TODO check those that starts with DEFAULT. and add all matching
        //ones.  Unless we want to keep the wildcard isntead and match them
        //at runtime?  check what is most efficient, considering we want
        //anyone to be able to overwride the mappings at some point.
        
        //TODO have a single ContentType-families point to the family key only
        // and have a separate ContentFamily[_lang].properties for translations
        
    }

    
    public ContentFamily forType(ContentType contentType) {
        //TODO use a weak hashmap for caching most frequent associations
        //btween content types and contentFamily
        return null;
    }

    public String getId() {
        //correspond to the .properties key.
        return null;
    }

    public String getName() {
        return null;
    }
    public String getName(Locale locale) {
        return null;
    }
    
    public ContentType[] getContentTypes() {
        return null;
    }
}
