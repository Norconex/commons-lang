package com.norconex.commons.lang.file;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.file.FileUtil;

public class FileUtilTest {

    @Test
    public void testSafeFileName() {
        String unsafe = "Voilà, à bientôt! :-)";
        String safe = FileUtil.toSafeFileName(unsafe);
        Assert.assertEquals(unsafe, FileUtil.fromSafeFileName(safe));
    }
}
