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
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.dao.Frequency;
import org.openpreservation.fixity.apps.dao.ScanSchedule;
import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.openpreservation.fixity.core.digests.Algorithms;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Recurring scan schedules. A schedule is persisted and registered with Quartz, so it survives
 * a restart (re-registered on startup) and fires on its own.
 *
 * <p>Callers pick a friendly preset (frequency plus time of day); the cron expression is derived
 * from it. A raw Quartz cron may be supplied instead, for power users.
 */
@jakarta.ws.rs.Path("/api/schedules")
@Produces(MediaType.APPLICATION_JSON)
public class SchedulesResource {

    /** JSON create body. Either (frequency + time) or cron must be supplied. */
    public static final class ScheduleRequest {
        public @Nullable Long pathId;
        public @Nullable String frequency;      // HOURLY | DAILY | WEEKLY
        public int minute;                      // 0-59
        public int hour;                        // 0-23 (DAILY, WEEKLY)
        public int dayOfWeek = 1;               // 1-7, Sun-Sat (WEEKLY)
        public @Nullable String algorithm;      // e.g. "SHA-256"; defaults to the app default
        public @Nullable String cron;           // raw Quartz cron, overrides the preset
    }

    private final DataFactory dataFactory;

    public SchedulesResource(final DataFactory dataFactory) {
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public @NonNull List<@NonNull ScanSchedule> list() {
        return dataFactory.scanScheduleDAO().findAll();
    }

    @UnitOfWork
    @GET
    @jakarta.ws.rs.Path("/{id}/")
    public ScanSchedule get(@PathParam("id") final Long id) {
        try {
            return dataFactory.scanScheduleDAO().findById(id);
        } catch (final NoResultException e) {
            throw new NotFoundException("Schedule with id " + id + " not found.", e);
        }
    }

    @UnitOfWork
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ScanSchedule create(final ScheduleRequest request) {
        if (request == null || request.pathId == null) {
            throw new BadRequestException("A pathId is required.");
        }
        final CollectionPath path;
        try {
            path = dataFactory.collectionPathDAO().findById(request.pathId);
        } catch (final NoResultException e) {
            throw new NotFoundException("Path with id " + request.pathId + " not found.", e);
        }
        final Algorithms algorithm = parseAlgorithm(request.algorithm);

        final ScanSchedule schedule;
        if (request.cron != null && !request.cron.isBlank()) {
            schedule = ScanSchedule.ofCron(path, request.cron.trim(), algorithm);
        } else {
            final Frequency frequency = parseFrequency(request.frequency);
            try {
                schedule = ScanSchedule.of(path, frequency, request.minute, request.hour, request.dayOfWeek, algorithm);
            } catch (final IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }
        }

        final ScanSchedule saved = dataFactory.scanScheduleDAO().create(schedule);
        registerWithQuartz(saved);
        return saved;
    }

    @UnitOfWork
    @DELETE
    @jakarta.ws.rs.Path("/{id}/")
    public void delete(@PathParam("id") final Long id) {
        final ScanSchedule schedule;
        try {
            schedule = dataFactory.scanScheduleDAO().findById(id);
        } catch (final NoResultException e) {
            throw new NotFoundException("Schedule with id " + id + " not found.", e);
        }
        ScheduleManager.unscheduleQuietly(schedule.getJobName());
        dataFactory.scanScheduleDAO().delete(schedule);
    }

    private void registerWithQuartz(final ScanSchedule schedule) {
        try {
            ScheduleManager.scheduleRecurringScan(
                    schedule.getJobName(),
                    schedule.getCollectionPath().getFullPath(),
                    schedule.getAlgorithm().getName(),
                    schedule.toCron());
        } catch (final Exception e) {
            // The schedule is persisted; surface the scheduling failure rather than hide it.
            throw new BadRequestException("Could not register schedule with the scheduler: " + e.getMessage());
        }
    }

    private static Algorithms parseAlgorithm(final @Nullable String name) {
        if (name == null || name.isBlank()) {
            return Algorithms.DEFAULT;
        }
        try {
            return Algorithms.fromString(name.trim());
        } catch (final Exception e) {
            throw new BadRequestException("Unknown algorithm: " + name);
        }
    }

    private static Frequency parseFrequency(final @Nullable String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("A frequency (HOURLY, DAILY or WEEKLY) or a cron is required.");
        }
        try {
            return Frequency.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new BadRequestException("Unknown frequency: " + name + " (expected HOURLY, DAILY or WEEKLY)");
        }
    }
}
