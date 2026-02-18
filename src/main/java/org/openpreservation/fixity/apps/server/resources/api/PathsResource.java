package org.openpreservation.fixity.apps.server.resources.api;

import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.schedule.ScanJobDetails;
import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;

@jakarta.ws.rs.Path("/api/paths")
public class PathsResource {
    private final DataFactory dataFactory;
    public PathsResource(DataFactory dataFactory) {
        super();
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public List<CollectionPath> getPaths() {
        List<CollectionPath> collections = dataFactory.collectionPathDAO().findAll();
        return collections;
    }

    @UnitOfWork
    @GET
    @jakarta.ws.rs.Path("/{folderId}/")
    public CollectionPath getPath(@PathParam("folderId") final Long folderId) {
        return dataFactory.collectionPathDAO().findById(folderId).orElseThrow(() -> new NotFoundException("Collection with ID " + folderId + " not found."));
    }

    @UnitOfWork
    @POST
    @jakarta.ws.rs.Path("/{folderId}/")
    public CollectionPath createPath(@PathParam("folderId") final int folderId) {
        Path folder = FoldersResource.getPathById(folderId);
        try {
            if (folder == null) throw new NotFoundException("Folder with ID " + folderId + " not found.");
            CollectionPath existingPath = dataFactory.collectionPathDAO().findByRoot(folder).orElse(null);
            if (existingPath != null) throw new SQLIntegrityConstraintViolationException("Folder with path " + folder.toAbsolutePath().toString() + " already exists.");
            return dataFactory.collectionPathDAO().create(CollectionPath.of(folder));
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BadRequestException("Folder with path " + folder.toAbsolutePath().toString() + " already exists.");
        }
    }

    @UnitOfWork
    @POST
    @jakarta.ws.rs.Path("/{pathId}/scan/SHA-512/{algorithm}/")
    public JobDetail scanSha512Path(@PathParam("pathId") final Long pathId, @Encoded @PathParam("algorithm") final String algorithm) {
        final String fixedAlgorithm = "SHA-512/" + algorithm;
        return scanPath(pathId, fixedAlgorithm); 
    }

    @UnitOfWork
    @POST
    @jakarta.ws.rs.Path("/{pathId}/scan/{algorithm}/")
    public JobDetail scanPath(@PathParam("pathId") final Long pathId, @Encoded @PathParam("algorithm") final String algorithm) {
        CollectionPath collectionPath = dataFactory.collectionPathDAO().findById(pathId).orElseThrow(() -> new NotFoundException("CollectionPath with ID " + pathId + " not found."));
        ScanJobDetails jobDetails = ScanJobDetails.of(collectionPath.getJobId(),
                                                      "User",
                                                      "",
                                                      collectionPath.getFullPath(),
                                                      algorithm);
        try {
            return ScheduleManager.scheduleScan(jobDetails);
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to schedule scan job: " + e.getMessage(), e);
        }
    }
}
