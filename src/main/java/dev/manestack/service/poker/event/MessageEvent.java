package dev.manestack.service.poker.event;

import dev.manestack.service.user.User;
import io.vertx.core.json.JsonObject;

public class MessageEvent extends BaseEvent {
    private final User user;
    private final String message;
    private final long timestamp;

    public MessageEvent(User user, String message) {
        super(EventType.CHAT_EVENT);
        this.user = user;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    JsonObject getEventData() {
        return JsonObject.mapFrom(this);
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
