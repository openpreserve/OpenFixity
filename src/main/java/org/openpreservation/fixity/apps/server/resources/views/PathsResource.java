package org.openpreservation.fixity.apps.server.resources.views;

import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.server.exceptions.OpenFixityException;
import org.openpreservation.fixity.apps.server.views.PathView;
import org.openpreservation.fixity.apps.server.views.PathsView;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.BadRequestException;
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

    @SuppressWarnings("null")
    @UnitOfWork
    @GET
    public PathsView getPaths() {
        return new PathsView(dataFactory.collectionPathDAO().findAll());
    }

    @UnitOfWork
    @GET
    @Path("/{pathId}/")
    public PathView getPath(@PathParam("pathId") Long pathId) {
        if (pathId == null) {
            throw OpenFixityException.of(new BadRequestException("Path ID cannot be null."), "Path.id: " + pathId);
        }
        try {
            return new PathView(dataFactory.collectionPathDAO().findById(pathId));
        } catch (NoResultException e) {
            throw OpenFixityException.of(new NotFoundException("CollectionPath with ID " + pathId + " not found.", e), "Path.id" + pathId);
        }
    }

}
