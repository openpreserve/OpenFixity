package org.openpreservation.fixity.apps.server.resources.views;

import java.sql.SQLIntegrityConstraintViolationException;

import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.server.exceptions.OpenFixityException;
import org.openpreservation.fixity.apps.server.views.CollectionView;
import org.openpreservation.fixity.apps.server.views.CollectionsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/collections")
public class CollectionsResource {
    private static final Logger logger = LoggerFactory.getLogger(CollectionsResource.class);
    final DataFactory dataFactory;
    public CollectionsResource(DataFactory dataFactory) {
        super();
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public CollectionsView getCollections() {
        logger.debug("Getting all collections");
        return new CollectionsView(dataFactory.collectionDAO().findAll());
    }

    @UnitOfWork
    @GET
    @Path("/{name}/")
    public CollectionView getCollection(@PathParam("name") final String name) {
        logger.debug("Getting collection {}", name);
        return new CollectionView(dataFactory.collectionDAO().
                findByName(name).
                orElseThrow(() -> OpenFixityException
                        .of(new NotFoundException("Collection with name " + name + " not found."),
                                                  "/collections/name/" + name)));
    }

    @UnitOfWork
    @POST
    @Path("/{name}/")
    public CollectionView createCollection(@PathParam("name") final String name) {
        logger.debug("Creating collection {}", name);
        try {
            return new CollectionView(dataFactory.collectionDAO().create(name));
        } catch (SQLIntegrityConstraintViolationException e) {
            final String message = "Collection with name " + name + " already exists.";
            logger.error(String.format("/collections/name/{} {}", name, message), e);
            throw OpenFixityException.of(new BadRequestException("Collection with name " + name + " already exists."), "/collections/name/" + name);
        }
    }

    @UnitOfWork
    @POST
    @Path("/{name}/folder/{folderId}/")
    public CollectionView registerFolder(@PathParam("name") final String name, @PathParam("folderId") final Long folderId) {
        return getCollection(name);
    }
}
