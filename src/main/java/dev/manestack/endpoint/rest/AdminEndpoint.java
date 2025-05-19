package dev.manestack.endpoint.rest;

import dev.manestack.service.UserService;
import dev.manestack.service.user.Deposit;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;

@Authenticated
@Path("/api/v1/admin")
public class AdminEndpoint {
    @Inject
    CurrentIdentityAssociation identity;
    @Inject
    UserService userService;

    @GET
    @Path("/deposit")
    public Uni<List<Deposit>> fetchDeposits(@QueryParam("userId") Integer userId, @QueryParam("adminId") Integer adminId) {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.fetchDeposits(userId, adminId));
    }

    @POST
    @Path("/deposit")
    public Uni<Deposit> createDeposit(Deposit deposit) {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.createDeposit(Integer.parseInt(identity.getPrincipal().getName()), deposit));
    }
}
