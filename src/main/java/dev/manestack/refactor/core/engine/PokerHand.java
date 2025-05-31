package dev.manestack.refactor.core.engine;


import dev.manestack.refactor.core.model.Card;

import java.util.List;

public class PokerHand implements Comparable<PokerHand> {
    private final Rank rank;
    private final List<Integer> tiebreakers;
    private final List<Card> combinationCards;

    public PokerHand(Rank rank, List<Integer> tiebreakers, List<Card> combinationCards) {
        this.rank = rank;
        this.tiebreakers = tiebreakers;
        this.combinationCards = combinationCards;
    }

    public Rank getRank() {
        return rank;
    }

    public void setBestCombination(List<Card> cards) {
        this.combinationCards.clear();
        this.combinationCards.addAll(cards);
    }

    public List<Card> getCombinationCards() {
        return combinationCards;
    }

    @Override
    public int compareTo(PokerHand other) {
        int cmp = Integer.compare(this.rank.ordinal(), other.getRank().ordinal());
        if (cmp != 0) return cmp;
        for (int i = 0; i < Math.min(tiebreakers.size(), other.tiebreakers.size()); i++) {
            cmp = Integer.compare(this.tiebreakers.get(i), other.tiebreakers.get(i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    public List<Integer> getTiebreakers() {
        return tiebreakers;
    }

    public enum Rank {
        HIGH_CARD,
        ONE_PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_FLUSH
    }
}
