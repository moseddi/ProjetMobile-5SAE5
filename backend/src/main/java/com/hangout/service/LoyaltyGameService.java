package com.hangout.service;

import com.hangout.dto.loyalty.*;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.GameResult;
import com.hangout.model.MemorySession;
import com.hangout.model.PointTransaction;
import com.hangout.model.QuizQuestion;
import com.hangout.repository.GameResultRepository;
import com.hangout.repository.MemorySessionRepository;
import com.hangout.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Server-authoritative gameplay logic for every loyalty game. The frontend only
 * renders the interaction (flipping cards, scratching, spinning); every point
 * reward and every "did you win" decision is computed and persisted here.
 */
@Service
@RequiredArgsConstructor
public class LoyaltyGameService {

    private final LoyaltyService loyaltyService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final GameResultRepository gameResultRepository;
    private final MemorySessionRepository memorySessionRepository;
    private final SecureRandom random = new SecureRandom();

    private static final int QUIZ_QUESTIONS_PER_ROUND = 5;
    private static final int QUIZ_POINTS_PER_CORRECT = 10;
    private static final int MEMORY_PAIR_COUNT = 8;
    private static final int DAILY_LIMIT_QUIZ = 1;
    private static final int DAILY_LIMIT_MEMORY = 3;
    private static final int DAILY_LIMIT_SCRATCH = 1;
    private static final int DAILY_LIMIT_WHEEL = 1;

    // ------------------------------------------------------------------ quiz

    public List<QuizQuestionDto> getTodaysQuiz(String userId) {
        loyaltyService.assertDailyLimitNotExceeded(userId, GameResult.GameType.QUIZ, DAILY_LIMIT_QUIZ);
        List<QuizQuestion> pool = new ArrayList<>(quizQuestionRepository.findByActiveTrue());
        if (pool.isEmpty()) {
            throw new BadRequestException("No quiz questions are configured");
        }
        Collections.shuffle(pool, random);
        return pool.stream()
            .limit(QUIZ_QUESTIONS_PER_ROUND)
            .map(this::toQuestionDto)
            .collect(Collectors.toList());
    }

    private QuizQuestionDto toQuestionDto(QuizQuestion q) {
        QuizQuestionDto dto = new QuizQuestionDto();
        dto.setId(q.getId());
        dto.setQuestion(q.getQuestion());
        dto.setOptions(q.getOptions());
        dto.setCategory(q.getCategory());
        dto.setDifficulty(q.getDifficulty() != null ? q.getDifficulty().name() : null);
        return dto;
    }

    public GameResultResponse submitQuiz(String userId, QuizSubmitRequest request) {
        loyaltyService.assertDailyLimitNotExceeded(userId, GameResult.GameType.QUIZ, DAILY_LIMIT_QUIZ);
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new BadRequestException("No answers submitted");
        }
        if (request.getAnswers().size() > QUIZ_QUESTIONS_PER_ROUND) {
            throw new BadRequestException("Too many answers submitted");
        }

