package com.norconex.commons.lang.file;

import org.junit.Assert;
import org.junit.Test;

public class ContentTypeTest {

    @Test
    public void testGetDisplayName() {
        Assert.assertEquals("Adobe Portable Document Format",
                ContentType.PDF.getDisplayName());
        Assert.assertEquals("Open eBook Publication Structure", ContentType.get(
                "application/oebps-package+xml").getDisplayName());
        Assert.assertEquals(".pdf", ContentType.PDF.getExtension());
        Assert.assertEquals(".wpd", 
                ContentType.get("application/wordperfect").getExtension());
        Assert.assertArrayEquals(new String[]{ ".wpd", ".wp", ".wp5", ".wp6" },
                ContentType.get("application/wordperfect").getExtensions());
    }
}
