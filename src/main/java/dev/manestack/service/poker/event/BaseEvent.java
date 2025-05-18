package dev.manestack.service.poker.event;

import io.vertx.core.json.JsonObject;

public abstract class BaseEvent {
    private final long timestamp;
    private final EventType eventType;

    public BaseEvent(EventType eventType) {
        this.timestamp = System.currentTimeMillis();
        this.eventType = eventType;
    }

    abstract JsonObject getEventData();

    public enum EventType {
        CHAT_EVENT,
        TABLE_EVENT,
        GAME_EVENT,
    }
}
