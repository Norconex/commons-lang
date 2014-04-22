package com.norconex.commons.lang.unit;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class DataUnitFormatterTest {

    @Test
    public void testFormat() {
        // non-breaking space: \u00A0
        Assert.assertEquals("3\u00A0GB", 
                new DataUnitFormatter().format(3, DataUnit.GB));
        Assert.assertEquals("2\u00A0MB",
                new DataUnitFormatter().format(3000, DataUnit.KB));
        Assert.assertEquals("2.9\u00A0MB", 
                new DataUnitFormatter(1).format(3000, DataUnit.KB));
        Assert.assertEquals("2,99\u00A0KB", new DataUnitFormatter(
                Locale.CANADA_FRENCH, 2).format(3071, DataUnit.B));
        Assert.assertEquals("10\u00A0000\u00A0KB", new DataUnitFormatter(
                Locale.CANADA_FRENCH, 2, true).format(10000, DataUnit.KB));
    }
}
