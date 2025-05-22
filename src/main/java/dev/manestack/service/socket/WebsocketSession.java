package dev.manestack.service.socket;

import dev.manestack.service.user.User;

public class WebsocketSession {
    private final String id;
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
}
