package dev.manestack.service;

import dev.manestack.jooq.generated.tables.records.PokerTableRecord;
import dev.manestack.service.poker.table.GamePlayer;
import dev.manestack.service.poker.table.GameSession;
import dev.manestack.service.poker.table.GameTable;
import dev.manestack.service.socket.WebsocketEvent;
import dev.manestack.service.socket.WebsocketSession;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.UpdateSetFirstStep;
import org.jooq.UpdateSetMoreStep;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.manestack.jooq.generated.Tables.POKER_TABLE;

@ApplicationScoped
public class GameService {
    private static final Logger LOG = Logger.getLogger(UserService.class);
    private final ExecutorService QUERY_THREADS = Executors.newFixedThreadPool(3);
    private final ExecutorService GAMEPLAY_THREAD = Executors.newFixedThreadPool(3);
    private final Map<Long, GameTable> TABLES = new HashMap<>();
    private final Map<String, WebsocketSession> SOCKET_SESSIONS = new HashMap<>();

    private MultiEmitter<? super WebsocketEvent> EVENT_HANDLER_EMITTER;
    private MultiEmitter<? super WebsocketEvent> EVENT_NOTIFIER_EMITTER;
    private Cancellable EVENT_HANDLER_TASK;
    private Cancellable EVENT_NOTIFIER_TASK;

    @Inject
    DSLContext context;
    @Inject
    JWTParser jwtParser;
    @Inject
    UserService userService;
    @Inject
    OpenConnections openConnections;

    public void init(@Observes StartupEvent ignored) {
        fetchTables().invoke(tables -> {
                    for (GameTable table : tables) {
                        table.connectToServer(this);
                        TABLES.put(table.getTableId(), table);
                    }
                })
                .subscribe().with(unused -> {
                });

        Multi<WebsocketEvent> eventHandlerMulti = Multi.createFrom().emitter(em -> EVENT_HANDLER_EMITTER = em);

        EVENT_HANDLER_TASK = eventHandlerMulti
                .emitOn(GAMEPLAY_THREAD)
                .call(this::handleMessage)
                .subscribe().with(unused -> {
                        }, failure -> LOG.errorv("Socket open failed: {0}", failure.getMessage()),
                        () -> LOG.infov("Socket open completed"));

        Multi<WebsocketEvent> eventNotifierMulti = Multi.createFrom().emitter(em -> EVENT_NOTIFIER_EMITTER = em);

        EVENT_NOTIFIER_TASK = eventNotifierMulti
                .emitOn(GAMEPLAY_THREAD)
                .call(this::sendMessageToConnection)
                .subscribe().with(unused -> {
                        }, failure -> LOG.errorv("Socket notifier failed: {0}", failure.getMessage()),
                        () -> LOG.infov("Socket notifier completed"));
    }

    public void shutdown(@Observes ShutdownEvent ignored) {
        if (EVENT_HANDLER_TASK != null) {
            EVENT_HANDLER_TASK.cancel();
        }
        if (EVENT_HANDLER_EMITTER != null) {
            EVENT_HANDLER_EMITTER.complete();
        }
        if (EVENT_NOTIFIER_TASK != null) {
            EVENT_NOTIFIER_TASK.cancel();
        }
        if (EVENT_NOTIFIER_EMITTER != null) {
            EVENT_NOTIFIER_EMITTER.complete();
        }
        QUERY_THREADS.shutdown();
        GAMEPLAY_THREAD.shutdown();
        LOG.infov("GameService shutdown completed");
    }

    /*
     * Socket Events
     */
    private void handleConnectedEvent(WebsocketEvent event) {
        LOG.infov("Received connected event for {0}: {1}", event.getId(), event.getData());
        EVENT_NOTIFIER_EMITTER.emit(event);
    }

    private void handleDisconnectEvent(WebsocketEvent event) {
        WebsocketSession session = SOCKET_SESSIONS.get(event.getId());
        if (session == null) {
            return;
        }
        if (session.getTable() != null) {
            GameTable gameTable = session.getTable();
            gameTable.leaveTable(session.getUser(), session);
        }
    }

    private Uni<Void> handleAuthEvent(WebsocketEvent event) {
        return Uni.createFrom().voidItem()
                .call(() -> {
                    LOG.infov("Received auth event for {0}: {1}", event.getId(), event.getData());
                    String accessToken = event.getData().getString("accessToken");
                    try {
                        WebsocketSession session = SOCKET_SESSIONS.get(event.getId());
                        if (session == null) {
                            return Uni.createFrom().voidItem();
                        }
                        Integer userId = Integer.parseInt(jwtParser.parse(accessToken).getSubject());
                        return userService.fetchUser(userId)
                                .invoke(session::setUser)
                                .invoke(() -> LOG.infov("User {0} authenticated", userId))
                                .invoke(user -> EVENT_NOTIFIER_EMITTER.emit(new WebsocketEvent(
                                        event.getId(),
                                        "AUTH",
                                        new JsonObject()
                                                .put("user", user)
                                )));
                    } catch (Exception e) {
                        LOG.errorv("Invalid token: {0}", e.getMessage());
                        return Uni.createFrom().voidItem();
                    }
                });
    }

