package com.hangout.chatbot;

/**
 * Failure while talking to the LLM provider. Messages are safe to show: they never contain the key or raw responses.
 */
public class AiServiceException extends RuntimeException {
    
    public enum Type {
        NOT_CONFIGURED,
        AUTHENTICATION,
        RATE_LIMITED,
        UNAVAILABLE,
        INVALID_REQUEST,
        INVALID_RESPONSE
    }
    
    private final Type type;
    
    public AiServiceException(Type type, String message) {
        super(message);
        this.type = type;
    }
    
    public AiServiceException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
}
