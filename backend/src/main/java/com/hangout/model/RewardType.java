package com.hangout.model;

/** What a client can exchange restaurant points for. The catalogue lives on the server. */
public enum RewardType {
    DISCOUNT_10("10% discount", "10% off your bill at this restaurant", 100),
    FREE_COFFEE("Free coffee", "A free coffee with your meal at this restaurant", 60),
    FREE_EVENT_ENTRY("Free event entry", "Free entry to one event of this restaurant", 150);

    private final String title;
    private final String description;
    private final int cost;

    RewardType(String title, String description, int cost) {
        this.title = title;
        this.description = description;
        this.cost = cost;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getCost() { return cost; }
}
