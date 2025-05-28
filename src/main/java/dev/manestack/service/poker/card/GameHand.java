package dev.manestack.service.poker.card;

import java.util.List;

public class GameHand implements Comparable<GameHand> {
    private final GameHandRank rank;
    private final List<Integer> tiebreakers;

    public GameHand(GameHandRank rank, List<Integer> tiebreakers) {
        this.rank = rank;
        this.tiebreakers = tiebreakers;
    }

    public GameHandRank getRank() {
        return rank;
    }

    @Override
    public int compareTo(GameHand other) {
        int cmp = Integer.compare(this.rank.ordinal(), other.getRank().ordinal());
        if (cmp != 0) return cmp;
        for (int i = 0; i < Math.min(tiebreakers.size(), other.tiebreakers.size()); i++) {
            cmp = Integer.compare(this.tiebreakers.get(i), other.tiebreakers.get(i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "GameHand{" +
                "rank=" + rank +
                ", tiebreakers=" + tiebreakers +
                '}';
    }
}
