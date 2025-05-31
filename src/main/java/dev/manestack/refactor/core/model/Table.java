package dev.manestack.refactor.core.model;

import java.time.OffsetDateTime;

public class Table {
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
}
