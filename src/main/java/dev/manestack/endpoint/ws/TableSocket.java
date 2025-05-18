package dev.manestack.endpoint.ws;

import dev.manestack.util.Utilities;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/table/{tableId}")
@ApplicationScoped
public class TableSocket {
    private static final Logger LOG = Logger.getLogger(TableSocket.class);
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Inject
    JWTParser parser;

    @OnOpen
    public void onOpen(Session session, @PathParam("tableId") String tableId) {
        Map<String, String> queryParams = Utilities.parseQuery(session.getQueryString());
        String userId = parseUserId(queryParams.get("accessToken"));
        if (userId == null) {
            LOG.error("User ID is missing in the query parameters.");
            try {
                session.close();
            } catch (Exception ignored) {}
            return;
        }
        sessions.put(userId, session);
        LOG.infov("User {0} connected to table {1}", userId, tableId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("tableId") String tableId) {
        Map<String, String> queryParams = Utilities.parseQuery(session.getQueryString());
        String userId = parseUserId(queryParams.get("accessToken"));
        if (userId == null) {
            LOG.error("User ID is missing in the query parameters.");
            try {
                session.close();
            } catch (Exception ignored) {}
            return;
        }
        sessions.remove(userId);
        LOG.infov("User {0} disconnected from table {1}", userId, tableId);
    }

    @OnError
    public void onError(Session session, @PathParam("tableId") String tableId, Throwable throwable) {
        Map<String, String> queryParams = Utilities.parseQuery(session.getQueryString());
        String userId = parseUserId(queryParams.get("accessToken"));
        if (userId == null) {
            LOG.error("User ID is missing in the query parameters.");
            try {
                session.close();
            } catch (Exception ignored) {}
            return;
        }
        LOG.errorv("Error occurred for user {0} on table {1}: {2}", userId, tableId, throwable.getMessage());
    }

    @OnMessage
    public void onMessage(Session session, String message, @PathParam("tableId") String tableId) {
        Map<String, String> queryParams = Utilities.parseQuery(session.getQueryString());
        String userId = parseUserId(queryParams.get("accessToken"));
        if (userId == null) {
            LOG.error("User ID is missing in the query parameters.");
            try {
                session.close();
            } catch (Exception ignored) {}
            return;
        }
        LOG.infov("Received message from user {0} on table {1}: {2}", userId, tableId, message);
    }

    private void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }

    private String parseUserId(String accessToken) {
        return accessToken;
    }
}
