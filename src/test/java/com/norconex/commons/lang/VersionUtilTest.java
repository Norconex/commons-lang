package com.norconex.commons.lang;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VersionUtilTest {

    @Test
    void testGetVersion() {
        Assertions.assertTrue(StringUtils.isNotBlank(
                VersionUtil.getVersion(getClass())));
    }

    @Test
    void testGetDetailedVersion() {
        Assertions.assertNull(VersionUtil.fromJarManifest(getClass()));
        Assertions.assertNotNull(
                VersionUtil.fromJarManifest(StringUtils.class));

        Assertions.assertNotNull(
                VersionUtil.fromUnpackedMavenPomXml(getClass()));
        Assertions.assertNull(
                VersionUtil.fromUnpackedMavenPomXml(String.class));
    }
}
