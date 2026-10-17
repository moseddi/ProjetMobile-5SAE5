package com.hangout.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the existing free-text Restaurant.openingHours field, e.g. "11:00 AM - 10:00 PM" or "11:00 - 22:00".
 */
public final class OpeningHoursParser {
    
    public record OpeningHours(LocalTime open, LocalTime close) {}
    
    private static final Pattern RANGE = Pattern.compile(
        "^\\s*(\\d{1,2}:\\d{2}\\s*(?:[AaPp][Mm])?)\\s*[-–]\\s*(\\d{1,2}:\\d{2}\\s*(?:[AaPp][Mm])?)\\s*$");
    private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("H:mm");
    
    private OpeningHoursParser() {}
    
    public static Optional<OpeningHours> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        Matcher m = RANGE.matcher(text);
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new OpeningHours(parseTime(m.group(1)), parseTime(m.group(2))));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
    
    private static LocalTime parseTime(String raw) {
        String t = raw.trim().toUpperCase(Locale.ENGLISH).replaceAll("\\s+", " ");
        if (t.endsWith("AM") || t.endsWith("PM")) {
            if (!t.contains(" ")) {
                t = t.substring(0, t.length() - 2) + " " + t.substring(t.length() - 2);
            }
            return LocalTime.parse(t, H12);
        }
        return LocalTime.parse(t, H24);
    }
}
