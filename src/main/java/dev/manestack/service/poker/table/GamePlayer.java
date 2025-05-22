package dev.manestack.service.poker.table;

import dev.manestack.service.user.User;

public class GamePlayer {
    private User user;
    private Integer balance;

    public GamePlayer() {
    }

    public GamePlayer(User user, Integer balance) {
        this.balance = balance;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }
}
