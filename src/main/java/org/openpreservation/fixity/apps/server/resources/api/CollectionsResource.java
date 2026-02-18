package org.openpreservation.fixity.apps.server.resources.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.Collection;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.dao.PathRegistration;
import org.openpreservation.fixity.apps.schedule.ScanJobDetails;
import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.openpreservation.fixity.apps.server.exceptions.OpenFixityException;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@jakarta.ws.rs.Path("/api/collections")
public class CollectionsResource {
    private static final Logger logger = LoggerFactory.getLogger(CollectionsResource.class);
    private final DataFactory dataFactory;
    /**
     * Constructor for the CollectionsResource REST api resource.
     * @param dataFactory DataFactory instance to use for database access.
     */
    public CollectionsResource(DataFactory dataFactory) {
        super();
        this.dataFactory = dataFactory;
    }

    /**
     * Return a list of all Collections.
     * @return a java.util.List of Collections.
     */
    @UnitOfWork
    @GET
    @Produces("application/json")
    public List<Collection> getCollections() {
        List<Collection> collections = dataFactory.collectionDAO().findAll();
        return collections;
    }

    /**
     * Return the Collection with the given name, or a 404 Not Found if it does not exist.
     *
     * @param name the name of the Collection to return.
     * @return the Collection with the given name.
     * @throws OpenFixityException if the Collection with the given name does not exist, or if there is an error accessing the database.
     */
    @UnitOfWork
    @GET
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{name}/")
    public Collection getCollection(@PathParam("name") final String name) throws OpenFixityException {
        return dataFactory
                .collectionDAO()
                .findByName(name)
                .orElseThrow(() -> OpenFixityException
                        .of(new NotFoundException("Collection with name " + name + " not found."),
                        "collections/name/" + name));
    }

    /**
     * Create a new Collection with the given name, or return a 400 Bad Request if a Collection with the given name already exists.
     *
     * @param name the name of the Collection to create.
     * @return the newly created Collection.
     * @throws OpenFixityException if a Collection with the given name already exists, or if there is an error accessing the database.
     */
    @UnitOfWork
    @POST
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{name}/")
    public Collection createCollection(@PathParam("name") final String name) {
        try {
            return dataFactory.collectionDAO().create(name);
        } catch (SQLIntegrityConstraintViolationException e) {
            final String message = "Collection with name " + name + " already exists.";
            logger.error(String.format("/collections/name/{} {}", name, message), e);
            throw OpenFixityException.of(new BadRequestException(message, e), "/collections/name/" + name);
        }
    }

    /**
     * Update a Collection with the given name, or return a 400 Bad Request if a Collection with the given name already exists.
     *
     * @param name the name of the Collection to create.
     * @return the newly created Collection.
     * @throws OpenFixityException if a Collection with the given name already exists, or if there is an error accessing the database.
     */
    @UnitOfWork
    @PUT
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{name}/")
    public Collection updateCollection(@PathParam("name") final String name) {
        try {
            return dataFactory.collectionDAO().create(name);
        } catch (SQLIntegrityConstraintViolationException e) {
            final String message = "Collection with name " + name + " already exists.";
            logger.error(String.format("/collections/name/{} {}", name, message), e);
            throw OpenFixityException.of(new BadRequestException(message, e), "/collections/name/" + name);
        }
    }

    /**
     * 
     * @param name
     * @return
     */
    @UnitOfWork
    @POST
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{name}/scan")
    public Set<JobDetail> scanCollection(@PathParam("name") final String name) {
        Collection collection = getCollection(name);
        Set<JobDetail> jobDetailsSet = new HashSet<>();
        for (CollectionPath path : collection.getPathRegistrations().stream().filter(pr -> pr.getDeregisteredAt() == null).map(PathRegistration::getCollectionPath).toList()) {
            jobDetailsSet.add(this.scanPath(name, path.getId()));
        }
        return jobDetailsSet;
    }

