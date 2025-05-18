package dev.manestack.service.poker.event;

import io.vertx.core.json.JsonObject;

public class TableEvent extends BaseEvent {
    private final String tableId;
    private final TableEventType tableEventType;
    private final String context;

    public TableEvent(String tableId, TableEventType tableEventType, String context) {
        super(EventType.TABLE_EVENT);
        this.tableId = tableId;
        this.tableEventType = tableEventType;
        this.context = context;
    }

    @Override
    JsonObject getEventData() {
        return JsonObject.mapFrom(this);
    }

    public String getTableId() {
        return tableId;
    }

    public TableEventType getTableEventType() {
        return tableEventType;
    }

    public String getContext() {
        return context;
    }

    public enum TableEventType {
        TABLE_CREATED,
        TABLE_CLOSED,
        TABLE_JOINED,
        TABLE_LEFT,
        TABLE_STARTED,
        TABLE_ENDED,
    }
}
