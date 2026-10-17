package com.hangout.controller;

import com.hangout.dto.AiHangoutRequest;
import com.hangout.dto.AiHangoutResponse;
import com.hangout.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/hangout-assistant")
    public ResponseEntity<AiHangoutResponse> hangoutAssistant(@Valid @RequestBody AiHangoutRequest request) {
        return ResponseEntity.ok(aiService.suggestHangout(request));
    }
}
