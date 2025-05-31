package dev.manestack.refactor.core.engine;

import dev.manestack.refactor.core.model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PokerPlayer {
    private String id; // Unique identifier for the player
    private String name;
    private Integer userId;
    private int chips; // Current chip count
    private boolean isReady; // Indicates if the player is ready for the next round
    private boolean isFolded; // Indicates if the player has folded in the current round
    private boolean isAllIn; // Indicates if the player has gone all-in
    private int betAmount; // Amount the player has bet in the current round
    private int totalBet; // Total amount bet by the player in the current session
    private boolean isConnected;
    private final List<Card> holeCards = new ArrayList<>(); // Player's hole cards

    public PokerPlayer(String id, String name, Integer userId, int chips) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.chips = chips;
        this.isReady = false;
        this.isFolded = false;
        this.isAllIn = false;
        this.betAmount = 0;
        this.totalBet = 0;
        this.isConnected = true;
    }

    /*
     * Business methods
     */

    public void addHoleCard(Card card) {
        if (holeCards.size() < 2) {
            holeCards.add(card);
        } else {
            throw new IllegalStateException("Cannot add more than two hole cards");
        }
    }

    public List<Card> getHoleCards() {
        return new ArrayList<>(holeCards); // Return a copy to prevent external modification
    }

    public void resetForNewRound() {
        isReady = false;
        isFolded = false;
        isAllIn = false;
        betAmount = 0;
        holeCards.clear(); // Clear hole cards for the new round
    }

    public void fold() {
        isFolded = true;
        betAmount = 0; // Reset bet amount when folding
    }

    public void deductChips(int amount) {
        if (amount > chips) {
            throw new IllegalArgumentException("Cannot deduct more chips than available");
        }
        chips -= amount;
        totalBet += amount;
    }

    public void addChips(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative chips");
        }
        chips += amount;
    }

    /*
     * Getters and Setters
     */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public boolean isFolded() {
        return isFolded;
    }

    public void setFolded(boolean folded) {
        isFolded = folded;
    }

    public boolean isAllIn() {
        return isAllIn;
    }

    public void setAllIn(boolean allIn) {
        isAllIn = allIn;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    public int getTotalBet() {
        return totalBet;
    }

    public void setTotalBet(int totalBet) {
        this.totalBet = totalBet;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PokerPlayer that = (PokerPlayer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
