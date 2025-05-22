package dev.manestack.endpoint.rest;

import dev.manestack.service.GameService;
import dev.manestack.service.UserService;
import dev.manestack.service.poker.table.GameTable;
import dev.manestack.service.user.Deposit;
import dev.manestack.service.user.User;
import dev.manestack.service.user.Withdrawal;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.util.List;

@Path("/api/v1/user")
public class UserEndpoint {
    @Inject
    CurrentIdentityAssociation identity;
    @Inject
    UserService userService;
    @Inject
    GameService gameService;

    @POST
    @Path("/login")
    public Uni<JsonObject> loginUser(JsonObject jsonObject) {
        String email = jsonObject.getString("email");
        String password = jsonObject.getString("password");
        if (email == null || password == null) {
            throw new BadRequestException("Email and password are required");
        }
        return userService.loginUser(email, password)
                .map(token -> {
                    JsonObject response = new JsonObject();
                    response.put("token", token);
                    return response;
                });
    }

    @POST
    @Path("/register")
    public Uni<JsonObject> registerUser(User user) {
        return userService.registerUser(user)
                .map(token -> {
                    JsonObject response = new JsonObject();
                    response.put("token", token);
                    return response;
                });
    }

    @Authenticated
    @GET
    @Path("/me")
    public Uni<User> fetchUserFromToken() {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.fetchUser(Integer.parseInt(identity.getPrincipal().getName())))
                .chain(user -> userService.fetchUserBalance(user.getUserId())
                        .map(userBalance -> {
                            user.setUserBalance(userBalance);
                            return user;
                        }));
    }

    @GET
    @Path("/search")
    public Uni<List<User>> searchUsers(@QueryParam("username") @DefaultValue("") String username) {
        return userService.searchUsers(username, false);
    }

    @GET
    @Path("/table")
    public Uni<List<GameTable>> fetchTables() {
        return identity.getDeferredIdentity()
                .chain(identity -> gameService.fetchTables());
    }

    @Authenticated
    @GET
    @Path("/deposit")
    public Uni<List<Deposit>> fetchMyDeposits() {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.fetchDeposits(Integer.parseInt(identity.getPrincipal().getName())));
    }

    @Authenticated
    @GET
    @Path("/withdrawal")
    public Uni<List<Withdrawal>> fetchMyWithdrawals() {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.fetchWithdrawals(Integer.parseInt(identity.getPrincipal().getName())));
    }

    @Authenticated
    @POST
    @Path("/withdrawal")
    public Uni<Withdrawal> createWithdrawal(Withdrawal withdrawal) {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.createWithdrawal(Integer.parseInt(identity.getPrincipal().getName()), withdrawal));
    }
}
