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

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.server.exceptions.OpenFixityException;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * JSON scan API, the counterpart to the Mustache scan views under resources/views. Carl built
 * the HTML side; the React frontend needs the same data as JSON.
 */
@jakarta.ws.rs.Path("/api/scans")
@Produces(MediaType.APPLICATION_JSON)
public class ScansResource {
    private final DataFactory dataFactory;

    public ScansResource(final DataFactory dataFactory) {
        super();
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public @NonNull List<@NonNull PathScan> getScans() {
        return dataFactory.pathScanDAO().findAll();
    }

    @UnitOfWork
    @GET
    @jakarta.ws.rs.Path("/{scanId}/")
    public PathScan getScan(@PathParam("scanId") final Long scanId) {
        if (scanId == null) {
            throw OpenFixityException.of(new BadRequestException("PathScan ID cannot be null."), "PathScan.id: " + scanId);
        }
        try {
            return dataFactory.pathScanDAO().findById(scanId);
        } catch (final NoResultException e) {
            throw OpenFixityException.of(new NotFoundException("PathScan with ID " + scanId + " not found.", e), "PathScan.id: " + scanId);
        }
    }
}
