package dev.manestack.service.poker.core;

import dev.manestack.service.poker.event.BaseEvent;

public interface GameSession {
    void startGame();
    void endGame();
    void addPlayer(GamePlayer player);
    void removePlayer(GamePlayer player);
    void shuffleDeck();
    void dealCards();
    void handleEvent(BaseEvent event);
    GameDeck getDeck();
    GamePlayer getCurrentPlayer();
    boolean isGameOver();
}
