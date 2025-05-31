package dev.manestack.refactor.core.engine;

import dev.manestack.refactor.core.model.Action;
import dev.manestack.refactor.core.model.Card;
import dev.manestack.refactor.core.model.Deck;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class PokerSession {
    private static final Logger LOG = Logger.getLogger(PokerSession.class);
    private final String sessionId;
    private final int smallBlindAmount;
    private final int bigBlindAmount;
    private final int minimumRaiseAmount;
    private final int rakePercentage;
    private final Deck deck;
    private final Map<String, PokerPlayer> players = new HashMap<>();

    private Stage stage;
    private int currentPot;
    private PokerPlayer currentPlayer;
    private final List<Card> communityCards = new ArrayList<>();
    private final Queue<PokerPlayer> actionQueue = new LinkedList<>();

    public PokerSession(String sessionId, int smallBlindAmount, int bigBlindAmount, int minimumRaiseAmount, int rakePercentage, Map<String, PokerPlayer> players) {
        this.sessionId = sessionId;
        this.smallBlindAmount = smallBlindAmount;
        this.bigBlindAmount = bigBlindAmount;
        this.minimumRaiseAmount = minimumRaiseAmount;
        this.rakePercentage = rakePercentage;
        this.deck = new Deck();
        this.stage = Stage.WAITING_FOR_PLAYERS;
        this.currentPot = 0;
        this.players.putAll(players);
    }

    public void startGame() {
        if (players.size() < 2) {
            throw new IllegalStateException("Not enough players to start the game");
        }
        this.stage = Stage.PRE_FLOP;
        this.deck.shuffle();
        dealHoleCards();
        rotateToNextActionQueue();
        betBlinds();
    }

    public void endGame() {
        // Handle end of game logic, such as determining the winner, distributing the pot, etc.
        LOG.infov("Calculating winnings for players in session {0}", sessionId);
        PokerHand best = null;
        List<PokerPlayer> winners = new ArrayList<>();

        for (PokerPlayer player : players.values()) {
            if (!player.isFolded()) {
                List<Card> fullHand = new ArrayList<>(player.getHoleCards());
                fullHand.addAll(communityCards);
                LOG.infov("Full hand for player {0}: {1}", player.getId(), fullHand);
                PokerHand current = PokerEvaluator.evaluate(fullHand);
                LOG.infov("Best hand for player {0} is {1} consisting of {2}", player.getId(), current.getRank(), current.getCombinationCards());
                if (best == null || current.compareTo(best) > 0) {
                    best = current;
                    winners.clear();
                    winners.add(player);
                } else if (current.compareTo(best) == 0) {
                    winners.add(player);
                }
            }
        }
        for (PokerPlayer winner : winners) {
            int winnings = currentPot / winners.size();
            winner.addChips(winnings);
            LOG.infov("Player {0} wins {1} chips", winner.getId(), winnings);
        }
//        table.propagatePlayerStacks();
        LOG.infov("Best hand in session {0} is {1} with winners: {2}", sessionId, best, winners);
    }

    public void receiveAction(Action action) {
        if (players.containsKey(action.getPlayerId())) {
            PokerPlayer player = players.get(action.getPlayerId());
            if (currentPlayer.equals(player)) {
                switch (action.getActionType()) {
                    case FOLD -> {
                        player.fold();
                        LOG.infov("{0} has folded", player.getName());
                        passTurnToNextPlayer();
                    }
                    case CHECK -> {
                        LOG.infov("{0} has checked", player.getName());
                        Map<String, Integer> playerBets = players.entrySet().stream()
                                .filter(entry -> entry.getValue().isConnected() && !entry.getValue().isFolded())
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getBetAmount()));
                        if (!playerBets.isEmpty()) {
                            LOG.warnv("Player {0} attempted to check when there are bets in session {1}", action.getPlayerId(), sessionId);
                            return;
                        }
                        passTurnToNextPlayer();
                    }
                    case CALL -> {
                        if (action.getAmount() <= 0) {
                            LOG.warnv("Player {0} attempted to call with non-positive amount in session {1}", action.getPlayerId(), sessionId);
                            return; // no-op if call is non-positive
                        }
                        Map<String, Integer> playerBets = players.entrySet().stream()
                                .filter(entry -> entry.getValue().isConnected() && !entry.getValue().isFolded())
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getBetAmount()));
                        if (playerBets.isEmpty()) {
                            LOG.warnv("Player {0} called without any bets in session {1}", action.getPlayerId(), sessionId);
                            return; // no-op if no bets to call
                        } else {
                            int highestBet = playerBets.values().stream().max(Integer::compareTo).orElse(0);
                            if (highestBet > action.getAmount() + playerBets.get(currentPlayer.getId())) {
                                LOG.warnv("Player {0} attempted to call with insufficient amount in session {1}", action.getPlayerId(), sessionId);
                                return; // no-op if call is less than highest bet
                            }
                            currentPlayer.deductChips(action.getAmount());
                            currentPlayer.setTotalBet(currentPlayer.getTotalBet() + action.getAmount());
                            currentPlayer.setBetAmount(currentPlayer.getBetAmount() + action.getAmount());
                        }
                        int callAmount = action.getAmount();
                        player.deductChips(callAmount);
                        LOG.infov("{0} has called {1}", player.getName(), callAmount);
                        passTurnToNextPlayer();
                    }
                    case RAISE -> {
                        int raiseAmount = action.getAmount();
                        if (raiseAmount < minimumRaiseAmount) {
                            throw new IllegalArgumentException("Raise amount must be at least the minimum bet amount");
                        }
                        player.deductChips(raiseAmount);
                        player.setTotalBet(player.getTotalBet() + raiseAmount);
                        player.setBetAmount(player.getBetAmount() + raiseAmount);
                        LOG.infov("{0} has raised by {1}", player.getName(), raiseAmount);
                        passTurnToNextPlayer();
                    }
                    default -> throw new IllegalArgumentException("Unknown action type: " + action.getActionType());
                }
            } else {
                LOG.warnv("Action received from player {0} but it's not their turn", player.getName());
                throw new IllegalStateException("It's not your turn to act");
            }
        } else {
            LOG.warnv("Action received from unknown player with ID {0}", action.getPlayerId());
            throw new IllegalArgumentException("Unknown player ID: " + action.getPlayerId());
        }
    }

    public void advanceGameState() {
        LOG.infov("Advancing game state from {0} to {1}", stage, stage.nextStage());
        for (PokerPlayer player : players.values()) {
            currentPot += player.getBetAmount();
            player.setBetAmount(0);
        }
        switch (stage) {
            case PRE_FLOP -> {
                stage = Stage.FLOP;
                communityCards.add(deck.drawCard());
                communityCards.add(deck.drawCard());
                communityCards.add(deck.drawCard());
            }
            case FLOP -> {
                stage = Stage.TURN;
                communityCards.add(deck.drawCard());
            }
            case TURN -> {
                stage = Stage.RIVER;
                communityCards.add(deck.drawCard());
            }
            case RIVER -> stage = Stage.SHOWDOWN;
            case SHOWDOWN -> stage = Stage.FINISHED;
            case FINISHED -> throw new IllegalStateException("Game is already over");
            default -> throw new IllegalStateException("Invalid state");
        }

        if (stage != Stage.FINISHED) {
            rotateToNextActionQueue();
            passTurnToNextPlayer();
        } else {
            endGame();
        }
    }

    private void dealHoleCards() {
        for (PokerPlayer player : players.values()) {
            if (player.isConnected()) {
                player.addHoleCard(deck.drawCard());
                player.addHoleCard(deck.drawCard());
            }
        }
    }

    private void rotateToNextActionQueue() {
        actionQueue.clear();
        for (PokerPlayer player : players.values()) {
            if (player.isConnected()) {
                actionQueue.offer(player);
            }
        }
    }

    private void passTurnToNextPlayer() {
        if (actionQueue.isEmpty()) {
            advanceGameState();
        } else {
            currentPlayer = actionQueue.poll();
            LOG.infov("Current player is now {0}", currentPlayer.getName());
        }
    }

    private void betBlinds() {
        if (players.size() < 2) {
            throw new IllegalStateException("Not enough players to post blinds");
        }
        PokerPlayer smallBlindPlayer = actionQueue.poll();
        if (smallBlindPlayer == null) {
            for (PokerPlayer player : players.values()) {
                if (player.isConnected()) {
                    actionQueue.offer(player);
                }
            }
            smallBlindPlayer = actionQueue.poll();
        }
        if (smallBlindPlayer == null) {
            throw new IllegalStateException("No players available to post small blind");
        }
        smallBlindPlayer.deductChips(smallBlindAmount);
        smallBlindPlayer.setBetAmount(smallBlindAmount);
        LOG.infov("{0} posted small blind of {1}", smallBlindPlayer.getName(), smallBlindAmount);

        PokerPlayer bigBlindPlayer = actionQueue.poll();
        if (bigBlindPlayer == null) {
            for (PokerPlayer player : players.values()) {
                if (player.isConnected() && !player.equals(smallBlindPlayer)) {
                    actionQueue.offer(player);
                }
            }
            bigBlindPlayer = actionQueue.poll();
        }
        if (bigBlindPlayer == null) {
            throw new IllegalStateException("No players available to post big blind");
        }
        bigBlindPlayer.deductChips(bigBlindAmount);
        bigBlindPlayer.setBetAmount(bigBlindAmount);
        LOG.infov("{0} posted big blind of {1}", bigBlindPlayer.getName(), bigBlindAmount);

        if (actionQueue.isEmpty()) {
            rotateToNextActionQueue();
        }
    }

    public enum Stage {
        WAITING_FOR_PLAYERS,
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER,
        SHOWDOWN,
        FINISHED;

        public Stage nextStage() {
            return switch (this) {
                case WAITING_FOR_PLAYERS -> PRE_FLOP;
                case PRE_FLOP -> FLOP;
                case FLOP -> TURN;
                case TURN -> RIVER;
                case RIVER -> SHOWDOWN;
                case SHOWDOWN -> FINISHED;
                default -> throw new IllegalStateException("No next stage available from " + this);
            };
        }
    }
}
