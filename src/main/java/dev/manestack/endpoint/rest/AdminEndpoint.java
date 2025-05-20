package dev.manestack.endpoint.rest;

import dev.manestack.service.UserService;
import dev.manestack.service.user.Deposit;
import dev.manestack.service.user.Withdrawal;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.util.List;

@RolesAllowed({"ADMIN"})
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

    @GET
    @Path("/withdrawal")
    public Uni<List<Withdrawal>> fetchWithdrawals(@QueryParam("userId") Integer userId, @QueryParam("adminId") Integer adminId) {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.fetchWithdrawals(userId, adminId));
    }

    @PUT
    @Path("/withdrawal/approve")
    public Uni<Withdrawal> approveWithdrawal(@QueryParam("withdrawalId") Long withdrawalId) {
        return identity.getDeferredIdentity()
                .chain(identity -> userService.approveWithdrawal(Integer.parseInt(identity.getPrincipal().getName()), withdrawalId));
    }
}
