package dev.manestack.refactor.core.model;

public class Card {
    private final Suit suit;
    private final Rank rank;
    private final boolean isSecret;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
        this.isSecret = false;
    }

    public Card(boolean isSecret) {
        this.suit = null;
        this.rank = null;
        this.isSecret = isSecret;
    }

    public boolean isSecret() {
        return isSecret;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    public enum Suit {
        HEARTS,
        DIAMONDS,
        CLUBS,
        SPADES
    }

    public enum Rank {
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        TEN(10),
        JACK(11),
        QUEEN(12),
        KING(13),
        ACE(14);

        private final int value;

        Rank(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
