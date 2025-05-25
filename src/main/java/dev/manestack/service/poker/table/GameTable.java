package dev.manestack.service.poker.table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.manestack.service.socket.WebsocketSession;
import dev.manestack.service.user.User;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GameTable {
    private static final Logger LOG = Logger.getLogger(GameTable.class);
    private Long tableId;
    private String tableName;
    private Integer maxPlayers;
    private Integer bigBlind;
    private Integer smallBlind;
    private Integer minBuyIn;
    private Integer maxBuyIn;
    private String variant;
    private OffsetDateTime createdAt;
    private Integer createdBy;
    private GameSession currentGameSession = null;
    private Integer currentDealer = 0;
    private final Map<Integer, GamePlayer> seats = new HashMap<>();
    private final Map<Integer, User> waitingList = new HashMap<>();
    @JsonIgnore
    private final Map<String, WebsocketSession> involvedSessions = new HashMap<>();

    public void validateCreate() {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        if (maxPlayers == null || maxPlayers <= 0) {
            throw new IllegalArgumentException("Max players must be greater than 0");
        }
        if (bigBlind == null || bigBlind <= 0) {
            throw new IllegalArgumentException("Big blind must be greater than 0");
        }
        if (smallBlind == null || smallBlind <= 0) {
            throw new IllegalArgumentException("Small blind must be greater than 0");
        }
        if (minBuyIn == null || minBuyIn <= 0) {
            throw new IllegalArgumentException("Min buy-in must be greater than 0");
        }
        if (maxBuyIn == null || maxBuyIn <= 0) {
            throw new IllegalArgumentException("Max buy-in must be greater than 0");
        }
    }

    public void joinWaitingList(User user, WebsocketSession session) {
        waitingList.put(user.getUserId(), user);
        involvedSessions.put(session.getId(), session);
    }

    public boolean leaveTable(User user, WebsocketSession session) {
        boolean isPlayer = false;
        for (Map.Entry<Integer, GamePlayer> entry : seats.entrySet()) {
            if (entry.getValue().getUser().getUserId() == user.getUserId()) {
                leaveSeat(entry.getKey(), user.getUserId(), session);
                isPlayer = true;
                break;
            }
        }
        waitingList.remove(user.getUserId());
        involvedSessions.remove(session.getId());
        return isPlayer;
    }

    public void takeSeat(int seatNumber, GamePlayer gamePlayer, WebsocketSession session) {
        if (seats.containsKey(seatNumber)) {
            throw new IllegalArgumentException("Seat " + seatNumber + " is already taken");
        }
        if (seats.size() >= maxPlayers) {
            throw new IllegalStateException("No more seats available");
        }
        for (Map.Entry<Integer, GamePlayer> entry : seats.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getUser().getUserId() == gamePlayer.getUser().getUserId()) {
                leaveSeat(entry.getKey(), entry.getValue().getUser().getUserId(), session);
            }
        }
        seats.put(seatNumber, gamePlayer);
        involvedSessions.put(session.getId(), session);

        System.out.println("HERE: " + seats + " " + currentGameSession);
        if (currentGameSession == null) {
            long nonNullPlayers = seats.values().stream().filter(Objects::nonNull).count();
            System.out.println(nonNullPlayers);
            if (nonNullPlayers >= 2) {
                startGame();
            }
        }
    }

    public void leaveSeat(int seatNumber, Integer userId, WebsocketSession session) {
        if (!seats.containsKey(seatNumber)) {
            throw new IllegalArgumentException("Seat " + seatNumber + " is not occupied");
        } else if (seats.get(seatNumber).getUser().getUserId() != userId) {
            throw new IllegalArgumentException("You cannot leave a seat that is not yours");
        }
        involvedSessions.remove(session.getId());
        seats.remove(seatNumber);
    }

    public void startGame() {
        if (currentGameSession != null) {
            throw new IllegalStateException("Game is already in progress");
        }
        if (seats.size() < 2) {
            throw new IllegalStateException("Not enough players to start the game");
        }
        LOG.infov("Starting game at table {0} with players: {1}", tableName, seats.values());
        currentGameSession = new GameSession(System.currentTimeMillis(), this, currentDealer, seats);
        currentGameSession.startGame();
    }

    public void receivePlayerAction(Integer playerId, GameSession.ActionType actionType, int amount) {
        if (currentGameSession == null) {
            throw new IllegalStateException("No game in progress");
        }
        currentGameSession.receivePlayerAction(playerId, actionType, amount);
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Integer getBigBlind() {
        return bigBlind;
    }

    public void setBigBlind(Integer bigBlind) {
        this.bigBlind = bigBlind;
    }

    public Integer getSmallBlind() {
        return smallBlind;
    }

    public void setSmallBlind(Integer smallBlind) {
        this.smallBlind = smallBlind;
    }

    public Integer getMinBuyIn() {
        return minBuyIn;
    }

    public void setMinBuyIn(Integer minBuyIn) {
        this.minBuyIn = minBuyIn;
    }

    public Integer getMaxBuyIn() {
        return maxBuyIn;
    }

    public void setMaxBuyIn(Integer maxBuyIn) {
        this.maxBuyIn = maxBuyIn;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Map<Integer, GamePlayer> getSeats() {
        return seats;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, WebsocketSession> getInvolvedSessions() {
        return involvedSessions;
    }

    public enum TableAction {
        JOIN_TABLE,
        TAKE_SEAT,
        LEAVE_SEAT,
    }

}
