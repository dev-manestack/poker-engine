package dev.manestack.refactor.core.engine;

import dev.manestack.refactor.core.model.Card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PokerEvaluator {
    public static PokerHand evaluate(List<Card> cards) {
        List<List<Card>> combinations = generate5CardCombinations(cards);
        PokerHand best = null;
        for (List<Card> combo : combinations) {
            PokerHand current = evaluate5CardHand(combo);
            if (best == null || current.compareTo(best) > 0) {
                best = current;
            }
        }
        return best;
    }

    private static PokerHand evaluate5CardHand(List<Card> cards) {
        List<Card> sorted = cards.stream()
                .sorted(Comparator.comparingInt((Card c) -> c.getRank().getValue()).reversed())
                .collect(Collectors.toList());

        Map<Card.Rank, List<Card>> rankGroups = sorted.stream()
                .collect(Collectors.groupingBy(Card::getRank));

        Map<Card.Suit, List<Card>> suitGroups = sorted.stream()
                .collect(Collectors.groupingBy(Card::getSuit));

        // Check for flush
        List<Card> flushCards = suitGroups.values().stream()
                .filter(g -> g.size() >= 5)
                .findFirst()
                .orElse(null);

        // Check for straight (or straight flush)
        List<Card> straight = getStraight(sorted);
        List<Card> straightFlush = flushCards == null ? null : getStraight(flushCards);

        if (straightFlush != null) {
            boolean isRoyal = straightFlush.get(0).getRank() == Card.Rank.ACE &&
                    straightFlush.get(4).getRank() == Card.Rank.TEN;
            return new PokerHand(isRoyal ? PokerHand.Rank.ROYAL_FLUSH : PokerHand.Rank.STRAIGHT_FLUSH,
                    extractTiebreakers(straightFlush), straightFlush);
        }

        for (List<Card> group : rankGroups.values()) {
            if (group.size() == 4) {
                List<Card> best = new ArrayList<>(group.subList(0, 4));
                best.add(getHighestExcluding(sorted, best));
                return new PokerHand(PokerHand.Rank.FOUR_OF_A_KIND, extractTiebreakers(best), best);
            }
        }

        Card.Rank three = null;
        Card.Rank two = null;

        for (Card.Rank r : Card.Rank.values()) {
            if (rankGroups.containsKey(r)) {
                int count = rankGroups.get(r).size();
                if (count == 3 && three == null) three = r;
                else if (count >= 2 && two == null && (three == null || r != three)) two = r;
            }
        }

        if (three != null && two != null) {
            List<Card> best = new ArrayList<>(rankGroups.get(three).subList(0, 3));
            best.addAll(rankGroups.get(two).subList(0, 2));
            return new PokerHand(PokerHand.Rank.FULL_HOUSE, extractTiebreakers(best), best);
        }

        if (flushCards != null) {
            List<Card> best = flushCards.subList(0, 5);
            return new PokerHand(PokerHand.Rank.FLUSH, extractTiebreakers(best), best);
        }

        if (straight != null) {
            return new PokerHand(PokerHand.Rank.STRAIGHT, extractTiebreakers(straight), straight);
        }

        for (List<Card> group : rankGroups.values()) {
            if (group.size() == 3) {
                List<Card> best = new ArrayList<>(group.subList(0, 3));
                best.addAll(getHighestKExcluding(sorted, best, 2));
                return new PokerHand(PokerHand.Rank.THREE_OF_A_KIND, extractTiebreakers(best), best);
            }
        }

        List<List<Card>> pairs = rankGroups.values().stream()
                .filter(g -> g.size() == 2)
                .sorted((a, b) -> b.getFirst().getRank().getValue() - a.getFirst().getRank().getValue())
                .toList();

        if (pairs.size() >= 2) {
            List<Card> best = new ArrayList<>();
            best.addAll(pairs.get(0));
            best.addAll(pairs.get(1));
            best.add(getHighestExcluding(sorted, best));
            return new PokerHand(PokerHand.Rank.TWO_PAIR, extractTiebreakers(best), best);
        }

        if (pairs.size() == 1) {
            List<Card> best = new ArrayList<>(pairs.get(0));
            best.addAll(getHighestKExcluding(sorted, best, 3));
            return new PokerHand(PokerHand.Rank.ONE_PAIR, extractTiebreakers(best), best);
        }

        List<Card> best = sorted.subList(0, 5);
        return new PokerHand(PokerHand.Rank.HIGH_CARD, extractTiebreakers(best), best);
    }

    private static List<Card> getStraight(List<Card> cards) {
        List<Card> distinct = cards.stream()
                .collect(Collectors.toMap(Card::getRank, c -> c, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparingInt((Card c) -> c.getRank().getValue()).reversed())
                .toList();

        List<Card> result = new ArrayList<>();
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
        boolean hasAce = distinct.stream().anyMatch(c -> c.getRank() == Card.Rank.ACE);
        boolean has2to5 = Stream.of(Card.Rank.TWO, Card.Rank.THREE, Card.Rank.FOUR, Card.Rank.FIVE)
                .allMatch(r -> distinct.stream().anyMatch(c -> c.getRank() == r));

        if (hasAce && has2to5) {
            List<Card> lowStraight = new ArrayList<>();
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == Card.Rank.FIVE).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == Card.Rank.FOUR).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == Card.Rank.THREE).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == Card.Rank.TWO).limit(1).toList());
            lowStraight.addAll(distinct.stream().filter(c -> c.getRank() == Card.Rank.ACE).limit(1).toList());
            return lowStraight;
        }

        return null;
    }

    private static Card getHighestExcluding(List<Card> cards, List<Card> exclude) {
        return cards.stream().filter(c -> !exclude.contains(c)).findFirst().orElse(null);
    }

    private static List<Card> getHighestKExcluding(List<Card> cards, List<Card> exclude, int k) {
        return cards.stream().filter(c -> !exclude.contains(c)).limit(k).collect(Collectors.toList());
    }

    private static List<Integer> extractTiebreakers(List<Card> cards) {
        return cards.stream().map(c -> c.getRank().getValue()).collect(Collectors.toList());
    }


    private static boolean isStraight(List<Card> sorted) {
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

    private static List<Integer> extractTiebreakers(Map<Card.Rank, Long> rankCount) {
        return rankCount.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : Integer.compare(b.getKey().getValue(), a.getKey().getValue());
                })
                .map(e -> e.getKey().getValue())
                .collect(Collectors.toList());
    }

    private static List<List<Card>> generate5CardCombinations(List<Card> cards) {
        List<List<Card>> combinations = new ArrayList<>();
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