    @UnitOfWork
    @POST
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{name}/folder/{folderId}/")
    public Collection registerFolder(@PathParam("name") final String name, @PathParam("folderId") final Integer folderId) {
        Collection collection = getCollection(name);
        Path toRegister = FoldersResource.getPathById(folderId);
        if (toRegister == null) throw new NotFoundException("Folder with ID " + folderId + " not found.");
        if (!Files.exists(toRegister) || !Files.isDirectory(toRegister) || !Files.isReadable(toRegister)) {
            throw new BadRequestException(String.format("Path %s to register must be an existing, readable directory.", toRegister.toString()));
        }
        try {
            checkPathRegistrationNotExists(collection.getPathRegistrations(), toRegister);
            Optional<CollectionPath> collectionPathOpt = dataFactory.collectionPathDAO().findByRoot(toRegister);
            CollectionPath collectionPath = (collectionPathOpt.isEmpty()) ? 
                    dataFactory.collectionPathDAO().create(CollectionPath.of(toRegister)) : collectionPathOpt.get();
            dataFactory.pathRegistrationDAO().register(collection, collectionPath);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw OpenFixityException.
                    of(new BadRequestException("Path is already registered to the Collection.",e),
                            "/collections/name/" + name + "/folder/" + folderId);
        } catch (IOException e) {
            throw OpenFixityException.
                    of(new InternalServerErrorException("Failed to register path.", e),
                            "/collections/name/" + name + "/folder/" + folderId);
        }
        return collection;
    }

    @UnitOfWork
    @DELETE
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{name}/paths/{folderId}/")
    public PathRegistration deregisterFolder(@PathParam("name") final String name, @PathParam("folderId") final Long folderId) {
        Collection collection = getCollection(name);
        CollectionPath collectionPath = dataFactory
                .collectionPathDAO()
                .findById(folderId)
                .orElseThrow(() -> 
                    OpenFixityException
                        .of(new NotFoundException("CollectionPath with ID " + folderId + " not found.")));
        try {
            return dataFactory.pathRegistrationDAO().deregister(collection, collectionPath);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw OpenFixityException.
                    of(new BadRequestException("No Path registered for that collections.",e),
                            "/collections/name/" + name + "/folder/" + folderId);
        }

    }

    private void checkPathRegistrationNotExists(Set<@NonNull PathRegistration> registrations, Path toCheck) throws SQLIntegrityConstraintViolationException, IOException {
        if ((registrations == null) || (registrations.isEmpty())) {
            return;
        }
        for (PathRegistration pr : registrations) {
            if (pr.getDeregisteredAt() == null && Files.exists(pr.getCollectionPath().getRoot()) && Files.isSameFile(toCheck, pr.getCollectionPath().getRoot())) {
                throw new SQLIntegrityConstraintViolationException("Path is already registered to the Collection.");
            }
        }
    }

    @UnitOfWork
    @POST
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{name}/paths/{pathId}/scan")
    public JobDetail scanPath(@PathParam("name") final String name, @PathParam("pathId") final Long pathId) {
        CollectionPath collectionPath = dataFactory.collectionPathDAO().findById(pathId).orElseThrow(() -> new NotFoundException("CollectionPath with ID " + pathId + " not found."));
        Collection collection = dataFactory.collectionDAO().findByName(name).orElseThrow(() -> new NotFoundException("Collection with name " + name + " not found."));
        ScanJobDetails jobDetails = ScanJobDetails.of(collectionPath.getJobId(),
                                                      collection.getJobId(),
                                                      "",
                                                      collectionPath.getFullPath(),
                                                      "SHA-256");
        try {
            return ScheduleManager.scheduleScan(jobDetails);
        } catch (SchedulerException e) {
            throw OpenFixityException.of(new BadRequestException("Scheduling exception for collection " + name + ", path" + pathId, e));
        }
    }
}
