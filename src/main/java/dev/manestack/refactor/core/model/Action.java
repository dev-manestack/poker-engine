package dev.manestack.refactor.core.model;

public class Action {
    private String actionId;
    private String playerId;
    private Long tableId;
    private ActionType actionType; // e.g., "bet", "fold", "call", "raise"
    private Integer amount; // Amount of chips involved in the action
    private String timestamp; // ISO 8601 format

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public enum ActionType {
        FOLD,
        SMALL_BLIND,
        BIG_BLIND,
        CALL,
        RAISE,
        CHECK
    }
}
