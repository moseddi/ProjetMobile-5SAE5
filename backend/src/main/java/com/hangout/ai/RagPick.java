package com.hangout.ai;

import com.hangout.model.Event;

import java.util.List;

public record RagPick(Event event, String restaurantName, double avgRating,
                      double score, List<ReviewSnippet> snippets) {}