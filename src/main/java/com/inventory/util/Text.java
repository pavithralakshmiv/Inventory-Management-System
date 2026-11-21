package com.inventory.util;

public final class Text {

    private Text() {
    }

    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String email(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }

    public static boolean contains(String text, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return text != null && text.toLowerCase().contains(query.trim().toLowerCase());
    }
}
