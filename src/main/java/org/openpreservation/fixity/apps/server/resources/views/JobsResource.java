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
package org.openpreservation.fixity.apps.server.resources.views;

import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.openpreservation.fixity.apps.server.views.JobsView;
import org.quartz.SchedulerException;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;

@Path("/jobs")
public class JobsResource {
    private final DataFactory dataFactory;

    public JobsResource(final DataFactory dataFactory) {
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public JobsView getSchedules() {
        try {
            return new JobsView(ScheduleManager.getScheduledJobKeys(),
                    dataFactory.scanScheduleDAO().findAll());
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to read scheduled jobs: " + e.getMessage(), e);
        }
    }
}
