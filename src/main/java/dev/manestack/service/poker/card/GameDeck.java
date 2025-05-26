package dev.manestack.service.poker.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameDeck {
    private final List<GameCard> cards = new ArrayList<>();

    public GameDeck() {
        for (GameCard.Suit suit : GameCard.Suit.values()) {
            for (GameCard.Rank rank : GameCard.Rank.values()) {
                cards.add(new GameCard(suit, rank));
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public GameCard drawCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards left in the deck");
        }
        return cards.removeLast();
    }

    public int remainingCards() {
        return cards.size();
    }
}
