package com.hangout.chatbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Groq settings (prefix "groq"). The key comes only from the GROQ_API_KEY environment variable.
 */
@Data
@Component
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {
    private String apiKey;
    private String model = "openai/gpt-oss-20b";
    private String baseUrl = "https://api.groq.com/openai/v1";
    private int timeoutSeconds = 15;
    private int maxCompletionTokens = 1024;
    /** Strict JSON schema output (supported by openai/gpt-oss-*); false falls back to plain JSON mode. */
    private boolean structuredOutput = true;
    
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
    
    // Lombok's toString would print the key in logs
    @Override
    public String toString() {
        return "GroqProperties(model=" + model + ", baseUrl=" + baseUrl + ", apiKey=" + (hasApiKey() ? "***" : "<absent>") + ")";
    }
}
