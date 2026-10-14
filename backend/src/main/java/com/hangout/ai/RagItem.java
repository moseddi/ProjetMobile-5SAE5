package com.hangout.ai;

import com.hangout.model.Event;

import java.util.List;

public record RagItem(Event event, String restaurantName, double avgRating,
                      int relevancePercent, String explanation, List<ReviewSnippet> sources) {}