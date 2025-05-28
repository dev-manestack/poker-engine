package dev.manestack.service.poker.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameHandEvaluator {
    public static GameHand evaluate(List<GameCard> cards) {
        List<List<GameCard>> combinations = generate5CardCombinations(cards);
        GameHand best = null;
        for (List<GameCard> combo : combinations) {
            GameHand current = evaluate5CardHand(combo);
            if (best == null || current.compareTo(best) > 0) {
                best = current;
            }
        }
        return best;
    }

    private static GameHand evaluate5CardHand(List<GameCard> cards) {
        List<GameCard> sorted = cards.stream()
                .sorted(Comparator.comparingInt(c -> c.getRank().getValue()))
                .collect(Collectors.toList());

        Map<GameCard.Rank, Long> rankCount = sorted.stream()
                .collect(Collectors.groupingBy(GameCard::getRank, Collectors.counting()));

        Map<GameCard.Suit, Long> suitCount = sorted.stream()
                .collect(Collectors.groupingBy(GameCard::getSuit, Collectors.counting()));

        boolean flush = suitCount.containsValue(5L);
        boolean straight = isStraight(sorted);
        List<Integer> tiebreakers = extractTiebreakers(rankCount);

        if (flush && straight) {
            boolean isRoyal = sorted.get(0).getRank() == GameCard.Rank.TEN;
            return new GameHand(isRoyal ? GameHandRank.ROYAL_FLUSH : GameHandRank.STRAIGHT_FLUSH, tiebreakers);
        }

        if (rankCount.containsValue(4L))
            return new GameHand(GameHandRank.FOUR_OF_A_KIND, tiebreakers);
        if (rankCount.containsValue(3L) && rankCount.containsValue(2L))
            return new GameHand(GameHandRank.FULL_HOUSE, tiebreakers);
        if (flush)
            return new GameHand(GameHandRank.FLUSH, tiebreakers);
        if (straight)
            return new GameHand(GameHandRank.STRAIGHT, tiebreakers);
        if (rankCount.containsValue(3L))
            return new GameHand(GameHandRank.THREE_OF_A_KIND, tiebreakers);

        long pairCount = rankCount.values().stream().filter(v -> v == 2L).count();
        if (pairCount == 2)
            return new GameHand(GameHandRank.TWO_PAIR, tiebreakers);
        if (pairCount == 1)
            return new GameHand(GameHandRank.ONE_PAIR, tiebreakers);

        return new GameHand(GameHandRank.HIGH_CARD, tiebreakers);
    }

    private static boolean isStraight(List<GameCard> sorted) {
        List<Integer> values = sorted.stream()
                .map(c -> c.getRank().getValue())
                .distinct()
                .toList();

        if (values.size() != 5) return false;
        int first = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) != first + i) return false;
        }

        // Special case: Ace-low straight (A-2-3-4-5)
        return true;
    }

    private static List<Integer> extractTiebreakers(Map<GameCard.Rank, Long> rankCount) {
        return rankCount.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : Integer.compare(b.getKey().getValue(), a.getKey().getValue());
                })
                .map(e -> e.getKey().getValue())
                .collect(Collectors.toList());
    }

    private static List<List<GameCard>> generate5CardCombinations(List<GameCard> cards) {
        List<List<GameCard>> combinations = new ArrayList<>();
        int n = cards.size();
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                for (int k = j + 1; k < n; k++)
                    for (int l = k + 1; l < n; l++)
                        for (int m = l + 1; m < n; m++)
                            combinations.add(List.of(cards.get(i), cards.get(j), cards.get(k), cards.get(l), cards.get(m)));
        return combinations;
    }
}
