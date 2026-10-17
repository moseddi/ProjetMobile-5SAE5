package com.hangout.controller;

import com.hangout.dto.loyalty.AssistantChatRequest;
import com.hangout.dto.loyalty.AssistantChatResponse;
import com.hangout.service.LoyaltyAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loyalty/assistant")
@RequiredArgsConstructor
public class LoyaltyAssistantController {

    private final LoyaltyAssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@RequestParam String userId, @Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(assistantService.chat(userId, request.getMessage()));
    }
}