        List<GameResultResponse.QuizExplanation> explanations = new ArrayList<>();
        int correct = 0;
        for (QuizSubmitRequest.QuizAnswer answer : request.getAnswers()) {
            QuizQuestion question = quizQuestionRepository.findById(answer.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown question: " + answer.getQuestionId()));
            boolean isCorrect = question.getCorrectAnswerIndex() != null
                && question.getCorrectAnswerIndex().equals(answer.getSelectedIndex());
            if (isCorrect) correct++;

            GameResultResponse.QuizExplanation exp = new GameResultResponse.QuizExplanation();
            exp.setQuestionId(question.getId());
            exp.setCorrect(isCorrect);
            exp.setCorrectAnswerIndex(question.getCorrectAnswerIndex());
            exp.setExplanation(question.getExplanation());
            explanations.add(exp);
        }

        int total = request.getAnswers().size();
        int pointsEarned = correct * QUIZ_POINTS_PER_CORRECT;

        GameResult result = persistResult(userId, GameResult.GameType.QUIZ, pointsEarned,
            correct == total ? GameResult.GameOutcome.WIN : (correct == 0 ? GameResult.GameOutcome.LOSE : GameResult.GameOutcome.DRAW),
            correct);

        int newBalance = pointsEarned > 0
            ? loyaltyService.creditPoints(userId, pointsEarned, PointTransaction.TransactionType.EARN_GAME,
                "Quiz du jour: " + correct + "/" + total + " bonnes reponses", result.getId())
            : loyaltyService.currentBalance(userId);
        loyaltyService.checkAndClaimChallenges(userId);

        GameResultResponse response = new GameResultResponse();
        response.setGameType("QUIZ");
        response.setCorrectAnswers(correct);
        response.setTotalQuestions(total);
        response.setPointsEarned(pointsEarned);
        response.setNewBalance(newBalance);
        response.setOutcome(result.getOutcome().name());
        response.setMessage(correct == total ? "Score parfait !" : correct == 0 ? "Pas de bonne reponse cette fois." : "Bien joue !");
        response.setExplanations(explanations);
        return response;
    }

    // ---------------------------------------------------------------- memory

    private static final String[] MEMORY_SYMBOL_POOL = {
        "🍕", "🍣", "🍔", "🍝", "🥗", "🍰", "🍷", "🍹", "🧀", "🥐", "🍩", "🍦"
    };

    public MemoryStartResponse startMemory(String userId) {
        loyaltyService.assertDailyLimitNotExceeded(userId, GameResult.GameType.CARD_MEMORY, DAILY_LIMIT_MEMORY);

        List<String> chosen = new ArrayList<>(Arrays.asList(MEMORY_SYMBOL_POOL));
        Collections.shuffle(chosen, random);
        chosen = chosen.subList(0, MEMORY_PAIR_COUNT);

        List<String> board = new ArrayList<>();
        board.addAll(chosen);
        board.addAll(chosen);
        Collections.shuffle(board, random);

        MemorySession session = new MemorySession();
        session.setUserId(userId);
        session.setPairCount(MEMORY_PAIR_COUNT);
        session.setSymbols(board);
        session.setCompleted(false);
        session = memorySessionRepository.save(session);

        MemoryStartResponse response = new MemoryStartResponse();
        response.setSessionId(session.getId());
        response.setPairCount(MEMORY_PAIR_COUNT);
        response.setSymbols(board);
        return response;
    }

    public GameResultResponse submitMemory(String userId, MemorySubmitRequest request) {
        MemorySession session = memorySessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Memory session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new BadRequestException("This session does not belong to you");
        }
        if (Boolean.TRUE.equals(session.getCompleted())) {
            throw new BadRequestException("This session was already submitted");
        }
        int pairCount = session.getPairCount();
        int minPossibleMoves = pairCount; // best case: every pair found first try
        if (request.getMoves() < minPossibleMoves) {
            throw new BadRequestException("Invalid move count for this session");
        }

        session.setCompleted(true);
        memorySessionRepository.save(session);

        // Fewer moves -> more points. Perfect game = 40, decays toward a 10-point floor.
        int extraMoves = Math.max(0, request.getMoves() - minPossibleMoves);
        int pointsEarned = Math.max(10, 40 - extraMoves * 2);

        GameResult result = persistResult(userId, GameResult.GameType.CARD_MEMORY, pointsEarned,
            GameResult.GameOutcome.WIN, request.getMoves());

        int newBalance = loyaltyService.creditPoints(userId, pointsEarned, PointTransaction.TransactionType.EARN_GAME,
            "Memory Match termine en " + request.getMoves() + " coups", result.getId());
        loyaltyService.checkAndClaimChallenges(userId);

