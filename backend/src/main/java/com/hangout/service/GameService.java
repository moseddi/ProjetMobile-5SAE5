package com.hangout.service;

import com.hangout.dto.GamePlayRequest;
import com.hangout.dto.GamePlayResponse;
import com.hangout.dto.GameStatusResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.GameResult;
import com.hangout.model.PointTransaction;
import com.hangout.model.Restaurant;
import com.hangout.repository.GameResultRepository;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The four daily mini-games. A client can play ONE game per day (any of the four); the server draws the
 * reward (the app only animates it) and a game only ever gives points.
 */
@Service
public class GameService {

    /** Games shown on the Loyalty screen, in order. */
    public static final List<GameResult.GameType> DAILY_GAMES = List.of(
        GameResult.GameType.LUCKY_WHEEL,
        GameResult.GameType.SCRATCH_CARD,
        GameResult.GameType.CARD_MEMORY,
        GameResult.GameType.LUCKY_DICE
    );

    private final GameResultRepository gameResultRepository;
    private final RestaurantRepository restaurantRepository;
    private final LoyaltyService loyaltyService;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Object> userLocks = new ConcurrentHashMap<>();

    public GameService(GameResultRepository gameResultRepository,
                       RestaurantRepository restaurantRepository,
                       @Lazy LoyaltyService loyaltyService) {
        this.gameResultRepository = gameResultRepository;
        this.restaurantRepository = restaurantRepository;
        this.loyaltyService = loyaltyService;
    }

    public GamePlayResponse play(String userId, GameResult.GameType gameType, GamePlayRequest request) {
        if (!DAILY_GAMES.contains(gameType)) {
            throw new BadRequestException("This game is no longer available");
        }
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (Boolean.FALSE.equals(restaurant.getLoyaltyEnabled())) {
            throw new BadRequestException(restaurant.getName()
                + " does not offer loyalty rewards, so you cannot earn points there. Choose another restaurant.");
        }

        // One request at a time per user, so a double tap cannot play twice.
        synchronized (userLocks.computeIfAbsent(userId, id -> new Object())) {
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            GameResult playedToday = gameResultRepository
                .findByUserIdAndPlayedAtGreaterThanEqual(userId, startOfToday).stream().findFirst().orElse(null);
            if (playedToday != null) {
                throw new BadRequestException("You already played " + title(playedToday.getGameType())
                    + " today. Only one game per day: come back tomorrow!");
            }

            int points = drawPoints(gameType);

            GameResult result = new GameResult();
            result.setUserId(userId);
            result.setRestaurantId(restaurant.getId());
            result.setGameType(gameType);
            result.setOutcome(GameResult.GameOutcome.WIN);
            result.setPointsEarned(points);
            result.setPlayedAt(LocalDateTime.now());
            result = gameResultRepository.save(result);

            loyaltyService.awardPoints(userId, points, PointTransaction.TransactionType.EARN_GAME,
                "Won " + points + " points playing " + title(gameType) + " at " + restaurant.getName(),
                result.getId(), restaurant.getId());

            GamePlayResponse response = new GamePlayResponse();
            response.setId(result.getId());
            response.setGameType(gameType.name());
            response.setRestaurantId(restaurant.getId());
            response.setRestaurantName(restaurant.getName());
            response.setPointsEarned(points);
            response.setPlayedAt(result.getPlayedAt());
            response.setTotalPoints(loyaltyService.getBalance(userId));
            response.setRestaurantPoints(loyaltyService.restaurantBalance(userId, restaurant.getId()));
            return response;
        }
    }

    public List<GameStatusResponse> getStatus(String userId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = startOfToday.plusDays(1);
        List<GameResult> today = gameResultRepository.findByUserIdAndPlayedAtGreaterThanEqual(userId, startOfToday);
        Map<String, String> restaurantNames = new ConcurrentHashMap<>();

        GameResult playedGame = today.stream()
            .max((a, b) -> a.getPlayedAt().compareTo(b.getPlayedAt()))
            .orElse(null);

        List<GameStatusResponse> statuses = new ArrayList<>();
        for (GameResult.GameType type : DAILY_GAMES) {
            GameResult last = playedGame != null && playedGame.getGameType() == type ? playedGame : null;
            boolean locked = playedGame != null && last == null;
            String restaurantName = null;
            if (last != null && last.getRestaurantId() != null) {
                restaurantName = restaurantNames.computeIfAbsent(last.getRestaurantId(),
                    id -> restaurantRepository.findById(id).map(Restaurant::getName).orElse(""));
            }
            statuses.add(new GameStatusResponse(
                type.name(), title(type), description(type),
                last != null,
                locked,
                last != null ? last.getPointsEarned() : null,
                restaurantName != null && !restaurantName.isBlank() ? restaurantName : null,
                playedGame != null ? tomorrow : null));
        }
        return statuses;
    }

    public List<GameResult> getHistory(String userId) {
        return gameResultRepository.findByUserIdOrderByPlayedAtDesc(userId);
    }

    // ------------------------------------------------------------------ rewards

    private int drawPoints(GameResult.GameType type) {
        switch (type) {
            case LUCKY_WHEEL:
                return weighted(new int[]{5, 10, 20, 30, 50, 100}, new int[]{30, 25, 20, 13, 9, 3});
            case SCRATCH_CARD:
                return weighted(new int[]{5, 10, 15, 25, 50}, new int[]{30, 30, 20, 15, 5});
            case CARD_MEMORY:
                return weighted(new int[]{5, 10, 20, 50}, new int[]{35, 35, 20, 10});
            case LUCKY_DICE:
                // The face rolled (1-6) is worth 5 points per pip.
                return (random.nextInt(6) + 1) * 5;
            default:
                throw new BadRequestException("This game is no longer available");
        }
    }

    private int weighted(int[] values, int[] weights) {
        int total = 0;
        for (int w : weights) total += w;
        int roll = random.nextInt(total);
        for (int i = 0; i < values.length; i++) {
            roll -= weights[i];
            if (roll < 0) return values[i];
        }
        return values[values.length - 1];
    }

    private static String title(GameResult.GameType type) {
        switch (type) {
            case LUCKY_WHEEL: return "Lucky Wheel";
            case SCRATCH_CARD: return "Scratch Card";
            case CARD_MEMORY: return "Pick a Card";
            case LUCKY_DICE: return "Lucky Dice";
            default: return type.name();
        }
    }

    private static String description(GameResult.GameType type) {
        switch (type) {
            case LUCKY_WHEEL: return "Spin the wheel";
            case SCRATCH_CARD: return "Scratch to reveal";
            case CARD_MEMORY: return "Choose a card";
            case LUCKY_DICE: return "Roll the dice";
            default: return "";
        }
    }
}
