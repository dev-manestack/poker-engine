package dev.manestack.service.user;

public class UserBalance {
    private Integer userId;
    private Integer balance;
    private Integer lockedAmount;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public Integer getLockedAmount() {
        return lockedAmount;
    }

    public void setLockedAmount(Integer lockedAmount) {
        this.lockedAmount = lockedAmount;
    }
}
