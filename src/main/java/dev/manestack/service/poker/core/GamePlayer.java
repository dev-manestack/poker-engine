package dev.manestack.service.poker.core;

import dev.manestack.service.user.User;

public class GamePlayer {
    private User user;
    private Integer tableBalance;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getTableBalance() {
        return tableBalance;
    }

    public void setTableBalance(Integer tableBalance) {
        this.tableBalance = tableBalance;
    }
}
