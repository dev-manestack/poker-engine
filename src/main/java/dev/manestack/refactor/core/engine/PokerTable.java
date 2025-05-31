package dev.manestack.refactor.core.engine;

import dev.manestack.refactor.core.model.Table;
import dev.manestack.service.GameService;
import dev.manestack.service.user.User;

import java.util.*;

public class PokerTable {
    private Table metadata;
    private final Set<String> subscribedSessions = new HashSet<>();
    private final Map<Integer, PokerPlayer> seats = new HashMap<>();
    private final GameService service;

    public PokerTable(GameService service, Table metadata) {
        this.metadata = metadata;
        this.service = service;
    }

    public void subscribe(String sessionId) {
        subscribedSessions.add(sessionId);
    }

    public void unsubscribe(String sessionId) {
        subscribedSessions.remove(sessionId);
    }

    public void takeSeat(User user, int buyInAmount, int seatNumber) {
        if (seats.containsKey(seatNumber)) {
            throw new IllegalArgumentException("Seat " + seatNumber + " is already taken.");
        }
        if (buyInAmount < metadata.getMinBuyIn() || buyInAmount > metadata.getMaxBuyIn()) {
            throw new IllegalArgumentException("Buy-in amount must be between " + metadata.getMinBuyIn() + " and " + metadata.getMaxBuyIn());
        }
        leaveSeat(user.getUserId());
        PokerPlayer player = new PokerPlayer(UUID.randomUUID().toString(), user.getUsername(), user.getUserId(), buyInAmount);
        seats.put(seatNumber, player);
        // Remove from balance.
    }

    public void leaveSeat(Integer userId) {
        for (Map.Entry<Integer, PokerPlayer> entry : seats.entrySet()) {
            if (Objects.equals(entry.getValue().getUserId(), userId)) {
                PokerPlayer player = seats.remove(entry.getKey());
                int leftChips = player.getChips();
                // Add it back to balance.
                return;
            }
        }
    }

    /*
     * Getters & Setters
     */
    public void setMetadata(Table metadata) {
        this.metadata = metadata;
    }
    // Getters and setters for the fields can be added here
}
