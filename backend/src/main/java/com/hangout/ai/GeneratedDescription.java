package com.hangout.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeneratedDescription(
        String titre,
        String description,
        List<String> hashtags,
        @JsonProperty("appel_action") String appelAction
) {}