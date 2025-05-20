package dev.manestack.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dev.manestack.jooq.generated.tables.records.PokerDepositRecord;
import dev.manestack.jooq.generated.tables.records.PokerWithdrawalRecord;
import dev.manestack.service.user.Deposit;
import dev.manestack.service.user.User;
import dev.manestack.service.user.UserBalance;
import dev.manestack.service.user.Withdrawal;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.postgresql.util.PSQLException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.manestack.jooq.generated.Tables.*;

@ApplicationScoped
public class UserService {
    private static final Logger LOG = Logger.getLogger(UserService.class);
    private final ExecutorService QUERY_THREADS = Executors.newFixedThreadPool(3);

    @Inject
    DSLContext context;

    @ConfigProperty(name = "dev.manestack.jwt.issuer", defaultValue = "https://manestack.dev")
    String jwtIssuer;

    private String generateJWT(Integer userId, String role) {
        return Jwt.issuer(jwtIssuer)
                .upn(String.valueOf(userId))
                .groups(role)
                .expiresIn(Duration.ofHours(24))
                .subject(String.valueOf(userId))
                .sign();
    }

    public Uni<User> fetchUser(Integer userId) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> context.selectFrom(POKER_USER)
                        .where(POKER_USER.USER_ID.eq(userId))
                        .fetchOneInto(User.class))
                .onItem().transform(user -> {
                    if (user != null) {
                        user.setPassword(null);
                        user.setBankName(null);
                        user.setAccountNumber(null);
                    }
                    return user;
                });
    }

    public Uni<List<User>> searchUsers(String username) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    List<User> userList = new ArrayList<>();
                    context.selectFrom(POKER_USER)
                            .where(POKER_USER.USERNAME.likeIgnoreCase("%" + username + "%"))
                            .fetchInto(User.class)
                            .forEach(user -> {
                                // TODO: Don't set bank and account number null if user is admin.
                                user.setPassword(null);
                                user.setBankName(null);
                                user.setAccountNumber(null);
                                userList.add(user);
                            });
                    return userList;
                });
    }

    public Uni<String> registerUser(User user) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .invoke(user::validateRegister)
                .map(unused -> {
                    try {
                        String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());
                        Integer userId = context.insertInto(POKER_USER)
                                .set(POKER_USER.EMAIL, user.getEmail())
                                .set(POKER_USER.USERNAME, user.getUsername())
                                .set(POKER_USER.PASSWORD, hashedPassword)
                                .set(POKER_USER.BANK_NAME, user.getBankName())
                                .set(POKER_USER.ACCOUNT_NUMBER, user.getAccountNumber())
                                .set(POKER_USER.ROLE, User.Role.USER.name())
                                .returning(POKER_USER.USER_ID)
                                .fetchOne(POKER_USER.USER_ID);
                        return generateJWT(userId, User.Role.USER.name());
                    } catch (IntegrityConstraintViolationException integrityException) {
                        if (integrityException.getCause() instanceof PSQLException psqlException) {
                            if (psqlException.getServerErrorMessage() != null &&
                                    psqlException.getServerErrorMessage().getConstraint() != null) {
                                String constraint = psqlException.getServerErrorMessage().getConstraint();
                                switch (constraint) {
                                    case "unique_email": {
                                        throw new RuntimeException("Email already exists");
                                    }
                                    case "unique_username": {
                                        throw new RuntimeException("Username already exists");
                                    }
                                    default: {
                                        throw new RuntimeException("Failed to create user");
                                    }
                                }
                            }
                        }
                        throw new RuntimeException("Failed to create user");
                    }
                });
    }

    public Uni<String> loginUser(String email, String password) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    User user = context.selectFrom(POKER_USER)
                            .where(POKER_USER.EMAIL.eq(email))
                            .fetchOneInto(User.class);
                    if (user != null) {
                        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
                        if (!result.verified) {
                            throw new RuntimeException("Invalid username or password");
                        }
                    } else {
                        throw new RuntimeException("User not found");
                    }
                    return generateJWT(user.getUserId(), user.getRole().name());
                });
    }

    /*
     * Details Methods
     */
    public Uni<List<Deposit>> fetchDeposits(Integer userId, Integer adminId) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    List<Deposit> deposits = new ArrayList<>();
                    if (userId != null) {
                        context.selectFrom(POKER_DEPOSIT)
                                .where(POKER_DEPOSIT.USER_ID.eq(userId))
                                .fetch()
                                .forEach(record -> {
                                    Deposit deposit = new Deposit(record);
                                    deposits.add(deposit);
                                });
                    } else if (adminId != null) {
                        context.selectFrom(POKER_DEPOSIT)
                                .where(POKER_DEPOSIT.ADMIN_ID.eq(adminId))
                                .fetch()
                                .forEach(record -> {
                                    Deposit deposit = new Deposit(record);
                                    deposits.add(deposit);
                                });
                    } else {
                        context.selectFrom(POKER_DEPOSIT)
                                .fetch()
                                .forEach(record -> {
                                    Deposit deposit = new Deposit(record);
                                    deposits.add(deposit);
                                });
                    }
                    return deposits;
                });
    }

    public Uni<Deposit> createDeposit(Integer adminId, Deposit deposit) {
        return fetchUser(adminId)
                .emitOn(QUERY_THREADS)
                .chain(adminUser -> {
                    deposit.validate();
                    try {
                        PokerDepositRecord record = context.insertInto(POKER_DEPOSIT)
                                .set(POKER_DEPOSIT.USER_ID, deposit.getUserId())
                                .set(POKER_DEPOSIT.ADMIN_ID, adminId)
                                .set(POKER_DEPOSIT.AMOUNT, deposit.getAmount())
                                .set(POKER_DEPOSIT.TYPE, deposit.getType().name())
                                .set(POKER_DEPOSIT.CREATE_DATE, OffsetDateTime.now())
                                .set(POKER_DEPOSIT.DETAILS, JSONB.valueOf(deposit.getDetails().encode()))
                                .returning()
                                .fetchOne();
                        if (record != null) {
                            return incrementBalance(deposit.getUserId(), deposit.getAmount())
                                    .replaceWith(new Deposit(record));
                        } else {
                            throw new RuntimeException("Failed to create deposit");
                        }
                    } catch (IntegrityConstraintViolationException integrityException) {
                        if (integrityException.getCause() instanceof PSQLException psqlException) {
                            if (psqlException.getServerErrorMessage() != null &&
                                    psqlException.getServerErrorMessage().getConstraint() != null) {
                                String constraint = psqlException.getServerErrorMessage().getConstraint();
                                if (constraint.equals("poker_deposit_user_id_fkey")) {
                                    throw new RuntimeException("User not found");
                                }
                                throw new RuntimeException("Failed to create deposit");
                            }
                        }
                        throw new RuntimeException("Failed to create deposit");
                    }
                });
    }

    public Uni<Withdrawal> fetchWithdrawalById(Long withdrawalId) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    PokerWithdrawalRecord record = context.selectFrom(POKER_WITHDRAWAL)
                            .where(POKER_WITHDRAWAL.WITHDRAWAL_ID.eq(withdrawalId))
                            .fetchOne();
                    if (record != null) {
                        return new Withdrawal(record);
                    } else {
                        throw new RuntimeException("Withdrawal not found");
                    }
                });
    }

    public Uni<List<Withdrawal>> fetchWithdrawals(Integer userId, Integer adminId) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    List<Withdrawal> withdrawals = new ArrayList<>();
                    if (userId != null) {
                        context.selectFrom(POKER_WITHDRAWAL)
                                .where(POKER_WITHDRAWAL.USER_ID.eq(userId))
                                .fetch()
                                .forEach(record -> {
                                    Withdrawal withdrawal = new Withdrawal(record);
                                    withdrawals.add(withdrawal);
                                });
                    } else if (adminId != null) {
                        context.selectFrom(POKER_WITHDRAWAL)
                                .where(POKER_WITHDRAWAL.APPROVED_BY.eq(adminId))
                                .fetch()
                                .forEach(record -> {
                                    Withdrawal withdrawal = new Withdrawal(record);
                                    withdrawals.add(withdrawal);
                                });
                    } else {
                        context.selectFrom(POKER_WITHDRAWAL)
                                .fetch()
                                .forEach(record -> {
                                    Withdrawal withdrawal = new Withdrawal(record);
                                    withdrawals.add(withdrawal);
                                });
                    }
                    return withdrawals;
                });
    }

    public Uni<Withdrawal> createWithdrawal(Integer userId, Withdrawal withdrawal) {
        return fetchUserBalance(userId)
                .emitOn(QUERY_THREADS)
                .map(userBalance -> {
                    withdrawal.validate();
                    if (withdrawal.getAmount() > userBalance.getBalance()) {
                        throw new RuntimeException("Insufficient balance");
                    }
                    PokerWithdrawalRecord record = context.insertInto(POKER_WITHDRAWAL)
                            .set(POKER_WITHDRAWAL.AMOUNT, withdrawal.getAmount())
                            .set(POKER_WITHDRAWAL.USER_ID, userId)
                            .set(POKER_WITHDRAWAL.CREATE_DATE, OffsetDateTime.now())
                            .set(POKER_WITHDRAWAL.DETAILS, JSONB.valueOf(withdrawal.getDetails().encode()))
                            .returning()
                            .fetchOne();
                    if (record != null) {
                        return new Withdrawal(record);
                    } else {
                        throw new RuntimeException("Failed to create withdrawal");
                    }
                });
    }

    public Uni<Withdrawal> approveWithdrawal(Integer agentId, Long withdrawalId) {
        return fetchWithdrawalById(withdrawalId)
                .map(withdrawal -> {
                    if (withdrawal.getApprovedBy() != null) {
                        throw new RuntimeException("Withdrawal already approved");
                    }
                    if (withdrawal.getAmount() <= 0) {
                        throw new RuntimeException("Invalid withdrawal amount");
                    }
                    return context.update(POKER_WITHDRAWAL)
                            .set(POKER_WITHDRAWAL.APPROVED_BY, agentId)
                            .set(POKER_WITHDRAWAL.APPROVE_DATE, OffsetDateTime.now())
                            .where(POKER_WITHDRAWAL.WITHDRAWAL_ID.eq(withdrawalId))
                            .returning()
                            .fetchOneInto(Withdrawal.class);
                })
                .call(withdrawal -> incrementBalance(withdrawal.getUserId(), -withdrawal.getAmount()));
    }

    public Uni<UserBalance> fetchUserBalance(Integer userId) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> context.selectFrom(POKER_USER_BALANCE)
                        .where(POKER_USER_BALANCE.USER_ID.eq(userId))
                        .fetchOneInto(UserBalance.class))
                .onItem().transform(userBalance -> {
                    if (userBalance != null) {
                        return userBalance;
                    } else {
                        UserBalance newUserBalance = new UserBalance();
                        newUserBalance.setUserId(userId);
                        newUserBalance.setBalance(0);
                        newUserBalance.setLockedAmount(0);
                        return newUserBalance;
                    }
                });
    }

    private Uni<UserBalance> incrementBalance(Integer userId, int amount) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    UserBalance userBalance = context.update(POKER_USER_BALANCE)
                            .set(POKER_USER_BALANCE.BALANCE, POKER_USER_BALANCE.BALANCE.add(amount))
                            .where(POKER_USER_BALANCE.USER_ID.eq(userId))
                            .returning()
                            .fetchOneInto(UserBalance.class);
                    if (userBalance != null) {
                        return userBalance;
                    } else {
                        UserBalance newUserBalance = new UserBalance();
                        newUserBalance.setUserId(userId);
                        newUserBalance.setBalance(amount);
                        newUserBalance.setLockedAmount(0);
                        context.insertInto(POKER_USER_BALANCE)
                                .set(POKER_USER_BALANCE.USER_ID, userId)
                                .set(POKER_USER_BALANCE.BALANCE, amount)
                                .execute();
                        return newUserBalance;
                    }
                });
    }
}
