package dev.manestack.service.poker.core;

public enum GameVariant {
    TEXAS_HOLDEM("Texas Hold'em"),
    OMAHA("Omaha"),
    SEVEN_CARD_STUD("Seven Card Stud"),
    FIVE_CARD_DRAW("Five Card Draw"),
    RAKEBACK("Rakeback");

    private final String displayName;

    GameVariant(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
