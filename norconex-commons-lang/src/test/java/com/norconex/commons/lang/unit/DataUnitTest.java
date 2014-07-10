package com.norconex.commons.lang.unit;

import org.junit.Assert;
import org.junit.Test;

public class DataUnitTest {

    @Test
    public void testConversions() {
        Assert.assertEquals(0, DataUnit.B.toKilobytes(5));
        Assert.assertEquals(2048, DataUnit.KB.toBytes(2));
        Assert.assertEquals(3, DataUnit.MB.toGigabytes(3072));
    }
}
