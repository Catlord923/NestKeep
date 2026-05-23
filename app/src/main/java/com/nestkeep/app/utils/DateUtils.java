
package com.nestkeep.app.utils;

import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Converts LocalDate to "DD/MM/YYYY" string for UI display
    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    // Converts "DD/MM/YYYY" string from UI input back to LocalDate
    public static LocalDate parseDate(String dateString) {
        try {
            if (dateString == null || dateString.trim().isEmpty()) return null;
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Converts LocalTime to "HH:MM" string for UI display
    public static String formatTime(LocalTime time) {
        if (time == null) return "";
        return time.format(TIME_FORMATTER);
    }

    // Converts "HH:MM" string from UI input back to LocalTime
    public static LocalTime parseTime(String timeString) {
        try {
            if (timeString == null || timeString.trim().isEmpty()) return null;
            return LocalTime.parse(timeString, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}