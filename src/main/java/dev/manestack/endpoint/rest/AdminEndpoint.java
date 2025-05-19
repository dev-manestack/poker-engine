package dev.manestack.endpoint.rest;

import dev.manestack.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/api/v1/admin")
public class AdminEndpoint {
    @Inject
    UserService userService;
}
