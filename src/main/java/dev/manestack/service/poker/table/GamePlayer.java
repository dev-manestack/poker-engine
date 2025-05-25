package dev.manestack.service.poker.table;

import dev.manestack.service.user.User;

public class GamePlayer {
    private User user;
    private Integer stack;
    private boolean inHand = false;

    public GamePlayer() {
    }

    public GamePlayer(User user, Integer balance) {
        this.stack = balance;
        this.user = user;
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

    public boolean isInHand() {
        return inHand;
    }

    public void setInHand(boolean inHand) {
        this.inHand = inHand;
    }
}
