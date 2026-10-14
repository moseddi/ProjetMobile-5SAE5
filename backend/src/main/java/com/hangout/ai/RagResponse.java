package com.hangout.ai;

import java.util.List;

public record RagResponse(
        String queryUsed,
        String mode,          // QUERY | PROFILE | DEFAULT | NONE
        boolean generated,    // true si l'explication vient du LLM
        List<RagItem> items,
        String message
) {}