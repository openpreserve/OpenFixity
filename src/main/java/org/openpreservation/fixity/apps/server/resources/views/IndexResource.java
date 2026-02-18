package org.openpreservation.fixity.apps.server.resources.views;

import java.net.URI;

import org.openpreservation.fixity.apps.server.views.AboutView;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
public class IndexResource {
    @GET
    public Response getIndex() {
        return Response.seeOther(URI.create("/collections")).build();
    }
    @GET
    @Path("/about/")
    public AboutView about() {
        return new AboutView();
    }
}
