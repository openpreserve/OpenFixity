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

import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.quartz.SchedulerException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// Produce JSON so boolean results carry an application/json content type. Without it Jersey
// serves a bare boolean as text/plain, which the SPA's fetch client discards as undefined,
// leaving the scheduler status (and its Pause/Resume button) stuck.
@Produces(MediaType.APPLICATION_JSON)
@jakarta.ws.rs.Path("/api")
public class SchedulerResource {

    @GET
    @jakarta.ws.rs.Path("/scheduler/isRunning")
    public boolean isSchedulerRunning() {
        try {
            return ScheduleManager.isRunning();
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to check if scheduler is running: " + e.getMessage(), e);
        }
    }

    @GET
    @jakarta.ws.rs.Path("/scheduler/isPaused")
    public boolean isSchedulerPaused() {
        try {
            return ScheduleManager.isPaused();
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to check if scheduler is paused: " + e.getMessage(), e);
        }
    }

    @POST
    @jakarta.ws.rs.Path("/scheduler/resume")
    public boolean resumeScheduler() {
        try {
            if (!ScheduleManager.isRunning()) {
                ScheduleManager.resume();
            }
            return false;
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to start scheduler: " + e.getMessage(), e);
        }
    }

    @POST
    @jakarta.ws.rs.Path("/scheduler/pause")
    public boolean pauseScheduler() {
        try {
            if (ScheduleManager.isRunning()) {
                ScheduleManager.pause();
            }
            return true;
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to pause scheduler: " + e.getMessage(), e);
        }
    }
}
