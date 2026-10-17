package com.hangout.controller;

import com.hangout.dto.loyalty.*;
import com.hangout.service.LoyaltyGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty/games")
@RequiredArgsConstructor
public class LoyaltyGameController {

    private final LoyaltyGameService gameService;

    @GetMapping("/quiz/today")
    public ResponseEntity<List<QuizQuestionDto>> getTodaysQuiz(@RequestParam String userId) {
        return ResponseEntity.ok(gameService.getTodaysQuiz(userId));
    }

    @PostMapping("/quiz/submit")
    public ResponseEntity<GameResultResponse> submitQuiz(@RequestParam String userId, @Valid @RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(gameService.submitQuiz(userId, request));
    }

    @PostMapping("/memory/start")
    public ResponseEntity<MemoryStartResponse> startMemory(@RequestParam String userId) {
        return ResponseEntity.ok(gameService.startMemory(userId));
    }

    @PostMapping("/memory/submit")
    public ResponseEntity<GameResultResponse> submitMemory(@RequestParam String userId, @Valid @RequestBody MemorySubmitRequest request) {
        return ResponseEntity.ok(gameService.submitMemory(userId, request));
    }

    @PostMapping("/scratch/play")
    public ResponseEntity<GameResultResponse> playScratchCard(@RequestParam String userId) {
        return ResponseEntity.ok(gameService.playScratchCard(userId));
    }

    @PostMapping("/wheel/spin")
    public ResponseEntity<GameResultResponse> spinWheel(@RequestParam String userId) {
        return ResponseEntity.ok(gameService.spinWheel(userId));
    }
}
