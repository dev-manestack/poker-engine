package dev.manestack.service.poker.table;

import dev.manestack.service.poker.card.GameCard;
import dev.manestack.service.poker.card.GameDeck;
import dev.manestack.service.poker.card.GameHand;
import dev.manestack.service.poker.card.GameHandEvaluator;
import org.jboss.logging.Logger;

import java.util.*;

public class GameSession {
    private static final Logger LOG = Logger.getLogger(GameSession.class);
    private final long sessionId;
    private final GameTable table;
    private final Queue<GamePlayer> originalPlayerQueue = new LinkedList<>();
    private final GameDeck deck;

    private State state;
    private GamePlayer currentPlayer;
    private int pot;
    private final List<GameCard> communityCards = new ArrayList<>();
    private final Queue<GamePlayer> currentQueue = new LinkedList<>();
    private final Map<Integer, Integer> playerBets = new HashMap<>();

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
                originalPlayerQueue.add(player);
            }
        }
    }


    public void startGame() {
        if (originalPlayerQueue.size() < 2) throw new IllegalStateException("Not enough players");
        LOG.infov("Starting game session {0} with players: {1}", sessionId, originalPlayerQueue);
        state = State.PRE_FLOP;
        dealCards();
        table.sendGameStateUpdateToParticipants(state, communityCards);
        rotateToNextPlayerQueue();
        promptNextPlayer();
    }

    private void rotateToNextPlayerQueue() {
        currentQueue.clear();
        playerBets.clear();
        currentQueue.addAll(originalPlayerQueue.stream().filter(GamePlayer::isInHand).toList());
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
            case CALL -> {
                if (amount <= 0) {
                    LOG.warnv("Player {0} attempted to call with non-positive amount in session {1}", playerId, sessionId);
                    return; // no-op if call is non-positive
                }
                if (playerBets.isEmpty()) {
                    LOG.warnv("Player {0} called without any bets in session {1}", playerId, sessionId);
                    return; // no-op if no bets to call
                } else {
                    int highestBet = playerBets.values().stream().max(Integer::compareTo).orElse(0);
                    if (amount < highestBet) {
                        LOG.warnv("Player {0} attempted to call with insufficient amount in session {1}", playerId, sessionId);
                        return; // no-op if call is less than highest bet
                    }
                    currentPlayer.deductFromStack(amount);
                    currentPlayer.addToTotalContribution(amount);
                    pot += amount;
                    playerBets.put(playerId, amount);
                }
            }
            case RAISE -> {
                if (amount <= 0) {
                    LOG.warnv("Player {0} attempted to raise with non-positive amount in session {1}", playerId, sessionId);
                    return; // no-op if raise is non-positive
                }
                currentPlayer.deductFromStack(amount);
                currentPlayer.addToTotalContribution(amount);
                pot += amount;
                playerBets.put(playerId, amount);
                LOG.infov("Player {0} raised by {1} chips in session {2}", playerId, amount, sessionId);
                currentQueue.clear();
                for (GamePlayer player : originalPlayerQueue) {
                    if (player.isInHand() && player != currentPlayer) {
                        currentQueue.add(player);
                    }
                }
            }
            case CHECK -> {
                if (!playerBets.isEmpty()) {
                    LOG.warnv("Player {0} attempted to check when there are bets in session {1}", playerId, sessionId);
                    return;
                }
                LOG.infov("Player {0} checked in session {1}", playerId, sessionId);
            } // no-op
        }
        table.propagatePlayerEvent(playerId, actionType, amount, playerBets);
        int remainingPlayers = (int) originalPlayerQueue.stream().filter(GamePlayer::isInHand).count();
        if (remainingPlayers <= 1) {
            LOG.infov("Only one player remaining in hand. Finishing game state early for session {0}", sessionId);
            state = State.SHOWDOWN;
            advanceGameState();
        } else {
            promptNextPlayer();
        }
    }

    private void dealCards() {
        LOG.infov("Dealing cards to players in session {0}", sessionId);
        for (GamePlayer player : originalPlayerQueue) {
            player.addCard(deck.drawCard());
            player.addCard(deck.drawCard());
            player.setInHand(true);
        }
        table.sendPersonalHoleCardsToPlayers();
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
            case SHOWDOWN -> {
                state = State.FINISHED;
                calculateWinningsAndUpdateBalance();
            }
            case FINISHED -> throw new IllegalStateException("Game is already over");
            default -> throw new IllegalStateException("Invalid state");
        }
        table.sendGameStateUpdateToParticipants(state, communityCards);
        if (state != State.FINISHED) {
            rotateToNextPlayerQueue();
            promptNextPlayer();
        } else {
            table.startNextGame();
        }
    }

    public void calculateWinningsAndUpdateBalance() {
        LOG.infov("Calculating winnings for players in session {0}", sessionId);
        GameHand best = null;
        List<GamePlayer> winners = new ArrayList<>();

        for (GamePlayer player : originalPlayerQueue) {
            if (player.isInHand()) {
                List<GameCard> fullHand = new ArrayList<>(player.getHoleCards());
                fullHand.addAll(communityCards);
                LOG.infov("Full hand for player {0}: {1}", player.getUser().getUserId(), fullHand);
                GameHand current = GameHandEvaluator.evaluate(fullHand);
                LOG.infov("Best hand for player {0} is {1} consisting of {2}", player.getUser().getUserId(), current.getRank(), current.getCombinationCards());
                if (best == null || current.compareTo(best) > 0) {
                    best = current;
                    winners.clear();
                    winners.add(player);
                } else if (current.compareTo(best) == 0) {
                    winners.add(player);
                }
            }
        }
        for (GamePlayer winner : winners) {
            int winnings = pot / winners.size();
            winner.addToStack(winnings);
            LOG.infov("Player {0} wins {1} chips", winner.getUser().getUserId(), winnings);
        }
        table.propagatePlayerStacks();
        LOG.infov("Best hand in session {0} is {1} with winners: {2}", sessionId, best, winners);
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

    public Queue<GamePlayer> getOriginalPlayerQueue() {
        return originalPlayerQueue;
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
