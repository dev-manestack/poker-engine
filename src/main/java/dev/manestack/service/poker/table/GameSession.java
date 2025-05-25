package dev.manestack.service.poker.table;

import dev.manestack.service.poker.card.GameCard;
import dev.manestack.service.poker.card.GameDeck;
import org.jboss.logging.Logger;

import java.util.*;

public class GameSession {
    private static final Logger LOG = Logger.getLogger(GameSession.class);
    private final long sessionId;
    private final GameTable table;
    private final Queue<GamePlayer> playerQueue = new LinkedList<>();
    private final GameDeck deck;

    private State state;
    private GamePlayer currentPlayer;
    private int pot;
    private final List<GameCard> communityCards = new ArrayList<>();
    private final Queue<GamePlayer> currentQueue = new LinkedList<>();

    public GameSession(long sessionId, GameTable table, int dealerPosition, Map<Integer, GamePlayer> players) {
        this.sessionId = sessionId;
        this.table = table;
        this.state = State.WAITING_FOR_PLAYERS;
        this.deck = new GameDeck();
        List<Integer> orderedSeats = players.keySet().stream().sorted().toList();
        int startIndex = (orderedSeats.indexOf(dealerPosition) + 1) % orderedSeats.size();
        for (int i = 0; i < orderedSeats.size(); i++) {
            int seatIndex = (startIndex + i) % orderedSeats.size();
            int seat = orderedSeats.get(seatIndex);
            GamePlayer player = players.get(seat);
            if (player != null) {
                playerQueue.add(player);
            }
        }
    }


    public void startGame() {
        if (playerQueue.size() < 2) throw new IllegalStateException("Not enough players");
        LOG.infov("Starting game session {0} with players: {1}", sessionId, playerQueue);
        state = State.PRE_FLOP;
        dealCards();
        table.sendGameStateUpdateToParticipants(state, new ArrayList<>());
        rotateToNextPlayerQueue();
        promptNextPlayer();
    }

    private void rotateToNextPlayerQueue() {
        currentQueue.clear();
        for (int i = 0; i < playerQueue.size(); i++) {
            currentQueue.add(playerQueue.poll());
        }
    }

    private void promptNextPlayer() {
        if (currentQueue.isEmpty()) {
            advanceGameState();
            return;
        }
        currentPlayer = currentQueue.poll();
        table.sendTurnUpdateToParticipants(currentPlayer);
        LOG.infov("Turn has been passed to player {0} at table {1} in session {2}",
                currentPlayer.getUser().getUserId(), table.getTableName(), sessionId);
    }

    public void receivePlayerAction(Integer playerId, ActionType actionType, int amount) {
        if (currentPlayer.getUser().getUserId() != playerId) throw new IllegalStateException("Not this player's turn");
        LOG.infov("Player {0} at table {1} in session {2} performed action: {3} with amount: {4}",
                playerId, table.getTableName(), sessionId, actionType, amount);
        switch (actionType) {
            case FOLD -> currentPlayer.setInHand(false);
            case CALL, RAISE -> {
                currentPlayer.deductFromStack(amount);
                pot += amount;
            }
            case CHECK -> {
            } // no-op
        }

        promptNextPlayer();
    }

    private void dealCards() {
        LOG.infov("Dealing cards to players in session {0}", sessionId);
        for (GamePlayer player : playerQueue) {
            player.addCard(deck.drawCard());
            player.addCard(deck.drawCard());
        }
    }

    private void advanceGameState() {
        LOG.infov("Advancing game state from {0} to next state", state);
        switch (state) {
            case PRE_FLOP -> {
                state = State.FLOP;
                communityCards.add(deck.drawCard());
                communityCards.add(deck.drawCard());
                communityCards.add(deck.drawCard());
            }
            case FLOP -> {
                state = State.TURN;
                communityCards.add(deck.drawCard());
            }
            case TURN -> {
                state = State.RIVER;
                communityCards.add(deck.drawCard());
            }
            case RIVER -> state = State.SHOWDOWN;
            case SHOWDOWN -> state = State.FINISHED;
            case FINISHED -> throw new IllegalStateException("Game is already over");
            default -> throw new IllegalStateException("Invalid state");
        }
        table.sendGameStateUpdateToParticipants(state, new ArrayList<>());
        if (state != State.FINISHED) {
            rotateToNextPlayerQueue();
            promptNextPlayer();
        }
    }

    /*
     Getters & Setters
     */

    public long getSessionId() {
        return sessionId;
    }

    public GameTable getTable() {
        return table;
    }

    public Queue<GamePlayer> getPlayerQueue() {
        return playerQueue;
    }

    public State getState() {
        return state;
    }

    public GamePlayer getCurrentPlayer() {
        return currentPlayer;
    }

    public int getPot() {
        return pot;
    }

    public Queue<GamePlayer> getCurrentQueue() {
        return currentQueue;
    }

    public enum State {
        WAITING_FOR_PLAYERS,
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER,
        SHOWDOWN,
        FINISHED
    }

    public enum ActionType {
        FOLD,
        CALL,
        RAISE,
        CHECK
    }
}