    private void handleTableEvent(WebsocketEvent event) {
        Long tableId = event.getData().getLong("tableId");
        GameTable.TableAction action = GameTable.TableAction.valueOf(event.getData().getString("action"));
        WebsocketSession session = SOCKET_SESSIONS.get(event.getId());
        if (session == null) {
            LOG.errorv("Session not found for {0}", event.getId());
            return;
        }
        GameTable table = TABLES.get(tableId);
        if (table == null) {
            throw new RuntimeException("Table not found");
        }
        switch (action) {
            case JOIN_TABLE -> {
                Integer userId = session.getUser().getUserId();
                table.joinWaitingList(session.getUser(), session);
                LOG.infov("User {0} joined waiting list for table {1}", userId, tableId);
                EVENT_NOTIFIER_EMITTER.emit(new WebsocketEvent(
                        session.getId(),
                        "TABLE",
                        new JsonObject()
                                .put("action", "JOIN_TABLE")
                                .put("tableId", tableId)
                                .put("table", table)
                ));
            }
            case TAKE_SEAT -> {
                Integer seatNumber = event.getData().getInteger("seatIndex");
                GamePlayer gamePlayer = new GamePlayer(session.getUser(), 500);
                table.takeSeat(seatNumber, gamePlayer, session);
                LOG.infov("User {0} took seat {1} at table {2}", session.getUser(), seatNumber, tableId);
                session.setTable(table);
            }
            case LEAVE_SEAT -> {
                Integer seatNumber = event.getData().getInteger("seatIndex");
                table.leaveSeat(seatNumber, session.getUser().getUserId(), session);
                session.setTable(null);
                LOG.infov("User {0} left seat {1} at table {2}", event.getId(), seatNumber, tableId);
                EVENT_NOTIFIER_EMITTER.emit(new WebsocketEvent(
                        event.getId(),
                        "TABLE",
                        new JsonObject()
                                .put("action", "LEAVE_SEAT")
                                .put("tableId", tableId)
                                .put("table", table)
                ));
            }
        }
    }

    private void handleGameEvent(WebsocketEvent event) {
        LOG.infov("Received game event for {0}: {1}", event.getId(), event.getData());
        Long tableId = event.getData().getLong("tableId");
        GameSession.ActionType action = GameSession.ActionType.valueOf(event.getData().getString("action"));
        Integer amount = event.getData().getInteger("amount", 0);
        WebsocketSession session = SOCKET_SESSIONS.get(event.getId());
        if (session == null) {
            LOG.errorv("Session not found for {0}", event.getId());
            return;
        }
        GameTable table = TABLES.get(tableId);
        if (table == null) {
            throw new RuntimeException("Table not found");
        }
        table.receivePlayerAction(session.getUser().getUserId(), action, amount);
    }

