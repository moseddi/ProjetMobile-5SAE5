package com.hangout.chatbot;

import java.util.Locale;

/**
 * Closed list of what the assistant may handle. Anything else is OUT_OF_SCOPE.
 */
public enum ChatIntent {
    CREATE,
    CHECK_AVAILABILITY,
    MODIFY,
    CANCEL,
    STATUS,
    INFO,
    BOOK_EVENT,
    CHECK_EVENTS,
    CANCEL_EVENT_BOOKING,
    EVENT_STATUS,
    OUT_OF_SCOPE;
    
    /** Unknown or missing values from the model are treated as out of scope, never guessed. */
    public static ChatIntent fromModel(String value) {
        if (value == null) {
            return OUT_OF_SCOPE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OUT_OF_SCOPE;
        }
    }
}
