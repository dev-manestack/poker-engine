package dev.manestack.service.socket;

import dev.manestack.service.poker.table.GameTable;
import dev.manestack.service.user.User;

public class WebsocketSession {
    private final String id;
    private GameTable table;
    private User user;

    public WebsocketSession(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GameTable getTable() {
        return table;
    }

    public void setTable(GameTable table) {
        this.table = table;
    }
}