    private Uni<Void> handleMessage(WebsocketEvent event) {
        return Uni.createFrom().voidItem()
                .call(() -> {
                    WebsocketSession session = SOCKET_SESSIONS.get(event.getId());
                    if (session == null) {
                        LOG.errorv("Session not found for {0}", event.getId());
                        return Uni.createFrom().voidItem();
                    }
                    switch (event.getType()) {
                        case "CONNECTED" -> handleConnectedEvent(event);
                        case "DISCONNECTED" -> handleDisconnectEvent(event);
                        case "TABLE" -> handleTableEvent(event);
                        case "GAME" -> handleGameEvent(event);
                        case "AUTH" -> {
                            return handleAuthEvent(event);
                        }
                        default -> LOG.infov("Received unknown event for {0}: {1}", event.getId(), event.getData());
                    }
                    return Uni.createFrom().voidItem();
                })
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorv(throwable, "Error handling event {0}: {1}", event.getId(), throwable.getMessage());
                    EVENT_NOTIFIER_EMITTER.emit(new WebsocketEvent(
                            event.getId(),
                            "ERROR",
                            new JsonObject().put("error", throwable.getMessage())
                    ));
                    return Uni.createFrom().voidItem();
                });
    }

    public void sendWebsocketEvent(WebsocketEvent event) {
        EVENT_NOTIFIER_EMITTER.emit(event);
    }

    // This should only be called from EVENT_NOTIFIER_EMITTER emitter.
    private Uni<Void> sendMessageToConnection(WebsocketEvent event) {
        Optional<WebSocketConnection> optionalConnection = openConnections.findByConnectionId(event.getId());
        if (optionalConnection.isPresent()) {
            WebSocketConnection connection = optionalConnection.get();
            return connection.sendText(event);
        } else {
            LOG.infov("Cannot send event to stale connection: {0}", event.getId());
            return Uni.createFrom().voidItem();
        }
    }

    /*
     * Websocket Event Emitters
     */

    public void handleOnConnectEvent(String id) {
        LOG.infov("Received connection event for {0}", id);
        SOCKET_SESSIONS.put(id, new WebsocketSession(id));
        addWebsocketEventToQueue(id, new WebsocketEvent(
                id,
                "CONNECTED",
                new JsonObject()
        ));
    }

    public void handleOnCloseEvent(String id) {
        LOG.infov("Received close event for {0}", id);
        addWebsocketEventToQueue(id, new WebsocketEvent(
                id,
                "DISCONNECTED",
                new JsonObject()
        ));
    }

    public void addWebsocketEventToQueue(String id, WebsocketEvent event) {
        LOG.infov("Emitting message event for {0}: {1}", id, event);
        event.setId(id);
        EVENT_HANDLER_EMITTER.emit(event);
    }

    /*
     * CRUD Operations
     */
    public Uni<List<GameTable>> fetchTables() {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> context.selectFrom(POKER_TABLE)
                        .fetchInto(GameTable.class));
    }

    public Uni<GameTable> createTable(Integer userId, GameTable table) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    table.validateCreate();
                    PokerTableRecord pokerTableRecord = context.insertInto(POKER_TABLE)
                            .set(POKER_TABLE.TABLE_NAME, table.getTableName())
                            .set(POKER_TABLE.MAX_PLAYERS, table.getMaxPlayers())
                            .set(POKER_TABLE.BIG_BLIND, table.getBigBlind())
                            .set(POKER_TABLE.SMALL_BLIND, table.getSmallBlind())
                            .set(POKER_TABLE.MIN_BUY_IN, table.getMinBuyIn())
                            .set(POKER_TABLE.MAX_BUY_IN, table.getMaxBuyIn())
                            .set(POKER_TABLE.VARIANT, table.getVariant())
                            .set(POKER_TABLE.CREATED_AT, OffsetDateTime.now())
                            .set(POKER_TABLE.CREATED_BY, userId)
                            .returning(POKER_TABLE.TABLE_ID)
                            .fetchOne();
                    if (pokerTableRecord != null) {
                        table.setTableId(pokerTableRecord.getTableId());
                        table.setCreatedAt(pokerTableRecord.getCreatedAt());
                        table.setCreatedBy(pokerTableRecord.getCreatedBy());
                        TABLES.put(table.getTableId(), table);
                        LOG.infov("Created table {0}", table.getTableName());
                        table.connectToServer(this);
                        return table;
                    } else {
                        LOG.errorv("Failed to create table {0}", table.getTableName());
                        throw new RuntimeException("Failed to create table");
                    }
                });
    }

    public Uni<GameTable> updateTable(GameTable table) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    UpdateSetFirstStep<?> update = context.update(POKER_TABLE);
                    UpdateSetMoreStep<?> updateSetMoreStep = null;
                    if (table.getTableName() != null)
                        updateSetMoreStep = update.set(POKER_TABLE.TABLE_NAME, table.getTableName());
                    if (table.getMaxPlayers() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.MAX_PLAYERS, table.getMaxPlayers()) : update.set(POKER_TABLE.MAX_PLAYERS, table.getMaxPlayers());
                    if (table.getBigBlind() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.BIG_BLIND, table.getBigBlind()) : update.set(POKER_TABLE.BIG_BLIND, table.getBigBlind());
                    if (table.getSmallBlind() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.SMALL_BLIND, table.getSmallBlind()) : update.set(POKER_TABLE.SMALL_BLIND, table.getSmallBlind());
                    if (table.getMinBuyIn() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.MIN_BUY_IN, table.getMinBuyIn()) : update.set(POKER_TABLE.MIN_BUY_IN, table.getMinBuyIn());
                    if (table.getMaxBuyIn() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.MAX_BUY_IN, table.getMaxBuyIn()) : update.set(POKER_TABLE.MAX_BUY_IN, table.getMaxBuyIn());
                    if (table.getVariant() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.VARIANT, table.getVariant()) : update.set(POKER_TABLE.VARIANT, table.getVariant());
                    if (updateSetMoreStep == null) {
                        LOG.errorv("No fields to update for table {0}", table.getTableName());
                        throw new RuntimeException("No fields to update");
                    }
                    GameTable updatedTable = updateSetMoreStep
                            .where(POKER_TABLE.TABLE_ID.eq(table.getTableId()))
                            .returning()
                            .fetchOneInto(GameTable.class);

                    if (updatedTable != null) {
                        LOG.infov("Updated table {0}", table.getTableName());
                        TABLES.put(table.getTableId(), updatedTable);
                        updatedTable.connectToServer(this);
                        return updatedTable;
                    } else {
                        LOG.errorv("Failed to update table {0}", table.getTableName());
                        throw new RuntimeException("Failed to update table");
                    }
                });
    }

    public Uni<Void> deleteTable(Long tableId, Integer userId) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    LOG.infov("User {0} is deleting table {1}", userId, tableId);
                    context.deleteFrom(POKER_TABLE)
                            .where(POKER_TABLE.TABLE_ID.eq(tableId))
                            .execute();
                    TABLES.remove(tableId);
                    LOG.infov("Deleted table {0}", tableId);
                    return null;
                });
    }
}
