package com.hangout.chatbot;

import com.hangout.dto.chat.ChatTurn;
import com.hangout.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * First protection layer, applied before any call to the model (so blocked messages cost no API call):
 * size limits, per-user rate limit and detection of prompt-injection attempts.
 */
@Component
@RequiredArgsConstructor
public class InputGuard {
    
    public enum Verdict {
        ALLOWED,
        EMPTY,
        TOO_LONG,
        INJECTION
    }
    
    // Written on accent-free lowercase text (see normalize)
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        Pattern.compile("\\b(ignore|ignorez|oublie|oubliez|neglige|outrepasse)\\b.{0,40}\\b(instruction|regle|consigne|directive|prompt|limite)s?\\b"),
        Pattern.compile("\\b(ignore|forget|disregard|override)\\b.{0,40}\\b(instruction|rule|prompt|guideline)s?\\b"),
        Pattern.compile("\\b(system|systeme)\\s*prompt\\b|\\bprompt\\s*(systeme|system)\\b"),
        Pattern.compile("\\b(tu es|vous etes|you are) (maintenant|desormais|now)\\b"),
        Pattern.compile("\\b(agis|agissez|act|joue le role)\\b.{0,20}\\b(comme|as|que|to be|d'un|de)\\b"),
        Pattern.compile("\\b(fais comme si|faites comme si|pretend|imagine que tu)\\b|\\bsans (aucune )?(limites?|restrictions?|filtres?)\\b"),
        Pattern.compile("\\b(nouvelles?|new) (instructions?|consignes?|rules?)\\b"),
        Pattern.compile("\\b(jailbreak|developer mode|mode developpeur|dan mode)\\b"),
        Pattern.compile("\\b(reponds?|repondez|answer) a (toutes?|tout|n'importe)\\b"),
        Pattern.compile("\\b(revele|affiche|montre|repete|reveal|print|show)\\b.{0,30}\\b(instructions?|prompt|consignes?)\\b"),
        Pattern.compile("</?\\s*(system|assistant|instructions?)\\s*>|^\\s*(system|assistant)\\s*:")
    );
    
    private final ChatbotProperties properties;
    private final Clock clock;
    private final Map<String, Deque<Instant>> requestsByUser = new ConcurrentHashMap<>();
    
    /**
     * Checks the new message. The history is not judged here: see {@link #cleanHistory}.
     */
    public Verdict inspect(String message) {
        if (message == null || message.isBlank()) {
            return Verdict.EMPTY;
        }
        if (message.length() > properties.getMaxMessageLength()) {
            return Verdict.TOO_LONG;
        }
        if (looksLikeInjection(message)) {
            return Verdict.INJECTION;
        }
        return Verdict.ALLOWED;
    }
    
    /**
     * The history is sent back by the browser, so the user could have edited it. Turns that look like an
     * injection (an old refused message, or a forged "assistant" turn) are dropped and never reach the model.
     * Dropping instead of refusing the whole request keeps the conversation usable after one refused message.
     */
    public List<ChatTurn> cleanHistory(List<ChatTurn> history) {
        return history.stream()
            .filter(turn -> !looksLikeInjection(turn.content()))
            .toList();
    }
    
    public boolean looksLikeInjection(String text) {
        String normalized = normalize(text);
        return INJECTION_PATTERNS.stream().anyMatch(p -> p.matcher(normalized).find());
    }
    
    /** Sliding one-minute window per user; protects the Groq quota from abuse. */
    public void checkRateLimit(String userId) {
        Instant now = clock.instant();
        Deque<Instant> timestamps = requestsByUser.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && Duration.between(timestamps.peekFirst(), now).toSeconds() >= 60) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= properties.getRateLimitPerMinute()) {
                throw new TooManyRequestsException("Trop de messages envoyés. Veuillez patienter une minute.");
            }
            timestamps.addLast(now);
        }
    }
    
    /** Removes control characters (except new lines) before the text is sent to the model. */
    public String sanitize(String text) {
        return text == null ? "" : text.replaceAll("[\\p{Cntrl}&&[^\\n]]", " ").trim();
    }
    
    static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replace('’', '\'').replaceAll("\\s+", " ");
    }
}
