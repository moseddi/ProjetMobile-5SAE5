package com.hangout.ai;

public record ReviewSnippet(String label, double rating, String text, int similarityPercent) {}