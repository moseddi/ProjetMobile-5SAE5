package com.hangout.service;

import com.hangout.dto.loyalty.*;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.*;
import com.hangout.repository.GameResultRepository;
import com.hangout.repository.MemorySessionRepository;
import com.hangout.repository.QuizQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests proving every loyalty game is scored server-side: the client
 * cannot invent correct quiz answers, cannot claim an impossible move count,
 * and cannot bypass a daily attempt limit.
 */
@DisplayName("LoyaltyGameService - server-authoritative gameplay (unit)")
class LoyaltyGameServiceTest {

    private LoyaltyService loyaltyService;
    private QuizQuestionRepository quizQuestionRepository;
    private GameResultRepository gameResultRepository;
    private MemorySessionRepository memorySessionRepository;
    private LoyaltyGameService gameService;

    @BeforeEach
    void setUp() {
        loyaltyService = mock(LoyaltyService.class);
        quizQuestionRepository = mock(QuizQuestionRepository.class);
        gameResultRepository = mock(GameResultRepository.class);
        memorySessionRepository = mock(MemorySessionRepository.class);

        gameService = new LoyaltyGameService(loyaltyService, quizQuestionRepository, gameResultRepository, memorySessionRepository);

        when(gameResultRepository.save(any(GameResult.class))).thenAnswer(inv -> {
            GameResult r = inv.getArgument(0);
            r.setId("result-1");
            return r;
        });
        when(loyaltyService.creditPoints(anyString(), anyInt(), any(), anyString(), anyString())).thenReturn(999);
        when(loyaltyService.currentBalance(anyString())).thenReturn(500);
    }

    private QuizQuestion question(String id, int correctIndex) {
        QuizQuestion q = new QuizQuestion();
        q.setId(id);
        q.setQuestion("Q" + id);
        q.setOptions(List.of("A", "B", "C", "D"));
        q.setCorrectAnswerIndex(correctIndex);
        q.setExplanation("explanation");
        return q;
    }

    @Test
    @DisplayName("submitQuiz scores strictly against the server-stored correct answer, never the client's claim")
    void submitQuiz_scoresAgainstServerTruth() {
        when(quizQuestionRepository.findById("q1")).thenReturn(Optional.of(question("q1", 2)));
        when(quizQuestionRepository.findById("q2")).thenReturn(Optional.of(question("q2", 0)));

        QuizSubmitRequest request = new QuizSubmitRequest();
        QuizSubmitRequest.QuizAnswer a1 = new QuizSubmitRequest.QuizAnswer();
        a1.setQuestionId("q1");
        a1.setSelectedIndex(2); // correct
        QuizSubmitRequest.QuizAnswer a2 = new QuizSubmitRequest.QuizAnswer();
        a2.setQuestionId("q2");
        a2.setSelectedIndex(1); // wrong (correct is 0)
        request.setAnswers(List.of(a1, a2));

        GameResultResponse response = gameService.submitQuiz("u1", request);

        assertThat(response.getCorrectAnswers()).isEqualTo(1);
        assertThat(response.getTotalQuestions()).isEqualTo(2);
        assertThat(response.getPointsEarned()).isEqualTo(10); // 1 correct * 10 points
        verify(loyaltyService).creditPoints(eq("u1"), eq(10), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("submitQuiz rejects an answer referencing an unknown question id")
    void submitQuiz_unknownQuestion_throwsNotFound() {
        when(quizQuestionRepository.findById("ghost")).thenReturn(Optional.empty());

        QuizSubmitRequest request = new QuizSubmitRequest();
        QuizSubmitRequest.QuizAnswer a1 = new QuizSubmitRequest.QuizAnswer();
        a1.setQuestionId("ghost");
        a1.setSelectedIndex(0);
        request.setAnswers(List.of(a1));

        assertThatThrownBy(() -> gameService.submitQuiz("u1", request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("quiz daily limit blocks a second attempt the same day")
    void quiz_dailyLimitEnforced() {
        doThrow(new BadRequestException("Daily limit reached"))
            .when(loyaltyService).assertDailyLimitNotExceeded(eq("u1"), eq(GameResult.GameType.QUIZ), anyInt());

        assertThatThrownBy(() -> gameService.getTodaysQuiz("u1")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submitMemory rejects a session that does not belong to the requesting user")
    void submitMemory_wrongUser_rejected() {
        MemorySession session = new MemorySession();
        session.setId("s1");
        session.setUserId("someone-else");
        session.setPairCount(8);
        session.setCompleted(false);
        when(memorySessionRepository.findById("s1")).thenReturn(Optional.of(session));

        MemorySubmitRequest request = new MemorySubmitRequest();
        request.setSessionId("s1");
        request.setMoves(10);
        request.setDurationMs(5000);

        assertThatThrownBy(() -> gameService.submitMemory("u1", request))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submitMemory rejects a move count below the mathematical minimum (impossible claim)")
    void submitMemory_impossibleMoveCount_rejected() {
        MemorySession session = new MemorySession();
        session.setId("s1");
        session.setUserId("u1");
        session.setPairCount(8);
        session.setCompleted(false);
        when(memorySessionRepository.findById("s1")).thenReturn(Optional.of(session));

        MemorySubmitRequest request = new MemorySubmitRequest();
        request.setSessionId("s1");
        request.setMoves(3); // fewer than 8 pairs is impossible
        request.setDurationMs(5000);

        assertThatThrownBy(() -> gameService.submitMemory("u1", request))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submitMemory rejects submitting the same session twice")
    void submitMemory_alreadyCompleted_rejected() {
        MemorySession session = new MemorySession();
        session.setId("s1");
        session.setUserId("u1");
        session.setPairCount(8);
        session.setCompleted(true);
        when(memorySessionRepository.findById("s1")).thenReturn(Optional.of(session));

        MemorySubmitRequest request = new MemorySubmitRequest();
        request.setSessionId("s1");
        request.setMoves(10);
        request.setDurationMs(5000);

        assertThatThrownBy(() -> gameService.submitMemory("u1", request))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submitMemory awards a perfect-game bonus for the minimum possible moves")
    void submitMemory_perfectGame_awardsMaxPoints() {
        MemorySession session = new MemorySession();
        session.setId("s1");
        session.setUserId("u1");
        session.setPairCount(8);
        session.setCompleted(false);
        when(memorySessionRepository.findById("s1")).thenReturn(Optional.of(session));

        MemorySubmitRequest request = new MemorySubmitRequest();
        request.setSessionId("s1");
        request.setMoves(8); // perfect: one match per move
        request.setDurationMs(5000);

        GameResultResponse response = gameService.submitMemory("u1", request);

        assertThat(response.getPointsEarned()).isEqualTo(40);
    }

    @Test
    @DisplayName("scratch card daily limit blocks a second play the same day")
    void scratchCard_dailyLimitEnforced() {
        doThrow(new BadRequestException("Daily limit reached"))
            .when(loyaltyService).assertDailyLimitNotExceeded(eq("u1"), eq(GameResult.GameType.SCRATCH_CARD), anyInt());

        assertThatThrownBy(() -> gameService.playScratchCard("u1")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("wheel spin always returns a prize within the configured segment range")
    void wheelSpin_prizeWithinConfiguredRange() {
        for (int i = 0; i < 50; i++) {
            GameResultResponse response = gameService.spinWheel("u1");
            assertThat(response.getPointsEarned()).isBetween(0, 150);
        }
    }
}
