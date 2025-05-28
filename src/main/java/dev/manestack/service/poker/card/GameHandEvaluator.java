package dev.manestack.service.poker.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .sorted(Comparator.comparingInt((GameCard c) -> c.getRank().getValue()).reversed())
                .collect(Collectors.toList());

        Map<GameCard.Rank, List<GameCard>> rankGroups = sorted.stream()
                .collect(Collectors.groupingBy(GameCard::getRank));

        Map<GameCard.Suit, List<GameCard>> suitGroups = sorted.stream()
                .collect(Collectors.groupingBy(GameCard::getSuit));

        // Check for flush
        List<GameCard> flushCards = suitGroups.values().stream()
                .filter(g -> g.size() >= 5)
                .findFirst()
                .orElse(null);

        // Check for straight (or straight flush)
        List<GameCard> straight = getStraight(sorted);
        List<GameCard> straightFlush = flushCards == null ? null : getStraight(flushCards);

        if (straightFlush != null) {
            boolean isRoyal = straightFlush.get(0).getRank() == GameCard.Rank.ACE &&
                    straightFlush.get(4).getRank() == GameCard.Rank.TEN;
            return new GameHand(isRoyal ? GameHandRank.ROYAL_FLUSH : GameHandRank.STRAIGHT_FLUSH,
                    extractTiebreakers(straightFlush), straightFlush);
        }

        for (List<GameCard> group : rankGroups.values()) {
            if (group.size() == 4) {
                List<GameCard> best = new ArrayList<>(group.subList(0, 4));
                best.add(getHighestExcluding(sorted, best));
                return new GameHand(GameHandRank.FOUR_OF_A_KIND, extractTiebreakers(best), best);
            }
        }

        GameCard.Rank three = null;
        GameCard.Rank two = null;

        for (GameCard.Rank r : GameCard.Rank.values()) {
            if (rankGroups.containsKey(r)) {
                int count = rankGroups.get(r).size();
                if (count == 3 && three == null) three = r;
                else if (count >= 2 && two == null && (three == null || r != three)) two = r;
            }
        }

        if (three != null && two != null) {
            List<GameCard> best = new ArrayList<>(rankGroups.get(three).subList(0, 3));
            best.addAll(rankGroups.get(two).subList(0, 2));
            return new GameHand(GameHandRank.FULL_HOUSE, extractTiebreakers(best), best);
        }

        if (flushCards != null) {
            List<GameCard> best = flushCards.subList(0, 5);
            return new GameHand(GameHandRank.FLUSH, extractTiebreakers(best), best);
        }

        if (straight != null) {
            return new GameHand(GameHandRank.STRAIGHT, extractTiebreakers(straight), straight);
        }

        for (List<GameCard> group : rankGroups.values()) {
            if (group.size() == 3) {
                List<GameCard> best = new ArrayList<>(group.subList(0, 3));
                best.addAll(getHighestKExcluding(sorted, best, 2));
                return new GameHand(GameHandRank.THREE_OF_A_KIND, extractTiebreakers(best), best);
            }
        }

        List<List<GameCard>> pairs = rankGroups.values().stream()
                .filter(g -> g.size() == 2)
                .sorted((a, b) -> b.get(0).getRank().getValue() - a.get(0).getRank().getValue())
                .collect(Collectors.toList());

        if (pairs.size() >= 2) {
            List<GameCard> best = new ArrayList<>();
            best.addAll(pairs.get(0));
            best.addAll(pairs.get(1));
            best.add(getHighestExcluding(sorted, best));
            return new GameHand(GameHandRank.TWO_PAIR, extractTiebreakers(best), best);
        }

        if (pairs.size() == 1) {
            List<GameCard> best = new ArrayList<>(pairs.get(0));
            best.addAll(getHighestKExcluding(sorted, best, 3));
            return new GameHand(GameHandRank.ONE_PAIR, extractTiebreakers(best), best);
        }

        List<GameCard> best = sorted.subList(0, 5);
        return new GameHand(GameHandRank.HIGH_CARD, extractTiebreakers(best), best);
    }

    private static List<GameCard> getStraight(List<GameCard> cards) {
        List<GameCard> distinct = cards.stream()
                .collect(Collectors.toMap(GameCard::getRank, c -> c, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparingInt((GameCard c) -> c.getRank().getValue()).reversed())
                .collect(Collectors.toList());

        List<GameCard> result = new ArrayList<>();
        for (int i = 0; i < distinct.size(); i++) {
            result.clear();
            result.add(distinct.get(i));
            int lastVal = distinct.get(i).getRank().getValue();

            for (int j = i + 1; j < distinct.size() && result.size() < 5; j++) {
                int curr = distinct.get(j).getRank().getValue();
                if (curr == lastVal - 1) {
                    result.add(distinct.get(j));
                    lastVal = curr;
                } else if (curr < lastVal - 1) {
                    break;
                }
            }

            if (result.size() == 5) return result;
        }

        // Check Ace-low straight
        boolean hasAce = distinct.stream().anyMatch(c -> c.getRank() == GameCard.Rank.ACE);
        boolean has2to5 = Stream.of(GameCard.Rank.TWO, GameCard.Rank.THREE, GameCard.Rank.FOUR, GameCard.Rank.FIVE)
                .allMatch(r -> distinct.stream().anyMatch(c -> c.getRank() == r));

        if (hasAce && has2to5) {
            List<GameCard> lowStraight = new ArrayList<>();
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == GameCard.Rank.FIVE).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == GameCard.Rank.FOUR).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == GameCard.Rank.THREE).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == GameCard.Rank.TWO).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == GameCard.Rank.ACE).limit(1).toList());
            return lowStraight;
        }

        return null;
    }

    private static GameCard getHighestExcluding(List<GameCard> cards, List<GameCard> exclude) {
        return cards.stream().filter(c -> !exclude.contains(c)).findFirst().orElse(null);
    }

    private static List<GameCard> getHighestKExcluding(List<GameCard> cards, List<GameCard> exclude, int k) {
        return cards.stream().filter(c -> !exclude.contains(c)).limit(k).collect(Collectors.toList());
    }

    private static List<Integer> extractTiebreakers(List<GameCard> cards) {
        return cards.stream().map(c -> c.getRank().getValue()).collect(Collectors.toList());
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
