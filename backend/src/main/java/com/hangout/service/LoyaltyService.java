package com.hangout.service;

import com.hangout.dto.LoyaltyOverview;
import com.hangout.dto.MemberLookupResponse;
import com.hangout.dto.VoucherResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Notification;
import com.hangout.model.PointTransaction;
import com.hangout.model.Restaurant;
import com.hangout.model.RewardType;
import com.hangout.model.User;
import com.hangout.model.Voucher;
import com.hangout.repository.PointTransactionRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.UserRepository;
import com.hangout.repository.VoucherRepository;
import com.hangout.util.Newest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private static final int VOUCHER_VALID_DAYS = 30;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RestaurantRepository restaurantRepository;
    private final VoucherRepository voucherRepository;
    private final GameService gameService;

    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------------ earning

    @Transactional
    public void awardPoints(String userId, int points, PointTransaction.TransactionType type,
                             String description, String relatedId) {
        awardPoints(userId, points, type, description, relatedId, null);
    }

    /** Credits points; when [restaurantId] is given they belong to that restaurant. */
    @Transactional
    public void awardPoints(String userId, int points, PointTransaction.TransactionType type,
                             String description, String relatedId, String restaurantId) {
        if (points <= 0) return;
        if (restaurantId != null && !isLoyaltyEnabled(restaurantId)) return;

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLoyaltyPoints(orZero(user.getLoyaltyPoints()) + points);
        userRepository.save(user);

        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setRestaurantId(restaurantId);
        tx.setPoints(points);
        tx.setType(type);
        tx.setDescription(description);
        tx.setRelatedId(relatedId);
        pointTransactionRepository.save(tx);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("You earned " + points + " points!");
        notification.setMessage(description);
        notification.setType(Notification.NotificationType.LOYALTY);
        notification.setRelatedId(relatedId);
        notificationService.createNotification(notification);
    }

    /** Whether a restaurant runs the loyalty programme (on unless its owner switched it off). */
    public boolean isLoyaltyEnabled(String restaurantId) {
        return restaurantRepository.findById(restaurantId)
            .map(r -> !Boolean.FALSE.equals(r.getLoyaltyEnabled()))
            .orElse(false);
    }

    // ------------------------------------------------------------------ balances

    public int getBalance(String userId) {
        return orZero(userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
            .getLoyaltyPoints());
    }

    /** Points per restaurant, from the transaction ledger. */
    public Map<String, Integer> restaurantBalances(String userId) {
        Map<String, Integer> balances = new HashMap<>();
        for (PointTransaction tx : pointTransactionRepository.findByUserId(userId)) {
            if (tx.getRestaurantId() != null && tx.getPoints() != null) {
                balances.merge(tx.getRestaurantId(), tx.getPoints(), Integer::sum);
            }
        }
        return balances;
    }

    public int restaurantBalance(String userId, String restaurantId) {
        return Math.max(0, restaurantBalances(userId).getOrDefault(restaurantId, 0));
    }

    public List<PointTransaction> getHistory(String userId) {
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ------------------------------------------------------------------ overview

    public LoyaltyOverview getOverview(String userId) {
        int total = getBalance(userId);
        Map<String, Integer> balances = restaurantBalances(userId);

        List<LoyaltyOverview.RestaurantLoyalty> restaurants = restaurantRepository.findAll().stream()
            .map(r -> toRestaurantLoyalty(r, balances.getOrDefault(r.getId(), 0)))
            .sorted(Comparator.comparingInt(LoyaltyOverview.RestaurantLoyalty::getPoints).reversed()
                .thenComparing(LoyaltyOverview.RestaurantLoyalty::getName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .collect(Collectors.toList());

        int assigned = balances.values().stream().mapToInt(v -> Math.max(0, v)).sum();

        LoyaltyOverview overview = new LoyaltyOverview();
        overview.setTotalPoints(total);
        overview.setMemberCode(memberCodeFor(userId));
        overview.setUnassignedPoints(Math.max(0, total - assigned));
        overview.setGames(gameService.getStatus(userId));
        overview.setRestaurants(restaurants);
        return overview;
    }

    private LoyaltyOverview.RestaurantLoyalty toRestaurantLoyalty(Restaurant restaurant, int rawPoints) {
        int points = Math.max(0, rawPoints);
        LoyaltyOverview.RestaurantLoyalty item = new LoyaltyOverview.RestaurantLoyalty();
        item.setRestaurantId(restaurant.getId());
        item.setName(restaurant.getName());
        item.setImage(restaurant.getImage());
        item.setCuisine(restaurant.getCuisine());
        boolean enabled = !Boolean.FALSE.equals(restaurant.getLoyaltyEnabled());
        item.setPoints(points);
        item.setLoyaltyEnabled(enabled);

        List<LoyaltyOverview.Offer> offers = new ArrayList<>();
        for (RewardType type : RewardType.values()) {
            LoyaltyOverview.Offer offer = new LoyaltyOverview.Offer();
            offer.setType(type.name());
            offer.setTitle(type.getTitle());
            offer.setDescription(type.getDescription());
            offer.setCost(type.getCost());
            offer.setCanRedeem(enabled && points >= type.getCost());
            offer.setMissingPoints(Math.max(0, type.getCost() - points));
            offers.add(offer);
        }
        item.setOffers(offers);
        return item;
    }

    // ------------------------------------------------------------------ member QR

    /** The client's one permanent code: created the first time it is needed, never changed. */
    public String memberCodeFor(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getMemberCode() != null && !user.getMemberCode().isBlank()) {
            return user.getMemberCode();
        }
        String code;
        do {
            StringBuilder sb = new StringBuilder("HOM-");
            for (int i = 0; i < 10; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (userRepository.existsByMemberCode(code));
        user.setMemberCode(code);
        userRepository.save(user);
        return code;
    }

    /** Cashier scans a client's QR: shows who it is and what they can use at THIS restaurant. */
    public MemberLookupResponse lookupMember(String restaurantId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase().replace(" ", "");
        User client = userRepository.findByMemberCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("No client found for this code"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        List<Voucher> mine = voucherRepository.findByUserIdAndRestaurantId(client.getId(), restaurantId);
        List<VoucherResponse> active = mine.stream()
            .sorted(Newest.first(Voucher::getId))
            .map(v -> toVoucherResponse(v, restaurant))
            .filter(v -> "ACTIVE".equals(v.getStatus()))
            .collect(Collectors.toList());

        MemberLookupResponse response = new MemberLookupResponse();
        response.setMemberCode(client.getMemberCode());
        response.setName(client.getName());
        response.setAvatar(client.getAvatar());
        response.setMemberSince(client.getCreatedAt());
        response.setPointsAtRestaurant(restaurantBalance(client.getId(), restaurantId));
        response.setRestaurantName(restaurant.getName());
        response.setActiveVouchers(active);
        response.setUsedVouchers((int) mine.stream().filter(v -> v.getStatus() == Voucher.Status.USED).count());
        return response;
    }

    /** The cashier validates a reward: it can be used once, only at the restaurant it was bought for. */
    @Transactional
    public VoucherResponse useVoucher(String restaurantId, String voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
            .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        if (!restaurantId.equals(voucher.getRestaurantId())) {
            throw new BadRequestException("This voucher belongs to another restaurant");
        }
        if (voucher.getStatus() == Voucher.Status.USED) {
            throw new BadRequestException("This voucher was already used");
        }
        if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This voucher has expired");
        }
        voucher.setStatus(Voucher.Status.USED);
        voucher.setUsedAt(LocalDateTime.now());
        voucher = voucherRepository.save(voucher);

        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);

        Notification notification = new Notification();
        notification.setUserId(voucher.getUserId());
        notification.setTitle(voucher.getRewardType().getTitle() + " used");
        notification.setMessage("Your reward was validated at " + (restaurant != null ? restaurant.getName() : "the restaurant") + ". Enjoy!");
        notification.setType(Notification.NotificationType.LOYALTY);
        notification.setRelatedId(voucher.getId());
        notificationService.createNotification(notification);

        return toVoucherResponse(voucher, restaurant);
    }

    // ------------------------------------------------------------------ redeeming

    @Transactional
    public VoucherResponse redeem(String userId, String restaurantId, String rewardTypeName) {
        RewardType type;
        try {
            type = RewardType.valueOf(rewardTypeName == null ? "" : rewardTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown reward: " + rewardTypeName);
        }
        Restaurant restaurant = restaurantRepository.findById(restaurantId == null ? "" : restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (Boolean.FALSE.equals(restaurant.getLoyaltyEnabled())) {
            throw new BadRequestException(restaurant.getName() + " does not offer loyalty rewards at the moment.");
        }

        int available = restaurantBalance(userId, restaurant.getId());
        if (available < type.getCost()) {
            throw new BadRequestException("Not enough points at " + restaurant.getName() + ": you have "
                + available + " and need " + type.getCost() + " (" + (type.getCost() - available) + " more).");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLoyaltyPoints(Math.max(0, orZero(user.getLoyaltyPoints()) - type.getCost()));
        userRepository.save(user);

        Voucher voucher = new Voucher();
        voucher.setUserId(userId);
        voucher.setRestaurantId(restaurant.getId());
        voucher.setRewardType(type);
        voucher.setPointsSpent(type.getCost());
        voucher.setCode(newCode());
        voucher.setStatus(Voucher.Status.ACTIVE);
        voucher.setExpiresAt(LocalDateTime.now().plusDays(VOUCHER_VALID_DAYS));
        voucher = voucherRepository.save(voucher);

        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setRestaurantId(restaurant.getId());
        tx.setPoints(-type.getCost());
        tx.setType(PointTransaction.TransactionType.REDEEM);
        tx.setDescription(type.getTitle() + " at " + restaurant.getName());
        tx.setRelatedId(voucher.getId());
        pointTransactionRepository.save(tx);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(type.getTitle() + " unlocked!");
        notification.setMessage("Show code " + voucher.getCode() + " at " + restaurant.getName()
            + ". Valid " + VOUCHER_VALID_DAYS + " days.");
        notification.setType(Notification.NotificationType.LOYALTY);
        notification.setRelatedId(voucher.getId());
        notificationService.createNotification(notification);

        return toVoucherResponse(voucher, restaurant);
    }

    public List<VoucherResponse> getVouchers(String userId) {
        Map<String, Restaurant> restaurants = new HashMap<>();
        return voucherRepository.findByUserId(userId).stream()
            .sorted(Newest.first(Voucher::getId))
            .map(v -> toVoucherResponse(v, restaurants.computeIfAbsent(
                v.getRestaurantId() == null ? "" : v.getRestaurantId(),
                id -> restaurantRepository.findById(id).orElse(null))))
            .collect(Collectors.toList());
    }

    private VoucherResponse toVoucherResponse(Voucher voucher, Restaurant restaurant) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setRestaurantId(voucher.getRestaurantId());
        response.setRestaurantName(restaurant != null ? restaurant.getName() : "Unknown");
        response.setRestaurantImage(restaurant != null ? restaurant.getImage() : "");
        response.setRewardType(voucher.getRewardType().name());
        response.setTitle(voucher.getRewardType().getTitle());
        response.setDescription(voucher.getRewardType().getDescription());
        response.setPointsSpent(orZero(voucher.getPointsSpent()));
        response.setCreatedAt(voucher.getCreatedAt());
        response.setExpiresAt(voucher.getExpiresAt());
        response.setUsedAt(voucher.getUsedAt());

        String status = voucher.getStatus() == Voucher.Status.USED ? "USED"
            : (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(LocalDateTime.now()) ? "EXPIRED" : "ACTIVE");
        response.setStatus(status);
        return response;
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("HO-");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (voucherRepository.existsByCode(code));
        return code;
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
