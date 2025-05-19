package dev.manestack.endpoint;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Provider
public class ExceptionMappers {
    private static final Logger LOG = Logger.getLogger("VoiceChannelController");

    @ServerExceptionMapper
    public RestResponse<JsonObject> mapException(Throwable throwable) {
        Response.Status status;
        String message = throwable.getMessage();
        if (throwable instanceof NotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (throwable instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        LOG.debugv(throwable, "ERROR: {0}", throwable.getMessage());
        if (throwable.getCause() != null) {
            message = message + " - " + throwable.getCause().getMessage();
        }
        return RestResponse.status(status, new JsonObject()
                .put("status", "FAILED")
                .put("errorCode", status.getStatusCode())
                .put("errorMessage", message)
        );
    }
}
