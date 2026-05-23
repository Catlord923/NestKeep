package com.nestkeep.app.utils;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.*;

public class DateUtilsTest {

    // formatDate

    @Test
    public void formatDate_validDate_returnsFormattedString() {
        LocalDate date = LocalDate.of(2025, 5, 21);
        assertEquals("21/05/2025", DateUtils.formatDate(date));
    }

    @Test
    public void formatDate_singleDigitDayAndMonth_paddsWithZero() {
        LocalDate date = LocalDate.of(2025, 1, 5);
        assertEquals("05/01/2025", DateUtils.formatDate(date));
    }

    @Test
    public void formatDate_null_returnsEmptyString() {
        assertEquals("", DateUtils.formatDate(null));
    }

    // parseDate

    @Test
    public void parseDate_validString_returnsLocalDate() {
        LocalDate result = DateUtils.parseDate("21/05/2025");
        assertNotNull(result);
        assertEquals(LocalDate.of(2025, 5, 21), result);
    }

    @Test
    public void parseDate_null_returnsNull() {
        assertNull(DateUtils.parseDate(null));
    }

    @Test
    public void parseDate_emptyString_returnsNull() {
        assertNull(DateUtils.parseDate(""));
    }

    @Test
    public void parseDate_whitespaceOnly_returnsNull() {
        assertNull(DateUtils.parseDate("   "));
    }

    @Test
    public void parseDate_invalidFormat_returnsNull() {
        assertNull(DateUtils.parseDate("2025-05-21"));
    }

    @Test
    public void parseDate_invalidDate_returnsNull() {
        assertNull(DateUtils.parseDate("99/99/9999"));
    }

    @Test
    public void formatDate_thenParseDate_roundTrips() {
        LocalDate original = LocalDate.of(2025, 12, 31);
        String formatted = DateUtils.formatDate(original);
        LocalDate parsed = DateUtils.parseDate(formatted);
        assertEquals(original, parsed);
    }

    // formatTime

    @Test
    public void formatTime_validTime_returnsFormattedString() {
        LocalTime time = LocalTime.of(14, 30);
        assertEquals("14:30", DateUtils.formatTime(time));
    }

    @Test
    public void formatTime_midnight_returnsZeros() {
        LocalTime time = LocalTime.of(0, 0);
        assertEquals("00:00", DateUtils.formatTime(time));
    }

    @Test
    public void formatTime_singleDigitHourAndMinute_paddsWithZero() {
        LocalTime time = LocalTime.of(9, 5);
        assertEquals("09:05", DateUtils.formatTime(time));
    }

    @Test
    public void formatTime_null_returnsEmptyString() {
        assertEquals("", DateUtils.formatTime(null));
    }

    // parseTime

    @Test
    public void parseTime_validString_returnsLocalTime() {
        LocalTime result = DateUtils.parseTime("14:30");
        assertNotNull(result);
        assertEquals(LocalTime.of(14, 30), result);
    }

    @Test
    public void parseTime_null_returnsNull() {
        assertNull(DateUtils.parseTime(null));
    }

    @Test
    public void parseTime_emptyString_returnsNull() {
        assertNull(DateUtils.parseTime(""));
    }

    @Test
    public void parseTime_invalidFormat_returnsNull() {
        assertNull(DateUtils.parseTime("2:30 PM"));
    }

    @Test
    public void formatTime_thenParseTime_roundTrips() {
        LocalTime original = LocalTime.of(23, 59);
        String formatted = DateUtils.formatTime(original);
        LocalTime parsed = DateUtils.parseTime(formatted);
        assertEquals(original, parsed);
    }
}
