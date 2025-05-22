package dev.manestack.service.poker.table;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class GameSession {
    private final long sessionId;
    private final GameTable table;
    private final Queue<GamePlayer> playerQueue = new LinkedList<>();
    private final int dealerPosition;

    private State state;
    private GamePlayer currentPlayer;
    private int pot;
    private Queue<GamePlayer> currentQueue;

    public GameSession(long sessionId, GameTable table, int dealerPosition, Map<Integer, GamePlayer> players) {
        this.sessionId = sessionId;
        this.table = table;
        this.dealerPosition = dealerPosition;
        this.state = State.WAITING_FOR_PLAYERS;
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
        state = State.PRE_FLOP;
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
        // Wait externally for `receivePlayerAction(...)` to be called
    }

    public void receivePlayerAction(Integer playerId, ActionType actionType, int amount) {
        if (currentPlayer.getUser().getUserId() != playerId) throw new IllegalStateException("Not this player's turn");

        switch (actionType) {
            case FOLD -> currentPlayer.inHand = false;
            case CALL -> {
                currentPlayer.stack -= action.amount;
                pot += action.amount;
            }
            case RAISE -> {
                currentPlayer.stack -= action.amount;
                pot += action.amount;
            }
            case CHECK -> {} // no-op
        }

        promptNextPlayer();
    }

    private void advanceGameState() {
        switch (state) {
            case PRE_FLOP -> state = State.FLOP;
            case FLOP -> state = State.TURN;
            case TURN -> state = State.RIVER;
            case RIVER -> state = State.SHOWDOWN;
            case SHOWDOWN -> state = State.FINISHED;
            case FINISHED -> throw new IllegalStateException("Game is already over");
            default -> throw new IllegalStateException("Invalid state");
        }

        if (state != State.FINISHED) {
            rotateToNextPlayerQueue();
            promptNextPlayer();
        }
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
