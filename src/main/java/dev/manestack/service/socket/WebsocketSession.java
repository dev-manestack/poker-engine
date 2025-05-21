package dev.manestack.service.socket;

import dev.manestack.service.user.User;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;

public class WebsocketSession {
    private final String id;
    private final WebSocketConnection connection;
    private User user;

    public WebsocketSession(String id, WebSocketConnection connection) {
        this.id = id;
        this.connection = connection;
    }

    public Uni<Void> respond() {
        return connection.sendText("test");
    }

    public String getId() {
        return id;
    }

    public WebSocketConnection getConnection() {
        return connection;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
