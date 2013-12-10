package com.norconex.commons.lang.io;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class FileUtilTest {

    @Test
    public void testSafeFileName() {
        String unsafe = "Voilà, à bientôt! :-)";
        String safe = FileUtil.toSafeFileName(unsafe);
        Assert.assertEquals(unsafe, FileUtil.fromSafeFileName(safe));
    }
}
