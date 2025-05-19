package dev.manestack.service.user;

import dev.manestack.jooq.generated.tables.records.PokerDepositRecord;
import io.vertx.core.json.JsonObject;

import java.time.OffsetDateTime;

public class Deposit {
    private Long depositId;
    private Integer userId;
    private Integer adminId;
    private Integer amount;
    private DepositType type;
    private JsonObject details;
    private OffsetDateTime createDate;

    public Deposit() {
    }

    public Deposit(PokerDepositRecord record) {
        this.depositId = record.getDepositId();
        this.userId = record.getUserId();
        this.adminId = record.getAdminId();
        this.amount = record.getAmount();
        this.type = DepositType.valueOf(record.getType());
        this.details = new JsonObject(record.getDetails().data());
        this.createDate = record.getCreateDate();
    }

    public void validate() {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive integer.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive integer.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Deposit type cannot be null.");
        }
        if (details == null) {
            throw new IllegalArgumentException("Details cannot be null.");
        }
    }

    public Long getDepositId() {
        return depositId;
    }

    public void setDepositId(Long depositId) {
        this.depositId = depositId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public DepositType getType() {
        return type;
    }

    public void setType(DepositType type) {
        this.type = type;
    }

    public OffsetDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(OffsetDateTime createDate) {
        this.createDate = createDate;
    }

    public JsonObject getDetails() {
        return details;
    }

    public void setDetails(JsonObject details) {
        this.details = details;
    }

    public enum DepositType {
        CASH,
        BANK_TRANSFER,
        GIFT
    }
}
