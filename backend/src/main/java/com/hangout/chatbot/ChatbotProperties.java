package com.hangout.chatbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chatbot")
public class ChatbotProperties {
    private int maxMessageLength = 500;
    /** Previous messages sent back to the model, to keep context while limiting tokens. */
    private int maxHistory = 10;
    private int rateLimitPerMinute = 10;
    private int maxSuggestedSlots = 8;
}
