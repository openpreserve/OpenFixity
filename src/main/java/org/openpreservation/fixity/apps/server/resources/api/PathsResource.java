/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openpreservation.fixity.apps.server.resources.api;

import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.schedule.ScanJobDetails;
import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.openpreservation.fixity.apps.server.exceptions.OpenFixityException;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@jakarta.ws.rs.Path("/api/paths")
@Produces(MediaType.APPLICATION_JSON)
public class PathsResource {
    private final DataFactory dataFactory;
    public PathsResource(DataFactory dataFactory) {
        super();
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public @NonNull List<@NonNull CollectionPath> getPaths() {
        @NonNull List<@NonNull CollectionPath> collections = dataFactory.collectionPathDAO().findAll();
        return collections;
    }

    @UnitOfWork
    @GET
    @jakarta.ws.rs.Path("/{folderId}/")
    public CollectionPath getPath(@PathParam("folderId") final Long folderId) {
        if (folderId == null) {
            throw OpenFixityException.of(new BadRequestException("Path ID cannot be null."), "Path.id: " + folderId);
        }
        try {
            return dataFactory.collectionPathDAO().findById(folderId);
        } catch (Exception e) {
            throw OpenFixityException.of(new NotFoundException("Collection with ID " + folderId + " not found.", e), "Path.id" + folderId);
        }
    }

    @UnitOfWork
    @POST
    @jakarta.ws.rs.Path("/{folderId}/")
    public CollectionPath createPath(@PathParam("folderId") final int folderId) {
        Path folder = FolderInfoResource.getPathById(folderId);
        try {
            dataFactory.collectionPathDAO().findByRoot(folder);
            throw OpenFixityException
                    .of(new BadRequestException("Folder with path " + folder.toAbsolutePath().toString() + " already exists."),
                        "Path.root" + folder.toAbsolutePath().toString());
        } catch (NoResultException e) {
            // Expected, continue with creation
        }
        try {
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
        if (pathId == null) {
            throw OpenFixityException.of(new BadRequestException("Path ID cannot be null."), "Path.id: " + pathId);
        }
        try {
            CollectionPath collectionPath = dataFactory.collectionPathDAO().findById(pathId);
            ScanJobDetails jobDetails = ScanJobDetails.of(collectionPath.getJobId(),
                                                        "User",
                                                        "",
                                                        collectionPath.getFullPath(),
                                                        algorithm);
            return ScheduleManager.scheduleScan(jobDetails);
        } catch (NoResultException e) {
            throw OpenFixityException.of(new NotFoundException("CollectionPath with ID " + pathId + " not found.", e), "Path.id" + pathId);
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to schedule scan job: " + e.getMessage(), e);
        }
    }
}
