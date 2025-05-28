package dev.manestack.service.poker.card;

import java.util.List;

public class GameHand implements Comparable<GameHand> {
    private final GameHandRank rank;
    private final List<Integer> tiebreakers;
    private final List<GameCard> combinationCards;

    public GameHand(GameHandRank rank, List<Integer> tiebreakers, List<GameCard> combinationCards) {
        this.rank = rank;
        this.tiebreakers = tiebreakers;
        this.combinationCards = combinationCards;
    }

    public GameHandRank getRank() {
        return rank;
    }

    public void setBestCombination(List<GameCard> cards) {
        this.combinationCards.clear();
        this.combinationCards.addAll(cards);
    }

    public List<GameCard> getCombinationCards() {
        return combinationCards;
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

    public List<Integer> getTiebreakers() {
        return tiebreakers;
    }
}
