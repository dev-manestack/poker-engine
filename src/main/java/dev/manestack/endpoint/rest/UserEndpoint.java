package dev.manestack.endpoint.rest;

import dev.manestack.service.UserService;
import dev.manestack.service.user.User;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.util.List;

@Path("/api/v1/user")
public class UserEndpoint {
    @Inject
    UserService userService;

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

    @GET
    @Path("/search")
    public Uni<List<User>> searchUsers(@QueryParam("username") @DefaultValue("") String username) {
        return userService.searchUsers(username);
    }
}
