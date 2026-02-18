package org.openpreservation.fixity.apps.server.exceptions;

import org.openpreservation.fixity.apps.server.exceptions.RFC7807Details.RFC7807DetailsBuilder;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public final class OpenFixityException extends WebApplicationException {
    private static final String TYPE_ROOT = "https://openpreservation.org/fixitypro/errors/";
    private static final long serialVersionUID = 1L;
    private final RFC7807Details details;

    public RFC7807Details getDetails() {
        return details;
    }

    private OpenFixityException(final RFC7807Details details, final Response.Status status) {
        super(details.getTitle(), status);
        this.details = details;
    }

    protected OpenFixityException(final RFC7807Details details, final WebApplicationException cause) {
        super(details.getTitle(), cause, cause.getResponse().getStatusInfo().toEnum());
        this.details = details;
    }

    protected OpenFixityException(final RFC7807Details details, final Response.Status status, final Throwable cause) {
        super(details.getTitle(), cause, status);
        this.details = details;
    }

    public static OpenFixityException of(final RFC7807Details details, final Response.Status status) {
        return new OpenFixityException(details, status);
    }

    public static OpenFixityException of(final NotFoundException cause, final String instance) {
        return new OpenFixityException(builderOf(cause, "not-found", instance).build(), cause);
    }

    public static OpenFixityException of(final BadRequestException cause, final String instance) {
        return new OpenFixityException(builderOf(cause, "bad-request", instance).build(), cause);
    }

    public static OpenFixityException of(final InternalServerErrorException cause, final String instance) {
        return new OpenFixityException(builderOf(cause, "internal-server-error", instance).build(), cause);
    }

    public static OpenFixityException ofNotAllowed(final WebApplicationException cause) {
        return new OpenFixityException(builderOf(cause, "method-not-allowed", "no-instance").build(), cause);
    }

    public static OpenFixityException of(final Throwable cause) {
        return new OpenFixityException(builderOf(Status.INTERNAL_SERVER_ERROR, "internal-server-error")
                .withDetail(cause.getMessage())
                .withTraceId(cause.getStackTrace()[0].toString()).build(), Status.INTERNAL_SERVER_ERROR, cause);
    }

    private static RFC7807DetailsBuilder builderOf(final WebApplicationException cause, final String type, final String instance) {
         return builderOf(cause.getResponse().getStatusInfo().toEnum(), type)
                .withDetail(cause.getMessage())
                .withInstance(instance);
    }

    private static RFC7807DetailsBuilder builderOf(final Response.Status status, final String type) {
         return RFC7807DetailsBuilder.builder()
                .withType(TYPE_ROOT.concat(type))
                .withStatus(status.getStatusCode())
                .withTitle(status.getReasonPhrase());
    }
}
