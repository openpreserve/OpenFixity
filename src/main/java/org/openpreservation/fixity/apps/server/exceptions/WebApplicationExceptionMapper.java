package org.openpreservation.fixity.apps.server.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception) {
        OpenFixityException openFixityException = OpenFixityException.ofNotAllowed(exception);
        return Response.status(openFixityException.getResponse().getStatus())
                .entity(openFixityException.getDetails())
                .type("application/problem+json")
                .build();
    }
}
