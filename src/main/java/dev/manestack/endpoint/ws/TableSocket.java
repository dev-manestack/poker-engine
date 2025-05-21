package dev.manestack.endpoint.ws;

import dev.manestack.service.GameService;
import dev.manestack.service.socket.WebsocketEvent;
import io.quarkus.websockets.next.*;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@WebSocket(path = "/table")
public class TableSocket {
    private static final Logger LOG = Logger.getLogger(TableSocket.class);
    @Inject
    WebSocketConnection connection;
    @Inject
    GameService gameService;

    @OnOpen
    public void onOpen() {
        gameService.handleOnConnectEvent(connection.id(), connection);
    }

    @OnClose
    public void onClose() {
        gameService.handleOnCloseEvent(connection.id());
    }

    @OnTextMessage
    public void onTextMessage(WebsocketEvent websocketEvent) {
        gameService.emitMessageToHandler(connection.id(), websocketEvent);
    }
}
