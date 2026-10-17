package com.hangout.service;

import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.*;
import com.hangout.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the authoritative points ledger and the rewards catalogue /
 * redemption flow. MongoTemplate is mocked so these run without a database.
 */
@DisplayName("LoyaltyService - points ledger and rewards (unit)")
class LoyaltyServiceTest {

    private UserRepository userRepository;
    private PointTransactionRepository pointTransactionRepository;
    private RewardRepository rewardRepository;
    private RedemptionRepository redemptionRepository;
    private GameResultRepository gameResultRepository;
    private MongoTemplate mongoTemplate;
    private LoyaltyService loyaltyService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        pointTransactionRepository = mock(PointTransactionRepository.class);
        rewardRepository = mock(RewardRepository.class);
        redemptionRepository = mock(RedemptionRepository.class);
        gameResultRepository = mock(GameResultRepository.class);
        mongoTemplate = mock(MongoTemplate.class);

        loyaltyService = new LoyaltyService(userRepository, pointTransactionRepository,
            rewardRepository, redemptionRepository, gameResultRepository, mongoTemplate);

        when(pointTransactionRepository.save(any(PointTransaction.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private User userWithBalance(int points) {
        User user = new User();
        user.setId("u1");
        user.setLoyaltyPoints(points);
        return user;
    }

    @Test
    @DisplayName("creditPoints atomically increments the balance and records a transaction")
    void creditPoints_updatesBalanceAndRecordsTransaction() {
        User updated = userWithBalance(150);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
            .thenReturn(updated);

        int newBalance = loyaltyService.creditPoints("u1", 50, PointTransaction.TransactionType.EARN_GAME, "Quiz", "game-1");

        assertThat(newBalance).isEqualTo(150);
        verify(pointTransactionRepository).save(argThat(tx ->
            tx.getUserId().equals("u1") && tx.getPoints() == 50 && tx.getType() == PointTransaction.TransactionType.EARN_GAME));
    }

    @Test
    @DisplayName("creditPoints rejects a non-positive amount")
    void creditPoints_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> loyaltyService.creditPoints("u1", 0, PointTransaction.TransactionType.EARN_GAME, "x", null))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("creditPoints on an unknown user throws ResourceNotFoundException")
    void creditPoints_unknownUser_throwsNotFound() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> loyaltyService.creditPoints("ghost", 10, PointTransaction.TransactionType.EARN_GAME, "x", null))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("debitPoints succeeds when the balance is sufficient")
    void debitPoints_sufficientBalance_succeeds() {
        User updated = userWithBalance(50);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
            .thenReturn(updated);

        int newBalance = loyaltyService.debitPoints("u1", 100, PointTransaction.TransactionType.REDEEM, "Reward", "r1");

        assertThat(newBalance).isEqualTo(50);
    }

    @Test
    @DisplayName("debitPoints throws when the atomic guard rejects an insufficient balance")
    void debitPoints_insufficientBalance_throwsBadRequest() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
            .thenReturn(null);
        when(userRepository.existsById("u1")).thenReturn(true);

        assertThatThrownBy(() -> loyaltyService.debitPoints("u1", 1000, PointTransaction.TransactionType.REDEEM, "x", null))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Insufficient");

        verify(pointTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("creditOnce never credits the same relatedId twice (idempotent challenge rewards)")
    void creditOnce_isIdempotent() {
        when(pointTransactionRepository.findByUserIdAndTypeAndRelatedId("u1", PointTransaction.TransactionType.EARN_CHALLENGE, "ch1"))
            .thenReturn(Optional.of(new PointTransaction()));

        boolean awarded = loyaltyService.creditOnce("u1", 20, PointTransaction.TransactionType.EARN_CHALLENGE, "Challenge", "ch1");

        assertThat(awarded).isFalse();
        verify(mongoTemplate, never()).findAndModify(any(), any(), any(), eq(User.class));
    }

    @Test
    @DisplayName("tier thresholds classify balances correctly")
    void tierFor_classifiesBalances() {
        assertThat(loyaltyService.tierFor(0)).isEqualTo("Bronze");
        assertThat(loyaltyService.tierFor(499)).isEqualTo("Bronze");
        assertThat(loyaltyService.tierFor(500)).isEqualTo("Silver");
        assertThat(loyaltyService.tierFor(1500)).isEqualTo("Gold");
        assertThat(loyaltyService.tierFor(3000)).isEqualTo("Platinum");
    }

    @Test
    @DisplayName("redeemReward fails when the reward does not exist")
    void redeemReward_unknownReward_throwsNotFound() {
        when(rewardRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loyaltyService.redeemReward("u1", "ghost"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("redeemReward fails when the reward is inactive")
    void redeemReward_inactiveReward_throwsBadRequest() {
        Reward reward = new Reward();
        reward.setId("r1");
        reward.setActive(false);
        when(rewardRepository.findById("r1")).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> loyaltyService.redeemReward("u1", "r1"))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("redeemReward fails when stock is tracked and exhausted")
    void redeemReward_outOfStock_throwsBadRequest() {
        Reward reward = new Reward();
        reward.setId("r1");
        reward.setActive(true);
        reward.setStock(0);
        reward.setPointsCost(100);
        when(rewardRepository.findById("r1")).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> loyaltyService.redeemReward("u1", "r1"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("stock");
    }

    @Test
    @DisplayName("redeemReward debits points, decrements stock and records the redemption on success")
    void redeemReward_success_debitsAndRecords() {
        Reward reward = new Reward();
        reward.setId("r1");
        reward.setName("Free dessert");
        reward.setActive(true);
        reward.setStock(5);
        reward.setPointsCost(200);
        when(rewardRepository.findById("r1")).thenReturn(Optional.of(reward));

        Reward stockUpdated = new Reward();
        stockUpdated.setId("r1");
        stockUpdated.setStock(4);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Reward.class)))
            .thenReturn(stockUpdated);

        User userUpdated = userWithBalance(50);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
            .thenReturn(userUpdated);

        when(redemptionRepository.save(any(Redemption.class))).thenAnswer(inv -> {
            Redemption r = inv.getArgument(0);
            r.setId("red1");
            return r;
        });

        var dto = loyaltyService.redeemReward("u1", "r1");

        assertThat(dto.getId()).isEqualTo("red1");
        assertThat(dto.getRewardName()).isEqualTo("Free dessert");
        assertThat(dto.getPointsCost()).isEqualTo(200);
        verify(pointTransactionRepository).save(argThat(tx -> tx.getPoints() == -200));
    }
}
