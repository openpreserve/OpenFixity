package org.openpreservation.fixity.apps.server.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class OpenFixityExceptionMapper implements ExceptionMapper<OpenFixityException> {
    @Override
    public Response toResponse(OpenFixityException exception) {
        return Response.status(exception.getResponse().getStatus())
                .entity(exception.getDetails())
                .type("application/problem+json")
                .build();
    }
}
