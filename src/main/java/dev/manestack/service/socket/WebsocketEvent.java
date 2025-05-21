package dev.manestack.service.socket;

import io.vertx.core.json.JsonObject;

public class WebsocketEvent {
    private String id;
    private String type;
    private JsonObject data;

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "WebsocketEvent{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", data=" + data +
                '}';
    }
}