        GameResultResponse response = new GameResultResponse();
        response.setGameType("CARD_MEMORY");
        response.setScore(request.getMoves());
        response.setPointsEarned(pointsEarned);
        response.setNewBalance(newBalance);
        response.setOutcome("WIN");
        response.setMessage("Bravo, grille terminee en " + request.getMoves() + " coups !");
        return response;
    }

    // --------------------------------------------------------------- scratch

    private static final int[] SCRATCH_PRIZES =   {0, 10, 20, 50, 100};
    private static final int[] SCRATCH_WEIGHTS =  {30, 35, 20, 12, 3}; // sums to 100

    public GameResultResponse playScratchCard(String userId) {
        loyaltyService.assertDailyLimitNotExceeded(userId, GameResult.GameType.SCRATCH_CARD, DAILY_LIMIT_SCRATCH);

        int pointsEarned = weightedPick(SCRATCH_PRIZES, SCRATCH_WEIGHTS);

        GameResult result = persistResult(userId, GameResult.GameType.SCRATCH_CARD, pointsEarned,
            pointsEarned > 0 ? GameResult.GameOutcome.WIN : GameResult.GameOutcome.LOSE, null);

        int newBalance = pointsEarned > 0
            ? loyaltyService.creditPoints(userId, pointsEarned, PointTransaction.TransactionType.EARN_GAME,
                "Carte a gratter", result.getId())
            : loyaltyService.currentBalance(userId);
        loyaltyService.checkAndClaimChallenges(userId);

        GameResultResponse response = new GameResultResponse();
        response.setGameType("SCRATCH_CARD");
        response.setPointsEarned(pointsEarned);
        response.setNewBalance(newBalance);
        response.setOutcome(pointsEarned > 0 ? "WIN" : "LOSE");
        response.setMessage(pointsEarned > 0 ? "Tu as gagne " + pointsEarned + " points !" : "Pas de chance cette fois, reessaie demain !");
        return response;
    }

    // ----------------------------------------------------------------- wheel

    private static final int[] WHEEL_PRIZES =  {0, 5, 15, 25, 50, 75, 100, 150};
    private static final int[] WHEEL_WEIGHTS = {15, 25, 20, 15, 12, 7, 4, 2}; // sums to 100

    public GameResultResponse spinWheel(String userId) {
        loyaltyService.assertDailyLimitNotExceeded(userId, GameResult.GameType.DAILY_WHEEL, DAILY_LIMIT_WHEEL);

        int pointsEarned = weightedPick(WHEEL_PRIZES, WHEEL_WEIGHTS);

        GameResult result = persistResult(userId, GameResult.GameType.DAILY_WHEEL, pointsEarned,
            pointsEarned > 0 ? GameResult.GameOutcome.WIN : GameResult.GameOutcome.LOSE, null);

        int newBalance = pointsEarned > 0
            ? loyaltyService.creditPoints(userId, pointsEarned, PointTransaction.TransactionType.EARN_GAME,
                "Roue quotidienne", result.getId())
            : loyaltyService.currentBalance(userId);
        loyaltyService.checkAndClaimChallenges(userId);

        GameResultResponse response = new GameResultResponse();
        response.setGameType("DAILY_WHEEL");
        response.setPointsEarned(pointsEarned);
        response.setNewBalance(newBalance);
        response.setOutcome(pointsEarned > 0 ? "WIN" : "LOSE");
        response.setMessage(pointsEarned > 0 ? "La roue s'arrete sur " + pointsEarned + " points !" : "Dommage, la roue s'arrete sur 0. Retente demain !");
        return response;
    }

    // ------------------------------------------------------------------ util

    private int weightedPick(int[] values, int[] weights) {
        int totalWeight = Arrays.stream(weights).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < values.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return values[i];
            }
        }
        return values[values.length - 1];
    }

    private GameResult persistResult(String userId, GameResult.GameType type, int pointsEarned, GameResult.GameOutcome outcome, Integer score) {
        GameResult result = new GameResult();
        result.setUserId(userId);
        result.setGameType(type);
        result.setPointsEarned(pointsEarned);
        result.setOutcome(outcome);
        result.setScore(score);
        return gameResultRepository.save(result);
    }
}
