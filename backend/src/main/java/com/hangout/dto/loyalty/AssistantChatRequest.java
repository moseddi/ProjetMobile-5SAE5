package com.hangout.dto.loyalty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssistantChatRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 500, message = "Message is too long")
    private String message;
}
