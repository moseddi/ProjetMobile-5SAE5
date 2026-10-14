package com.hangout.ai;

import jakarta.validation.constraints.Size;

public record RagRequest(
        @Size(max = 300, message = "Demande trop longue (300 caractères max)")
        String query,
        String userId,
        Integer limit
) {}