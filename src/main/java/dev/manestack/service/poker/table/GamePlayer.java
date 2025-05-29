package dev.manestack.service.poker.table;

import dev.manestack.service.poker.card.GameCard;
import dev.manestack.service.user.User;

import java.util.ArrayList;
import java.util.List;

public class GamePlayer {
    private User user;
    private Integer seatId;
    private Integer stack;
    private boolean inHand = false;
    private boolean isAllIn = false;
    private Integer totalContribution = 0;
    private final List<GameCard> holeCards = new ArrayList<>();

    public GamePlayer() {
    }

    public GamePlayer(User user, Integer balance) {
        this.stack = balance;
        this.user = user;
    }

    public Integer getSeatId() {
        return seatId;
    }

    public void setSeatId(Integer seatId) {
        this.seatId = seatId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getStack() {
        return stack;
    }

    public void setStack(Integer stack) {
        this.stack = stack;
    }

    public void deductFromStack(int amount) {
        if (amount > stack) {
            throw new IllegalArgumentException("Cannot deduct more than current stack");
        }
        this.stack -= amount;
    }

    public void addToStack(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative amount to stack");
        }
        this.stack += amount;
    }

    public void addCard(GameCard gameCard) {
        holeCards.add(gameCard);
    }

    public void refreshHoleCards() {
        holeCards.clear();
    }

    public boolean isAllIn() {
        return isAllIn;
    }

    public void setAllIn(boolean allIn) {
        isAllIn = allIn;
    }

    public Integer getTotalContribution() {
        return totalContribution;
    }

    public void setTotalContribution(Integer totalContribution) {
        this.totalContribution = totalContribution;
    }

    public void addToTotalContribution(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative amount to total contribution");
        }
        this.totalContribution += amount;
    }

    public void resetTotalContribution() {
        this.totalContribution = 0;
    }

    public boolean isInHand() {
        return inHand;
    }

    public void setInHand(boolean inHand) {
        this.inHand = inHand;
    }

    public List<GameCard> getHoleCards() {
        return holeCards;
    }
}
