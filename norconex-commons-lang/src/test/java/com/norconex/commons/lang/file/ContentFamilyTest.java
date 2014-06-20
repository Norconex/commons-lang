package com.norconex.commons.lang.file;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class ContentFamilyTest {

    @Test
    public void testGetDisplayName() {
        
        Assert.assertEquals("Word Processor", ContentFamily.valueOf(
                "wordprocessor").getDisplayName(Locale.ENGLISH));
        Assert.assertEquals("Traitement de texte", ContentFamily.valueOf(
                "wordprocessor").getDisplayName(Locale.FRENCH));

        Assert.assertEquals("Spreadsheet", ContentFamily.forContentType(
                ContentType.TSV).getDisplayName(Locale.ENGLISH));
        
        Assert.assertTrue(ContentFamily.forContentType(
                ContentType.TSV).contains("text/tab-separated-values"));
    }
}
