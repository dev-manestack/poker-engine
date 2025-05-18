package dev.manestack.service.poker.core;

import dev.manestack.service.poker.event.BaseEvent;
import dev.manestack.service.poker.event.MessageEvent;
import dev.manestack.service.user.User;
import io.smallrye.mutiny.subscription.MultiEmitter;

import java.util.List;
import java.util.Map;

public class GameTable {
    private String id;
    private String name;
    private GameVariant gameType;
    private int maxPlayers;
    private int minimumBuyIn;
    private int maximumBuyIn;
    private double rakePercentage;
    private Theme theme;

    private Map<String, GamePlayer> playerSeats;
    private List<User> waitingPlayers;
    private List<MessageEvent> messages;
    private MultiEmitter<? super BaseEvent> eventEmitter;

    public enum Theme {
        CLASSIC("Classic"),
        MODERN("Modern"),
        DARK("Dark"),
        LIGHT("Light");

        private final String displayName;

        Theme(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
