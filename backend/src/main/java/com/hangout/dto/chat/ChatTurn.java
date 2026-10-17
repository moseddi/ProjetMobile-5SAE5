package com.hangout.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** One previous message of the conversation, kept by the browser only (nothing is stored server side). */
public record ChatTurn(
    @NotBlank @Pattern(regexp = "user|assistant") String role,
    @NotBlank @Size(max = 2000) String content
) {}
