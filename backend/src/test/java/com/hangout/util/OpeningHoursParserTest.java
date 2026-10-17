package com.hangout.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningHoursParserTest {
    
    @Test
    void parses12HourFormatUsedBySeedData() {
        var hours = OpeningHoursParser.parse("11:00 AM - 10:00 PM").orElseThrow();
        assertThat(hours.open()).isEqualTo(LocalTime.of(11, 0));
        assertThat(hours.close()).isEqualTo(LocalTime.of(22, 0));
    }
    
    @Test
    void parses24HourFormat() {
        var hours = OpeningHoursParser.parse("11:30 - 22:30").orElseThrow();
        assertThat(hours.open()).isEqualTo(LocalTime.of(11, 30));
        assertThat(hours.close()).isEqualTo(LocalTime.of(22, 30));
    }
    
    @Test
    void parsesNoonAndCompactAmPm() {
        var hours = OpeningHoursParser.parse("12:00PM - 11:00pm").orElseThrow();
        assertThat(hours.open()).isEqualTo(LocalTime.NOON);
        assertThat(hours.close()).isEqualTo(LocalTime.of(23, 0));
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Tous les jours", "25:00 - 26:00", "11h - 22h"})
    void returnsEmptyForUnknownFormats(String text) {
        assertThat(OpeningHoursParser.parse(text)).isEmpty();
    }
}
