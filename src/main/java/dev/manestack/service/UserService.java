package dev.manestack.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dev.manestack.service.user.User;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.postgresql.util.PSQLException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.manestack.jooq.generated.Tables.POKER_USER;

@ApplicationScoped
public class UserService {
    private static final Logger LOG = Logger.getLogger(UserService.class);

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

    public Uni<List<User>> searchUsers(String username) {
        return Uni.createFrom().voidItem()
                .map(unused -> {
                    List<User> userList = new ArrayList<>();
                    context.selectFrom(POKER_USER)
                            .where(POKER_USER.USERNAME.likeIgnoreCase("%" + username + "%"))
                            .fetchInto(User.class)
                            .forEach(user -> {
                                // TODO: Don't set bank and accont number null if user is admin.
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
}
