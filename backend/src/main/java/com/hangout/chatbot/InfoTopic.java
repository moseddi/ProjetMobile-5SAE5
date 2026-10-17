package com.hangout.chatbot;

import java.util.Locale;

public enum InfoTopic {
    REQUIRED_INFO,
    HOW_TO_CANCEL,
    HOW_TO_MODIFY,
    OPENING_HOURS,
    GENERAL;
    
    public static InfoTopic fromModel(String value) {
        if (value == null) {
            return GENERAL;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}
