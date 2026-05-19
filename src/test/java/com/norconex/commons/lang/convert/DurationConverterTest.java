package com.norconex.commons.lang.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.time.DurationUnit;

class DurationConverterTest {

    @Test
    void testConvertEnglishAndFrenchDurations() {
        var converter = new DurationConverter();

        assertThat(converter.toString(Duration.ofSeconds(90)))
                .isEqualTo("90000");
        assertThat(converter.toType("2h 30m", Duration.class))
                .isEqualTo(Duration.ofHours(2).plusMinutes(30));
        assertThat(converter.toType("2j 3h", Duration.class))
                .isEqualTo(Duration.ofDays(2).plusHours(3));
        assertThat(converter.toType("1an2mois", Duration.class))
                .isEqualTo(Duration.ofMillis(
                        DurationUnit.YEAR.toMilliseconds()
                                + (2 * DurationUnit.MONTH.toMilliseconds())));
        assertThat(converter.toType("3sem 4j", Duration.class))
                .isEqualTo(Duration.ofDays(25));
    }
}