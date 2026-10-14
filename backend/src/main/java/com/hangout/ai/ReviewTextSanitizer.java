package com.hangout.ai;

import java.util.regex.Pattern;

public final class ReviewTextSanitizer {

    private static final Pattern HTML = Pattern.compile("<[^>]*>");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d .-]{7,}\\d)");

    private ReviewTextSanitizer() {}

    public static String clean(String s, int maxLength) {
        if (s == null) return "";
        String t = HTML.matcher(s).replaceAll(" ");
        t = URL.matcher(t).replaceAll("[lien]");
        t = EMAIL.matcher(t).replaceAll("[email]");
        t = PHONE.matcher(t).replaceAll("[tel]");
        t = t.replaceAll("\\s+", " ").trim();
        return t.length() <= maxLength ? t : t.substring(0, maxLength);
    }
}