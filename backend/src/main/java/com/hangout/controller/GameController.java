package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.GamePlayRequest;
import com.hangout.exception.BadRequestException;
import com.hangout.model.GameResult;
import com.hangout.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/{gameType}/play")
    public ResponseEntity<GameResult> play(@PathVariable String gameType, @Valid @RequestBody GamePlayRequest request) {
        GameResult.GameType type;
        try {
            type = GameResult.GameType.valueOf(gameType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown game type: " + gameType);
        }
        return ResponseEntity.ok(gameService.play(SecurityUtils.getCurrentUserId(), type, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<GameResult>> getHistory() {
        return ResponseEntity.ok(gameService.getHistory(SecurityUtils.getCurrentUserId()));
    }
}
