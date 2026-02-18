package org.openpreservation.fixity.apps.server.resources.views;

import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.server.views.PathView;
import org.openpreservation.fixity.apps.server.views.PathsView;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/paths")
public class PathsResource {
    final DataFactory dataFactory;
    public PathsResource(DataFactory dataFactory) {
        super();
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public PathsView getPaths() {
        return new PathsView(dataFactory.collectionPathDAO().findAll());
    }

    @UnitOfWork
    @GET
    @Path("/{pathId}/")
    public PathView getPath(@PathParam("pathId") Long pathId) {
        return new PathView(dataFactory.collectionPathDAO().findById(pathId).orElseThrow(() -> new NotFoundException("CollectionPath with ID " + pathId + " not found.")));
    }

}
