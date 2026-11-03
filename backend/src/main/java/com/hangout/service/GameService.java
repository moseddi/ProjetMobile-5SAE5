package com.hangout.service;

import com.hangout.dto.GamePlayRequest;
import com.hangout.exception.BadRequestException;
import com.hangout.model.GameResult;
import com.hangout.model.PointTransaction;
import com.hangout.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameResultRepository gameResultRepository;
    private final LoyaltyService loyaltyService;

    @Transactional
    public GameResult play(String userId, GameResult.GameType gameType, GamePlayRequest request) {
        GameResult.GameOutcome outcome;
        try {
            outcome = GameResult.GameOutcome.valueOf(request.getOutcome().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid outcome: " + request.getOutcome());
        }

        // The mini-games compute their own reward client-side (card pick / wheel spin);
        // the server trusts and records that amount rather than re-deriving it from the outcome.
        int points = request.getScore() != null
            ? Math.max(0, request.getScore())
            : switch (outcome) {
                case WIN -> 50;
                case DRAW -> 20;
                case LOSE -> 10;
            };

        GameResult result = new GameResult();
        result.setUserId(userId);
        result.setGameType(gameType);
        result.setOutcome(outcome);
        result.setPointsEarned(points);
        result = gameResultRepository.save(result);

        loyaltyService.awardPoints(userId, points, PointTransaction.TransactionType.EARN_GAME,
            "Played " + gameType.name().replace('_', ' ').toLowerCase(), result.getId());

        return result;
    }

    public List<GameResult> getHistory(String userId) {
        return gameResultRepository.findByUserIdOrderByPlayedAtDesc(userId);
    }
}
